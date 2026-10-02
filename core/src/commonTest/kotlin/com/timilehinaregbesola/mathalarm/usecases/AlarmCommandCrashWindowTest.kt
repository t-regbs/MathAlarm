package com.timilehinaregbesola.mathalarm.usecases

import com.timilehinaregbesola.mathalarm.data.AlarmDataSource
import com.timilehinaregbesola.mathalarm.data.AlarmRepository
import com.timilehinaregbesola.mathalarm.domain.model.Alarm
import com.timilehinaregbesola.mathalarm.fake.*
import com.timilehinaregbesola.mathalarm.interactors.AlarmInteractor
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlin.test.*

/** Faults assert surviving persisted/native state, rather than call-order-only expectations. */
class AlarmCommandCrashWindowTest {
    private class FailingWrites(private val source: AlarmDataSource) : AlarmDataSource by source {
        var writes = 0
        var failAt: Int? = null
        override suspend fun updateAlarm(alarm: Alarm) {
            writes++
            if (writes == failAt) error("storage write failed")
            source.updateAlarm(alarm)
        }
    }
    private val source = FailingWrites(AlarmRepositoryFake())
    private val repository = AlarmRepository(source)
    private val native = AlarmInteractorFake()
    private val notifications = NotificationInteractorFake()
    private val clock = DateTimeProviderFake().apply { setFixedDateTime(2030, 1, 7, 7, 0) }
    private val calculator = AlarmTimeCalculatorFake()
    private val active = Alarm(alarmId = 51, isSaved = true, isOn = true, activeAt = 1000,
        snoozeCount = 1, scheduleInitialized = true, repeat = true,
        pendingTimes = listOf(1_893_913_200_000L))
    private var recovery = true
    private val backend = object : AlarmInteractor by native {
        override fun cancelRecovery(alarm: Alarm) { recovery = false }
        override fun cancel(alarm: Alarm) { recovery = false; native.cancel(alarm) }
    }

    @Test fun desiredScheduleWriteFailureDoesNotTouchNativeOrUnresolvedState() = runTest {
        repository.addAlarm(active)
        native.schedule(active, active.pendingTimes.single())
        source.failAt = 1
        assertFailsWith<IllegalStateException> { ScheduleAlarm(repository, backend, calculator)(active, true) }
        assertEquals(active, repository.findAlarm(51))
        assertEquals(active.pendingTimes, native.getScheduledAlarms()[51]!!.map { it.timeInMillis })
        assertTrue(recovery)
    }

    @Test fun interruptedNativeScheduleRetainsDesiredAndUnresolvedState() = runTest {
        repository.addAlarm(active)
        val interrupted = object : AlarmInteractor by backend {
            override suspend fun scheduleRepeating(alarm: Alarm, times: List<Long>) {
                throw CancellationException("process terminated")
            }
        }
        assertFailsWith<CancellationException> { ScheduleAlarm(repository, interrupted, calculator)(active, true) }
        val stored = repository.findAlarm(51)!!
        assertEquals(active.activeAt, stored.activeAt)
        assertEquals(active.snoozeCount, stored.snoozeCount)
        assertEquals(calculator.calculateAlarmTimes(active), stored.pendingTimes)
        assertEquals(Alarm.SCHEDULING_IN_PROGRESS, stored.scheduleError)
        assertTrue(recovery)
    }

    @Test fun nativeScheduleAcceptanceThenFinalWriteFailureKeepsRecoveryAndReconciliationEvidence() = runTest {
        repository.addAlarm(active)
        notifications.show(active)
        source.failAt = 2
        assertFailsWith<IllegalStateException> {
            ScheduleAlarm(repository, backend, calculator, notificationInteractor = notifications)(active, true)
        }
        val stored = repository.findAlarm(51)!!
        assertEquals(active.activeAt, stored.activeAt)
        assertEquals(calculator.calculateAlarmTimes(active), stored.pendingTimes)
        assertNotNull(stored.scheduleError)
        assertTrue(native.isAlarmScheduled(active))
        assertTrue(recovery)
        assertTrue(notifications.isNotificationShown(51))
    }

    @Test fun acceptedScheduleReplacementStopsOnlyItsOwnPlaybackAfterDurableAcceptance() = runTest {
        val other = active.copy(alarmId = 52, activeAt = 2000)
        repository.addAlarm(active)
        repository.addAlarm(other)
        notifications.show(active)
        notifications.show(other)
        native.schedule(other, other.pendingTimes.single())
        ScheduleAlarm(repository, backend, calculator, notificationInteractor = notifications)(active, true)
        val replaced = repository.findAlarm(51)!!
        assertNull(replaced.activeAt)
        assertNull(replaced.scheduleError)
        assertTrue(native.isAlarmScheduled(active))
        assertFalse(notifications.isNotificationShown(51))
        assertTrue(notifications.isNotificationShown(52))
        assertTrue(native.isAlarmScheduled(other))
        assertEquals(other, repository.findAlarm(52))
        assertFalse(recovery)
    }

    @Test fun completionFinalWriteFailureLeavesUnresolvedRecoveryAndPlayback() = runTest {
        val oneTime = active.copy(repeat = false, pendingTimes = emptyList())
        repository.addAlarm(oneTime)
        notifications.show(oneTime)
        source.failAt = 1
        assertFailsWith<IllegalStateException> { CompleteAlarm(repository, backend, notifications, clock)(51, 1000) }
        assertEquals(oneTime, repository.findAlarm(51))
        assertTrue(recovery)
        assertTrue(notifications.isNotificationShown(51))
        assertTrue(CompleteAlarm(repository, backend, notifications, clock)(51, 1000))
        assertNull(repository.findAlarm(51)!!.activeAt)
        assertFalse(recovery)
    }

    @Test fun completionNativeCancellationFailureKeepsUnresolvedOccurrence() = runTest {
        repository.addAlarm(active)
        val rejecting = object : AlarmInteractor by backend {
            override fun cancelSnooze(alarm: Alarm) { error("OS cancellation failed") }
        }
        assertFailsWith<IllegalStateException> { CompleteAlarm(repository, rejecting, notifications, clock)(51, 1000) }
        assertEquals(active, repository.findAlarm(51))
        assertTrue(recovery)
    }

    @Test fun acceptedCompletionCleanupFailureRetainsDebtIncludingDisabledRowsAndRemainsAccepted() = runTest {
        val oneTime = active.copy(repeat = false, pendingTimes = emptyList())
        repository.addAlarm(oneTime)
        notifications.show(oneTime)
        var reported: Long? = null
        val rejecting = object : AlarmInteractor by backend {
            override fun cancelRecovery(alarm: Alarm) { error("recovery cancellation failed") }
        }
        assertTrue(CompleteAlarm(repository, rejecting, notifications, clock,
            onCleanupFailure = { id, _ -> reported = id })(51, 1000))
        val accepted = repository.findAlarm(51)!!
        assertFalse(accepted.isOn)
        assertNull(accepted.activeAt)
        assertTrue(AlarmCommandJournal.needsCleanup(accepted))
        assertEquals(51L, reported)
        assertTrue(recovery)
        RescheduleFutureAlarms(repository, backend, calculator,
            notificationInteractor = notifications).onAppResume()
        assertNull(repository.findAlarm(51)!!.scheduleError)
        assertFalse(recovery)
        assertFalse(notifications.isNotificationShown(51))
        assertFalse(CompleteAlarm(repository, backend, notifications, clock)(51, 1000))
    }

    @Test fun acceptedCleanupStorageFailureSurvivesRestartWithoutRestoringResolvedOccurrence() = runTest {
        repository.addAlarm(active)
        source.failAt = 2
        assertTrue(CompleteAlarm(repository, backend, notifications, clock)(51, 1000))
        val accepted = repository.findAlarm(51)!!
        assertNull(accepted.activeAt)
        assertTrue(AlarmCommandJournal.needsCleanup(accepted))
        assertFalse(recovery)
        RescheduleFutureAlarms(repository, backend, calculator).onAppResume()
        assertNull(repository.findAlarm(51)!!.activeAt)
        assertNull(repository.findAlarm(51)!!.scheduleError)
    }

    @Test fun snoozeIntentWriteFailureDoesNotRegisterOrConsumeAllowance() = runTest {
        repository.addAlarm(active)
        source.failAt = 1
        assertFailsWith<IllegalStateException> { SnoozeAlarm(clock, notifications, backend, repository)(51, expectedActiveAt = 1000) }
        assertEquals(active, repository.findAlarm(51))
        assertFalse(native.isAlarmScheduled(active))
        assertTrue(recovery)
    }

    @Test fun acceptedNativeSnoozeThenStorageFailureRestoresExactTimeWithoutNewAllowance() = runTest {
        repository.addAlarm(active)
        notifications.show(active)
        source.failAt = 2
        assertFailsWith<IllegalStateException> { SnoozeAlarm(clock, notifications, backend, repository)(51, expectedActiveAt = 1000) }
        val unresolved = repository.findAlarm(51)!!
        val nativeTime = native.getScheduledAlarms()[51]!!.single().timeInMillis
        assertEquals(active.activeAt, unresolved.activeAt)
        assertEquals(active.snoozeCount, unresolved.snoozeCount)
        assertEquals(nativeTime, AlarmCommandJournal.pendingSnooze(unresolved))
        assertTrue(recovery)
        assertTrue(notifications.isNotificationShown(51))
        clock.setFixedDateTime(2030, 1, 7, 7, 1)
        RescheduleFutureAlarms(repository, backend, calculator,
            notificationInteractor = notifications).onAppResume()
        val restored = repository.findAlarm(51)!!
        assertEquals(nativeTime, restored.snoozedUntil)
        assertEquals(2, restored.snoozeCount)
        assertNull(restored.activeAt)
        assertNull(restored.scheduleError)
        assertFalse(recovery)
        assertEquals(listOf(nativeTime), native.getScheduledAlarms()[51]!!.map { it.timeInMillis })
    }

    @Test fun snoozeRetryAfterFinalWriteFailureUsesSameNativeIdentity() = runTest {
        repository.addAlarm(active)
        source.failAt = 2
        val snooze = SnoozeAlarm(clock, notifications, backend, repository)
        assertFailsWith<IllegalStateException> { snooze(51, expectedActiveAt = 1000) }
        val desired = native.getAlarmTimeMillis(51)
        clock.setFixedDateTime(2030, 1, 7, 7, 1)
        assertTrue(snooze(51, expectedActiveAt = 1000))
        assertEquals(desired, repository.findAlarm(51)!!.snoozedUntil)
        assertEquals(2, repository.findAlarm(51)!!.snoozeCount)
        assertFalse(snooze(51, expectedActiveAt = 1000))
    }

    @Test fun laterKnownRepeatingDeliveryRemainsPendingUntilCurrentResolvesEvenAfterItsTime() = runTest {
        val later = 2000L
        repository.addAlarm(active.copy(pendingTimes = listOf(later)))
        val show = ShowAlarm(repository, notifications, ScheduleNextAlarm(backend, calculator))
        show(51, later)
        assertEquals(1000L, repository.findAlarm(51)!!.activeAt)
        assertEquals(listOf(later), repository.findAlarm(51)!!.pendingTimes)
        assertTrue(CompleteAlarm(repository, backend, notifications, clock)(51, 1000))
        assertEquals(listOf(later), repository.findAlarm(51)!!.pendingTimes)
        show(51, later)
        assertEquals(later, repository.findAlarm(51)!!.activeAt)
        assertFalse(later in repository.findAlarm(51)!!.pendingTimes)
        assertTrue(notifications.isNotificationShown(51))
    }

    @Test fun restorationNeverCancelsAnUnresolvedRecoveryDuringScheduleRepair() = runTest {
        repository.addAlarm(active.copy(pendingTimes = listOf(2000L), scheduleError = "repair failed"))
        RescheduleFutureAlarms(repository, backend, calculator).onAppResume()
        assertEquals(active.activeAt, repository.findAlarm(51)!!.activeAt)
        assertTrue(2000L in repository.findAlarm(51)!!.pendingTimes)
        assertTrue(recovery)
    }

    @Test fun cleanupCancellationAfterAcceptanceRemainsAcceptedAndRetryable() = runTest {
        repository.addAlarm(active)
        val interrupted = object : AlarmInteractor by backend {
            override fun cancelRecovery(alarm: Alarm) { throw CancellationException("observer cancelled") }
        }
        assertTrue(CompleteAlarm(repository, interrupted, notifications, clock)(51, 1000))
        assertNull(repository.findAlarm(51)!!.activeAt)
        assertTrue(AlarmCommandJournal.needsCleanup(repository.findAlarm(51)!!))
        assertTrue(recovery)
        RescheduleFutureAlarms(repository, backend, calculator).onAppResume()
        assertNull(repository.findAlarm(51)!!.scheduleError)
        assertFalse(recovery)
    }

    @Test fun snoozeCancellationRetainsExactIntentAndUnresolvedRecoveryUntilReplay() = runTest {
        repository.addAlarm(active)
        val interrupted = object : AlarmInteractor by backend {
            override suspend fun scheduleSnooze(alarm: Alarm, timeInMillis: Long) {
                throw CancellationException("process terminated")
            }
        }
        assertFailsWith<CancellationException> {
            SnoozeAlarm(clock, notifications, interrupted, repository)(51, expectedActiveAt = 1000)
        }
        val unresolved = repository.findAlarm(51)!!
        val desiredTime = AlarmCommandJournal.pendingSnooze(unresolved)
        assertNotNull(desiredTime)
        assertEquals(active.activeAt, unresolved.activeAt)
        assertEquals(1, unresolved.snoozeCount)
        assertTrue(recovery)
        RescheduleFutureAlarms(repository, backend, calculator).onAppResume()
        assertEquals(desiredTime, repository.findAlarm(51)!!.snoozedUntil)
        assertNull(repository.findAlarm(51)!!.activeAt)
        assertEquals(2, repository.findAlarm(51)!!.snoozeCount)
        assertFalse(recovery)
    }

    @Test fun readinessStorageFailureNeverStartsPlaybackOrConsumesDelivery() = runTest {
        val waiting = active.copy(activeAt = null, pendingTimes = listOf(1000L))
        repository.addAlarm(waiting)
        source.failAt = 1
        assertFailsWith<IllegalStateException> {
            ShowAlarm(repository, notifications, ScheduleNextAlarm(backend, calculator))(51, 1000)
        }
        assertEquals(waiting, repository.findAlarm(51))
        assertFalse(notifications.isNotificationShown(51))
        ShowAlarm(repository, notifications, ScheduleNextAlarm(backend, calculator))(51, 1000)
        assertEquals(1000L, repository.findAlarm(51)!!.activeAt)
        assertFalse(1000L in repository.findAlarm(51)!!.pendingTimes)
        assertTrue(notifications.isNotificationShown(51))
    }

    @Test fun expiredInterruptedSnoozeKeepsOriginalUnresolvedOccurrenceInsteadOfInventingDelay() = runTest {
        repository.addAlarm(active.copy(scheduleError = AlarmCommandJournal.snooze(2000, "repair failed")))
        native.scheduleSnooze(active, 2000)
        RescheduleFutureAlarms(repository, backend, calculator).onAppResume()
        val unresolved = repository.findAlarm(51)!!
        assertEquals(1000L, unresolved.activeAt)
        assertEquals(1, unresolved.snoozeCount)
        assertNull(unresolved.snoozedUntil)
        assertEquals("repair failed", unresolved.scheduleError)
        assertFalse(native.isAlarmScheduled(active))
        assertTrue(recovery)
    }

    @Test fun failedReplayRetainsExactSnoozeIntentAndReportsServiceFailure() = runTest {
        val desiredTime = calculator.calculateNextAlarmTime(active)!!
        val desired = active.copy(scheduleError = AlarmCommandJournal.snooze(desiredTime, null))
        repository.addAlarm(desired)
        var reported: Long? = null
        val rejecting = object : AlarmInteractor by backend {
            override suspend fun scheduleSnooze(alarm: Alarm, timeInMillis: Long) { error("native replay failed") }
        }
        RescheduleFutureAlarms(repository, rejecting, calculator,
            onCleanupFailure = { id, _ -> reported = id }).onAppResume()
        assertEquals(desired, repository.findAlarm(51))
        assertEquals(51L, reported)
        assertTrue(recovery)
    }

    @Test fun cleanupSuccessCallbackRunsOnlyAfterDebtWriteClears() = runTest {
        repository.addAlarm(active)
        var cleared = 0
        source.failAt = 2
        assertTrue(CompleteAlarm(repository, backend, notifications, clock,
            onCleanupSuccess = { cleared++ })(51, 1000))
        assertEquals(0, cleared)
        assertTrue(AlarmCommandJournal.needsCleanup(repository.findAlarm(51)!!))
        RescheduleFutureAlarms(repository, backend, calculator,
            onCleanupSuccess = { cleared++ }).onAppResume()
        assertEquals(1, cleared)
        assertFalse(AlarmCommandJournal.needsCleanup(repository.findAlarm(51)!!))
    }

    @Test fun cleanupPreservesPriorRecurringScheduleFailure() = runTest {
        repository.addAlarm(active.copy(scheduleError = "next weekday repair failed"))
        assertTrue(CompleteAlarm(repository, backend, notifications, clock)(51, 1000))
        assertEquals("next weekday repair failed", repository.findAlarm(51)!!.scheduleError)
    }
}
