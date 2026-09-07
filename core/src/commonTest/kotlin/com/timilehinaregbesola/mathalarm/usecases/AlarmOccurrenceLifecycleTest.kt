package com.timilehinaregbesola.mathalarm.usecases

import com.timilehinaregbesola.mathalarm.data.AlarmRepository
import com.timilehinaregbesola.mathalarm.domain.model.Alarm
import com.timilehinaregbesola.mathalarm.fake.*
import com.timilehinaregbesola.mathalarm.interactors.AlarmInteractor
import com.timilehinaregbesola.mathalarm.provider.AlarmTimeCalculatorImpl
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlin.test.*

class AlarmOccurrenceLifecycleTest {
    private val repository = AlarmRepository(AlarmRepositoryFake())
    private val backend = AlarmInteractorFake()
    private val notifications = NotificationInteractorFake()
    private val clock = DateTimeProviderFake().apply { setFixedDateTime(2030, 1, 7, 6, 0) }
    private val calculator = AlarmTimeCalculatorImpl(clock)
    private val next = ScheduleNextAlarm(backend, calculator)
    private val alarm = Alarm(alarmId = 12, hour = 7, minute = 0, isOn = true, isSaved = true,
        repeatDays = "FTFFFFF")
    private fun time(day: Int, hour: Int = 7) = LocalDateTime(2030, 1, day, hour, 0)
        .toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds()

    @Test fun deletingRingingAlarmStopsPlayback() = runTest {
        repository.addAlarm(alarm.copy(activeAt = time(7)))
        notifications.show(alarm)
        DeleteAlarm(repository, backend, notifications)(alarm.alarmId)
        assertFalse(notifications.isNotificationShown(alarm.alarmId))
        assertNull(repository.findAlarm(alarm.alarmId))
    }

    @Test fun completionStopsPlaybackWhenTheRowIsMissing() = runTest {
        notifications.show(alarm)
        CompleteAlarm(repository, backend, notifications, clock)(alarm.alarmId)
        assertFalse(notifications.isNotificationShown(alarm.alarmId))
    }

    @Test fun clearingAlarmsStopsEveryPlayback() = runTest {
        val alarms = listOf(alarm, alarm.copy(alarmId = 13))
        for (item in alarms) {
            repository.addAlarm(item)
            notifications.show(item)
        }
        ClearAlarms(repository, DeleteAlarm(repository, backend, notifications))(alarms)
        for (item in alarms) {
            assertFalse(notifications.isNotificationShown(item.alarmId))
            assertNull(repository.findAlarm(item.alarmId))
        }
    }

    @Test fun resumeDoesNotDiscardAnOccurrenceWaitingForDelivery() = runTest {
        val trigger = time(7, 6) - 1_000
        repository.addAlarm(alarm.copy(
            scheduleInitialized = true,
            scheduleTimeZone = TimeZone.currentSystemDefault().id,
            pendingTimes = listOf(trigger)
        ))
        backend.schedule(alarm, trigger)
        RescheduleFutureAlarms(repository, backend, calculator).onAppResume()
        ShowAlarm(repository, notifications, next)(12, trigger)
        assertTrue(notifications.isNotificationShown(12))
    }

    @Test fun resumeExpiresAnOldUndeliveredOneTimeAlarm() = runTest {
        repository.addAlarm(alarm.copy(
            scheduleInitialized = true,
            scheduleTimeZone = TimeZone.currentSystemDefault().id,
            pendingTimes = listOf(time(6))
        ))
        RescheduleFutureAlarms(repository, backend, calculator).onAppResume()
        assertFalse(repository.findAlarm(12)!!.isOn)
        assertTrue(repository.findAlarm(12)!!.pendingTimes.isEmpty())
    }

    @Test fun resumeReregistersFutureSchedulesAfterOsRemoval() = runTest {
        repository.addAlarm(alarm.copy(
            scheduleInitialized = true,
            scheduleTimeZone = TimeZone.currentSystemDefault().id,
            pendingTimes = listOf(time(7))
        ))
        // The persisted plan can survive an OS cancellation, such as a force-stop.
        RescheduleFutureAlarms(repository, backend, calculator).onAppResume()
        assertTrue(backend.isAlarmScheduled(alarm))
        assertEquals(listOf(time(7)), repository.findAlarm(12)!!.pendingTimes)
    }

    @Test fun resumeRetriesAnIncompleteSchedule() = runTest {
        repository.addAlarm(alarm.copy(
            scheduleInitialized = true,
            scheduleTimeZone = TimeZone.currentSystemDefault().id,
            scheduleError = Alarm.SCHEDULING_IN_PROGRESS,
            pendingTimes = listOf(time(7))
        ))
        RescheduleFutureAlarms(repository, backend, calculator).onAppResume()
        assertTrue(backend.isAlarmScheduled(alarm))
        assertNull(repository.findAlarm(12)!!.scheduleError)
    }

    @Test fun schedulingFailurePreservesRecoveryPlanAndError() = runTest {
        val failing = object : AlarmInteractor by backend {
            override suspend fun schedule(alarm: Alarm, timeInMillis: Long) { error("permission denied") }
        }
        repository.addAlarm(alarm)
        assertFailsWith<IllegalStateException> { ScheduleAlarm(repository, failing, calculator)(alarm, true) }
        val stored = repository.findAlarm(12)!!
        assertEquals("permission denied", stored.scheduleError)
        assertTrue(stored.scheduleInitialized)
        assertEquals(listOf(time(7)), stored.pendingTimes)
    }

    @Test fun failedSnoozeKeepsRingingAndDoesNotReplaceRecurrence() = runTest {
        val failing = object : AlarmInteractor by backend {
            override suspend fun scheduleSnooze(alarm: Alarm, timeInMillis: Long) { error("OS rejected snooze") }
        }
        repository.addAlarm(alarm)
        notifications.show(alarm)
        assertFailsWith<IllegalStateException> { SnoozeAlarm(clock, notifications, failing, repository)(12) }
        assertTrue(notifications.isNotificationShown(12))
        assertNull(repository.findAlarm(12)!!.snoozedUntil)
    }

    @Test fun snoozePersistsSeparateTimeWithoutEditingAlarm() = runTest {
        repository.addAlarm(alarm.copy(pendingTimes = listOf(time(14)), scheduleInitialized = true))
        SnoozeAlarm(clock, notifications, backend, repository)(12, 5)
        val stored = repository.findAlarm(12)!!
        assertEquals(7, stored.hour)
        assertEquals(0, stored.minute)
        assertEquals(listOf(time(14)), stored.pendingTimes)
        assertEquals(time(7, 6) + 300_000, stored.snoozedUntil)
    }

    @Test fun recoveryDoesNotResurrectCompletedOneTimeOccurrences() = runTest {
        repository.addAlarm(alarm.copy(scheduleInitialized = true, pendingTimes = listOf(time(6))))
        RescheduleFutureAlarms(repository, backend, calculator)()
        assertFalse(repository.findAlarm(12)!!.isOn)
        assertFalse(backend.isAlarmScheduled(alarm))
    }

    @Test fun recoveryPreservesConcreteOneTimeDateAndSnooze() = runTest {
        val future = time(14)
        val snooze = time(7, 6) + 300_000
        repository.addAlarm(alarm.copy(scheduleInitialized = true, pendingTimes = listOf(future), snoozedUntil = snooze))
        RescheduleFutureAlarms(repository, backend, calculator)()
        val stored = repository.findAlarm(12)!!
        assertEquals(listOf(future), stored.pendingTimes)
        assertEquals(snooze, stored.snoozedUntil)
        assertEquals(setOf(future, snooze), backend.getScheduledAlarms()[12]!!.map { it.timeInMillis }.toSet())
    }

    @Test fun obsoleteBroadcastCannotRingAnEditedAlarm() = runTest {
        repository.addAlarm(alarm.copy(scheduleInitialized = true, pendingTimes = listOf(time(14))))
        ShowAlarm(repository, notifications, next)(12, time(7))
        assertFalse(notifications.isNotificationShown(12))
    }

    @Test fun deliveredOccurrenceIsConsumedAndActiveStatePersists() = runTest {
        repository.addAlarm(alarm.copy(scheduleInitialized = true, pendingTimes = listOf(time(7), time(14))))
        ShowAlarm(repository, notifications, next)(12, time(7))
        assertEquals(listOf(time(14)), repository.findAlarm(12)!!.pendingTimes)
        assertEquals(time(7), repository.findAlarm(12)!!.activeAt)
        assertTrue(notifications.isNotificationShown(12))
    }

    @Test fun rearmingFailureCannotPreventCurrentAlarmFromRinging() = runTest {
        val failing = object : AlarmInteractor by backend {
            override suspend fun scheduleRepeating(alarm: Alarm, times: List<Long>) { error("rearm failed") }
        }
        repository.addAlarm(alarm.copy(repeat = true, scheduleInitialized = true, pendingTimes = listOf(time(7))))
        ShowAlarm(repository, notifications, ScheduleNextAlarm(failing, calculator))(12, time(7))
        assertTrue(notifications.isNotificationShown(12))
        assertEquals("rearm failed", repository.findAlarm(12)!!.scheduleError)
    }

    @Test fun interruptedRecoveryKeepsAnExplicitIncompleteState() = runTest {
        val interrupted = object : AlarmInteractor by backend {
            override suspend fun schedule(alarm: Alarm, timeInMillis: Long) {
                throw kotlinx.coroutines.CancellationException("process interrupted")
            }
        }
        repository.addAlarm(alarm)
        assertFailsWith<kotlinx.coroutines.CancellationException> {
            RescheduleFutureAlarms(repository, interrupted, calculator)()
        }
        val stored = repository.findAlarm(12)!!
        assertEquals(listOf(time(7)), stored.pendingTimes)
        assertEquals("Scheduling has not completed", stored.scheduleError)
    }

    @Test fun rebootClearsExpiredActiveOneTimeAlarm() = runTest {
        repository.addAlarm(alarm.copy(scheduleInitialized = true, activeAt = time(6)))
        RescheduleFutureAlarms(repository, backend, calculator)(clearActive = true)
        val stored = repository.findAlarm(12)!!
        assertFalse(stored.isOn)
        assertNull(stored.activeAt)
        assertNull(stored.scheduleError)
    }

    @Test fun recoveryContinuesAfterOneAlarmFails() = runTest {
        val failing = object : AlarmInteractor by backend {
            override suspend fun schedule(alarm: Alarm, timeInMillis: Long) {
                if (alarm.alarmId == 12L) error("first failed") else backend.schedule(alarm, timeInMillis)
            }
        }
        repository.addAlarm(alarm)
        repository.addAlarm(alarm.copy(alarmId = 13))
        RescheduleFutureAlarms(repository, failing, calculator)()
        assertEquals("first failed", repository.findAlarm(12)!!.scheduleError)
        assertTrue(backend.isAlarmScheduled(alarm.copy(alarmId = 13)))
    }

    @Test fun completionReadsCurrentRowRatherThanNotificationSnapshot() = runTest {
        repository.addAlarm(alarm.copy(repeat = true))
        CompleteAlarm(repository, backend, notifications, clock)(alarm.copy(repeat = false))
        assertTrue(repository.findAlarm(12)!!.isOn)
    }
}
