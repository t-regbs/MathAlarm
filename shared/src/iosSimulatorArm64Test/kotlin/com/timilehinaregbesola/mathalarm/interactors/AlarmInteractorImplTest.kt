package com.timilehinaregbesola.mathalarm.interactors

import co.touchlab.kermit.Logger
import com.timilehinaregbesola.mathalarm.alarm.AlarmScheduleCompletion
import com.timilehinaregbesola.mathalarm.alarm.AlarmAuthorizationCompletion
import com.timilehinaregbesola.mathalarm.alarm.AlarmScheduleRequest
import com.timilehinaregbesola.mathalarm.alarm.AlarmSchedulerBridge
import com.timilehinaregbesola.mathalarm.alarm.NativeAlarmScheduler
import com.timilehinaregbesola.mathalarm.domain.model.Alarm
import com.timilehinaregbesola.mathalarm.sound.AlarmSoundCatalog
import com.timilehinaregbesola.mathalarm.notification.IosAlarmScheduler
import com.timilehinaregbesola.mathalarm.framework.app.permission.AlarmPermissionImpl
import com.timilehinaregbesola.mathalarm.di.iosModule
import org.koin.dsl.koinApplication
import org.koin.dsl.module
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.*
import kotlinx.datetime.*

class AlarmInteractorImplTest {

    @Test fun bothInteractorsUseTheAlarmKitSchedulerRegisteredInDi() {
        val backend = NativeAlarmSchedulerFake()
        AlarmSchedulerBridge.registerScheduler(backend)
        backend.markScheduled(42, "day_1")
        backend.markScheduled(43, "recovery")
        backend.markScheduled(43, "day_1")
        val scheduler = IosAlarmScheduler(Logger.withTag("InjectedScheduler"))
        val app = koinApplication {
            modules(iosModule, module { single { scheduler } })
        }
        try {
            assertSame(scheduler, app.koin.get<IosAlarmScheduler>())
            app.koin.get<AlarmInteractor>().cancel(Alarm(alarmId = 42))
            app.koin.get<NotificationInteractor>().dismiss(43)
            assertEquals(listOf(42L), backend.cancelledAlarmIds)
            assertEquals(listOf("recovery"), backend.cancelledKeys)
            assertFalse(backend.hasPendingOccurrence(42, "day_1"))
            assertFalse(backend.hasPendingOccurrence(43, "recovery"))
            assertTrue(backend.hasPendingOccurrence(43, "day_1"))
        } finally {
            app.close()
        }
    }

    @Test fun missingAlarmKitBackendRejectsScheduling() = runTest {
        val backend = NativeAlarmSchedulerFake().apply { available = false }
        AlarmSchedulerBridge.registerScheduler(backend)
        val interactor = AlarmInteractorImpl(IosAlarmScheduler(Logger.withTag("Test")))
        assertFailsWith<IllegalStateException> {
            interactor.schedule(Alarm(alarmId = 42), 2_000_000_000_000)
        }
        assertTrue(backend.requests.isEmpty())
    }

    @Test fun libraryToneSurvivesSchedulingEditingAndSnoozing() = runTest {
        val backend = NativeAlarmSchedulerFake()
        AlarmSchedulerBridge.registerScheduler(backend)
        val interactor = AlarmInteractorImpl(IosAlarmScheduler(Logger.withTag("SoundTest")))
        val time = 2_000_000_000_000L
        for ((index, sound) in AlarmSoundCatalog.sounds.withIndex()) {
            val alarm = Alarm(alarmId = 600L + index, alarmTone = sound.id, pendingTimes = listOf(time))
            interactor.schedule(alarm, time)
            assertEquals(sound.id, backend.requests.last().soundName)
            val edited = alarm.copy(alarmTone = "alarm_rally")
            interactor.update(edited)
            assertEquals("alarm_rally", backend.requests.last().soundName)
            assertEquals(time, backend.requests.last().timeInMillis)
            interactor.scheduleSnooze(edited, time + 300_000)
            assertEquals("alarm_rally", backend.requests.last().soundName)
            assertEquals("snooze", backend.requests.last().occurrenceKey)
        }
    }

    @Test
    fun `hasPendingOccurrence returns true when AlarmKit bridge still has scheduled occurrence`() = runTest {
        val nativeScheduler = NativeAlarmSchedulerFake()
        AlarmSchedulerBridge.registerScheduler(nativeScheduler)

        val interactor = AlarmInteractorImpl(IosAlarmScheduler(Logger.withTag("AlarmInteractorImplTest")))
        val alarm = Alarm(
            alarmId = 42,
            hour = 7,
            minute = 0,
            repeat = false,
            repeatDays = "FTFFFFT",
            isOn = true,
            isSaved = true,
            title = "Wrapped one-time alarm"
        )

        val time = LocalDateTime(2030, 1, 7, 7, 0).toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds()
        val scheduledAlarm = alarm.copy(pendingTimes = listOf(time))
        nativeScheduler.markScheduled(alarm.alarmId, "day_1")

        assertTrue(interactor.hasPendingOccurrence(scheduledAlarm))
    }

    @Test fun snoozeCannotHideMissingRegularAlarm() = runTest {
        val backend = NativeAlarmSchedulerFake()
        AlarmSchedulerBridge.registerScheduler(backend)
        val interactor = AlarmInteractorImpl(IosAlarmScheduler(Logger.withTag("Test")))
        val monday = LocalDateTime(2030, 1, 7, 7, 0).toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds()
        val alarm = Alarm(alarmId = 43, repeat = true, repeatDays = "FTFFFFF",
            pendingTimes = listOf(monday), snoozedUntil = monday + 300_000)
        backend.markScheduled(43, "snooze")
        assertFalse(interactor.hasPendingOccurrence(alarm))
        backend.markScheduled(43, "day_1")
        assertTrue(interactor.hasPendingOccurrence(alarm))
    }


    @Test fun fixedDateAndSundayConventionReachTheNativeScheduler() = runTest {
        val backend = NativeAlarmSchedulerFake()
        AlarmSchedulerBridge.registerScheduler(backend)
        val interactor = AlarmInteractorImpl(IosAlarmScheduler(Logger.withTag("Test")))
        val time = LocalDateTime(2030, 1, 6, 7, 0).toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds()
        interactor.schedule(Alarm(alarmId = 9, hour = 7, repeatDays = "TFFFFFF"), time)
        assertEquals(time, backend.requests.single().timeInMillis)
        assertEquals("day_0", backend.requests.single().occurrenceKey)
        assertEquals("TFFFFFF", backend.requests.single().repeatDays)
        assertFalse(backend.requests.single().repeats)
    }
    @Test fun snoozeDoesNotReplaceARecurringSchedule() = runTest {
        val backend = NativeAlarmSchedulerFake()
        AlarmSchedulerBridge.registerScheduler(backend)
        val interactor = AlarmInteractorImpl(IosAlarmScheduler(Logger.withTag("Test")))
        val time = LocalDateTime(2030, 1, 7, 7, 0).toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds()
        val alarm = Alarm(alarmId = 9, hour = 7, repeat = true, repeatDays = "FTFFFFF")
        interactor.scheduleRepeating(alarm, listOf(time))
        interactor.scheduleSnooze(alarm, time + 300_000)
        assertTrue(backend.requests[0].repeats)
        assertEquals("day_1", backend.requests[0].occurrenceKey)
        assertFalse(backend.requests[1].repeats)
        assertEquals("snooze", backend.requests[1].occurrenceKey)
        assertEquals(time + 300_000, backend.requests[1].timeInMillis)
    }
    @Test fun nativeSchedulingFailureReachesTheCaller() = runTest {
        val backend = NativeAlarmSchedulerFake().apply { failure = "Permission denied" }
        AlarmSchedulerBridge.registerScheduler(backend)
        val interactor = AlarmInteractorImpl(IosAlarmScheduler(Logger.withTag("Test")))
        assertFailsWith<IllegalStateException> { interactor.schedule(Alarm(alarmId = 9), 2_000_000_000_000) }
    }

    @Test fun cancellingRegularOccurrencesLeavesNativeSnoozeRegistered() = runTest {
        val backend = NativeAlarmSchedulerFake()
        AlarmSchedulerBridge.registerScheduler(backend)
        val scheduler = IosAlarmScheduler(Logger.withTag("Test"))
        val alarm = Alarm(alarmId = 9, hour = 7, repeat = true, repeatDays = "FTFFFFF")
        val time = LocalDateTime(2030, 1, 7, 7, 0).toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds()
        scheduler.scheduleOccurrence(alarm, time, repeating = true)
        scheduler.scheduleOccurrence(alarm, time + 300_000, snooze = true)
        scheduler.cancelRegularOccurrences(alarm)
        assertEquals(listOf("snooze"), backend.requests.map { it.occurrenceKey })
        assertEquals(time + 300_000, backend.requests.single().timeInMillis)
        scheduler.cancelAlarm(alarm)
        assertTrue(backend.requests.isEmpty())
    }

    @Test fun cancellationFailureIsReportedAndRemainingOccurrencesAreAttempted() = runTest {
        val backend = NativeAlarmSchedulerFake().apply { cancelFailure = "AlarmKit could not cancel" }
        AlarmSchedulerBridge.registerScheduler(backend)
        val scheduler = IosAlarmScheduler(Logger.withTag("Test"))
        val alarm = Alarm(alarmId = 12, repeat = true, repeatDays = "FTFFFFF")

        assertFailsWith<IllegalStateException> { scheduler.cancelRegularOccurrences(alarm) }
        assertEquals((0..6).map { "day_$it" }, backend.cancelledKeys)
        assertFailsWith<IllegalStateException> { scheduler.cancelAlarm(alarm) }
    }

    @Test fun iOSPermissionReflectsAlarmKitAuthorization() {
        val backend = NativeAlarmSchedulerFake()
        AlarmSchedulerBridge.registerScheduler(backend)
        val permission = AlarmPermissionImpl()

        backend.authorization = "denied"
        assertFalse(permission.hasExactAlarmPermission())
        backend.authorization = "authorized"
        assertTrue(permission.hasExactAlarmPermission())
    }

    @Test fun advancingWeeklyAlarmRetainsBothExistingWeekdaysWithoutRescheduling() = runTest {
        val backend = NativeAlarmSchedulerFake().apply { failure = "Duplicate registration rejected" }
        AlarmSchedulerBridge.registerScheduler(backend)
        val interactor = AlarmInteractorImpl(IosAlarmScheduler(Logger.withTag("Test")))
        val monday = LocalDateTime(2030, 1, 7, 7, 0).toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds()
        val saturday = LocalDateTime(2030, 1, 12, 7, 0).toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds()
        val alarm = Alarm(alarmId = 42, hour = 7, repeat = true, repeatDays = "FTFFFFT")
        backend.markScheduled(42, "day_1")
        backend.markScheduled(42, "day_6")

        interactor.scheduleNextRepeating(alarm, listOf(monday, saturday))

        assertTrue(backend.requests.isEmpty())
        assertTrue(backend.hasPendingOccurrence(42, "day_1"))
        assertTrue(backend.hasPendingOccurrence(42, "day_6"))
    }

    @Test fun advancingWeeklyAlarmRepairsOnlyTheMissingWeekday() = runTest {
        val backend = NativeAlarmSchedulerFake()
        AlarmSchedulerBridge.registerScheduler(backend)
        val interactor = AlarmInteractorImpl(IosAlarmScheduler(Logger.withTag("Test")))
        val monday = LocalDateTime(2030, 1, 7, 7, 0).toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds()
        val saturday = LocalDateTime(2030, 1, 12, 7, 0).toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds()
        val alarm = Alarm(alarmId = 42, hour = 7, repeat = true, repeatDays = "FTFFFFT")
        backend.markScheduled(42, "day_6")
        backend.markScheduled(42, "snooze")

        interactor.scheduleNextRepeating(alarm, listOf(monday, saturday))

        assertEquals(listOf("day_1"), backend.requests.map { it.occurrenceKey })
        assertTrue(backend.requests.single().repeats)
        assertTrue(backend.hasPendingOccurrence(42, "snooze"))
    }

    @Test fun repairingMissingWeeklyRegistrationStillReportsSchedulingFailure() = runTest {
        val backend = NativeAlarmSchedulerFake().apply { failure = "Permission denied" }
        AlarmSchedulerBridge.registerScheduler(backend)
        val interactor = AlarmInteractorImpl(IosAlarmScheduler(Logger.withTag("Test")))
        assertFailsWith<IllegalStateException> {
            interactor.scheduleNextRepeating(Alarm(alarmId = 42, repeat = true), listOf(2_000_000_000_000))
        }
    }

    @Test fun explicitWeeklyEditsStillSubmitUpdatedSettings() = runTest {
        val backend = NativeAlarmSchedulerFake()
        AlarmSchedulerBridge.registerScheduler(backend)
        val interactor = AlarmInteractorImpl(IosAlarmScheduler(Logger.withTag("Test")))
        val monday = LocalDateTime(2030, 1, 7, 7, 0).toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds()
        backend.markScheduled(42, "day_1")
        val alarm = Alarm(alarmId = 42, hour = 7, repeat = true, title = "Updated", pendingTimes = listOf(monday))

        interactor.update(alarm)

        assertEquals("Updated", backend.requests.single().title)
        assertTrue(backend.cancelledKeys.isEmpty())
    }

    @Test fun challengeDismissalCancelsRecoveryWithoutRemovingWeeklyOrSnooze() {
        val backend = NativeAlarmSchedulerFake()
        AlarmSchedulerBridge.registerScheduler(backend)
        backend.markScheduled(42, "recovery")
        backend.markScheduled(42, "day_1")
        backend.markScheduled(42, "snooze")
        NotificationInteractorImpl(IosAlarmScheduler(Logger.withTag("Test"))).dismiss(42)
        assertFalse(backend.hasPendingOccurrence(42, "recovery"))
        assertTrue(backend.hasPendingOccurrence(42, "day_1"))
        assertTrue(backend.hasPendingOccurrence(42, "snooze"))
    }

    @Test fun recoveryCancellationFailureReachesChallengeDismissalCaller() {
        val backend = NativeAlarmSchedulerFake().apply { cancelFailure = "Cancellation failed" }
        AlarmSchedulerBridge.registerScheduler(backend)
        assertFailsWith<IllegalStateException> { NotificationInteractorImpl(IosAlarmScheduler(Logger.withTag("Test"))).dismiss(42) }
    }

    @Test fun failedSnoozeDoesNotCancelChallengeRecovery() = runTest {
        val backend = NativeAlarmSchedulerFake().apply { failure = "Schedule rejected" }
        AlarmSchedulerBridge.registerScheduler(backend)
        backend.markScheduled(42, "recovery")
        assertFailsWith<IllegalStateException> {
            AlarmInteractorImpl(IosAlarmScheduler(Logger.withTag("Test"))).scheduleSnooze(Alarm(alarmId = 42), 2_000_000_000_000)
        }
        assertTrue(backend.hasPendingOccurrence(42, "recovery"))
        assertTrue(backend.cancelledKeys.isEmpty())
    }

    @Test fun nativeSnoozeAcceptancePreservesRecoveryUntilApplicationPersistsAcceptance() = runTest {
        val backend = NativeAlarmSchedulerFake()
        AlarmSchedulerBridge.registerScheduler(backend)
        backend.markScheduled(42, "recovery")
        val interactor = AlarmInteractorImpl(IosAlarmScheduler(Logger.withTag("Test")))
        val alarm = Alarm(alarmId = 42)
        interactor.scheduleSnooze(alarm, 2_000_000_000_000)
        assertTrue(backend.hasPendingOccurrence(42, "recovery"))
        assertTrue(backend.hasPendingOccurrence(42, "snooze"))
        assertTrue(backend.cancelledKeys.isEmpty())
        // Core invokes this only after its authoritative accepted-state write.
        interactor.cancelRecovery(alarm)
        assertFalse(backend.hasPendingOccurrence(42, "recovery"))
        assertTrue(backend.hasPendingOccurrence(42, "snooze"))
        backend.markScheduled(42, "recovery")
        interactor.cancelSnooze(alarm)
        assertTrue(backend.hasPendingOccurrence(42, "recovery"))
        interactor.cancelRecovery(alarm)
        assertFalse(backend.hasPendingOccurrence(42, "recovery"))
        assertFalse(backend.hasPendingOccurrence(42, "snooze"))
    }

    @Test fun metadataUpdateAndSnoozeSettingDoNotResolveAnActiveRecovery() = runTest {
        val backend = NativeAlarmSchedulerFake()
        AlarmSchedulerBridge.registerScheduler(backend)
        backend.markScheduled(42, "recovery")
        val interactor = AlarmInteractorImpl(IosAlarmScheduler(Logger.withTag("ActiveEdit")))
        val time = 2_000_000_000_000L
        val alarm = Alarm(alarmId = 42, isOn = true, activeAt = 1000, pendingTimes = listOf(time), snooze = 0)
        interactor.update(alarm)
        interactor.cancelSnooze(alarm)
        assertTrue(backend.hasPendingOccurrence(42, "recovery"))
        assertEquals(time, backend.requests.single().timeInMillis)
        assertEquals(listOf("snooze"), backend.cancelledKeys)
    }

    private class NativeAlarmSchedulerFake : NativeAlarmScheduler {
        private val scheduledKeys = mutableSetOf<Pair<Long, String>>()

        fun markScheduled(alarmId: Long, key: String) {
            scheduledKeys.add(alarmId to key)
        }

        val requests = mutableListOf<AlarmScheduleRequest>()
        var failure: String? = null
        var cancelFailure: String? = null
        var authorization = "authorized"
        var available = true
        val cancelledAlarmIds = mutableListOf<Long>()
        val cancelledKeys = mutableListOf<String>()
        override fun scheduleAlarm(request: AlarmScheduleRequest, completion: AlarmScheduleCompletion) {
            requests.add(request)
            scheduledKeys.add(request.alarmId to request.occurrenceKey)
            completion.complete(failure == null, failure)
        }
        override fun cancelOccurrence(alarmId: Long, occurrenceKey: String): String? {
            cancelledKeys.add(occurrenceKey)
            if (cancelFailure == null) {
                requests.removeAll { it.alarmId == alarmId && it.occurrenceKey == occurrenceKey }
                scheduledKeys.remove(alarmId to occurrenceKey)
            }
            return cancelFailure
        }

        override fun cancelAlarm(alarmId: Long): String? {
            cancelledAlarmIds.add(alarmId)
            if (cancelFailure == null) {
                scheduledKeys.removeAll { it.first == alarmId }
                requests.removeAll { it.alarmId == alarmId }
            }
            return cancelFailure
        }

        override fun isAlarmKitAvailable(): Boolean = available

        override fun hasPendingOccurrence(alarmId: Long, occurrenceKey: String): Boolean =
            scheduledKeys.contains(alarmId to occurrenceKey)

        override fun authorizationStatus(): String = authorization

        override fun requestAuthorization(completion: AlarmAuthorizationCompletion) {
            completion.complete(authorization == "authorized")
        }

        override fun acknowledgePendingHandoff(payload: String) = Unit
        override fun hasPendingHandoff(): Boolean = false
    }
}
