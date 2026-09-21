package com.timilehinaregbesola.mathalarm.interactors

import co.touchlab.kermit.Logger
import com.timilehinaregbesola.mathalarm.alarm.AlarmScheduleCompletion
import com.timilehinaregbesola.mathalarm.alarm.AlarmScheduleRequest
import com.timilehinaregbesola.mathalarm.alarm.AlarmSchedulerBridge
import com.timilehinaregbesola.mathalarm.alarm.NativeAlarmScheduler
import com.timilehinaregbesola.mathalarm.domain.model.Alarm
import com.timilehinaregbesola.mathalarm.notification.IosAlarmScheduler
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.*
import kotlinx.datetime.*

class AlarmInteractorImplTest {

    @Test
    fun `hasPendingOccurrence returns true when AlarmKit bridge still has scheduled occurrence`() = runTest {
        val nativeScheduler = NativeAlarmSchedulerFake()
        AlarmSchedulerBridge.registerScheduler(nativeScheduler)

        val interactor = AlarmInteractorImpl(Logger.withTag("AlarmInteractorImplTest"))
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

        nativeScheduler.markScheduled(alarm.alarmId)

        assertTrue(interactor.hasPendingOccurrence(alarm))
    }


    @Test fun fixedDateAndSundayConventionReachTheNativeScheduler() = runTest {
        val backend = NativeAlarmSchedulerFake()
        AlarmSchedulerBridge.registerScheduler(backend)
        val interactor = AlarmInteractorImpl(Logger.withTag("Test"))
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
        val interactor = AlarmInteractorImpl(Logger.withTag("Test"))
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
        val interactor = AlarmInteractorImpl(Logger.withTag("Test"))
        assertFailsWith<IllegalStateException> { interactor.schedule(Alarm(alarmId = 9), 2_000_000_000_000) }
    }

    @Test fun cancellingRegularOccurrencesLeavesNativeSnoozeRegistered() = runTest {
        val backend = NativeAlarmSchedulerFake()
        AlarmSchedulerBridge.registerScheduler(backend)
        val removedNotifications = mutableListOf<List<String>>()
        val scheduler = IosAlarmScheduler(Logger.withTag("Test")) { removedNotifications.add(it) }
        val alarm = Alarm(alarmId = 9, hour = 7, repeat = true, repeatDays = "FTFFFFF")
        val time = LocalDateTime(2030, 1, 7, 7, 0).toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds()
        scheduler.scheduleOccurrence(alarm, time, repeating = true)
        scheduler.scheduleOccurrence(alarm, time + 300_000, snooze = true)
        scheduler.cancelRegularOccurrences(alarm)
        assertEquals(listOf("snooze"), backend.requests.map { it.occurrenceKey })
        assertEquals(time + 300_000, backend.requests.single().timeInMillis)
        assertEquals((listOf("alarm_9") + (0..6).map { "alarm_9_day_$it" }).toSet(), removedNotifications.single().toSet())
        scheduler.cancelAlarm(alarm)
        assertTrue(backend.requests.isEmpty())
        assertTrue("alarm_9_snooze" in removedNotifications.last())
    }

    @Test fun controlledSnoozesCannotUseTheNativeCountdown() = runTest {
        val backend = NativeAlarmSchedulerFake()
        AlarmSchedulerBridge.registerScheduler(backend)
        val interactor = AlarmInteractorImpl(Logger.withTag("Test"))
        val alarm = Alarm(alarmId = 11, snooze = 5)
        interactor.schedule(alarm, 2_000_000_000_000)
        interactor.schedule(alarm.copy(maxSnoozes = 0), 2_000_000_000_000)
        assertEquals(listOf(0, 5), backend.requests.map { it.snoozeMinutes })
    }

    private class NativeAlarmSchedulerFake : NativeAlarmScheduler {
        private val scheduledAlarmIds = mutableSetOf<Long>()

        fun markScheduled(alarmId: Long) {
            scheduledAlarmIds.add(alarmId)
        }

        val requests = mutableListOf<AlarmScheduleRequest>()
        var failure: String? = null
        override fun scheduleAlarm(request: AlarmScheduleRequest, completion: AlarmScheduleCompletion) {
            requests.add(request)
            scheduledAlarmIds.add(request.alarmId)
            completion.complete(failure == null, failure)
        }
        override fun cancelOccurrence(alarmId: Long, occurrenceKey: String) {
            requests.removeAll { it.alarmId == alarmId && it.occurrenceKey == occurrenceKey }
        }

        override fun cancelAlarm(alarmId: Long) {
            scheduledAlarmIds.remove(alarmId)
            requests.removeAll { it.alarmId == alarmId }
        }

        override fun cancelAllAlarms() {
            scheduledAlarmIds.clear()
        }

        override fun isAlarmKitAvailable(): Boolean = true

        override fun hasPendingOccurrence(alarmId: Long): Boolean = scheduledAlarmIds.contains(alarmId)

        override fun snoozeAlarm(alarmId: Long, minutes: Int) = Unit
    }
}
