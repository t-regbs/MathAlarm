package com.timilehinaregbesola.mathalarm.interactors

import co.touchlab.kermit.Logger
import com.timilehinaregbesola.mathalarm.alarm.AlarmScheduleCompletion
import com.timilehinaregbesola.mathalarm.alarm.AlarmScheduleRequest
import com.timilehinaregbesola.mathalarm.alarm.AlarmSchedulerBridge
import com.timilehinaregbesola.mathalarm.alarm.NativeAlarmScheduler
import com.timilehinaregbesola.mathalarm.domain.model.Alarm
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
        override fun cancelOccurrence(alarmId: Long, occurrenceKey: String) = Unit

        override fun cancelAlarm(alarmId: Long) {
            scheduledAlarmIds.remove(alarmId)
        }

        override fun cancelAllAlarms() {
            scheduledAlarmIds.clear()
        }

        override fun isAlarmKitAvailable(): Boolean = true

        override fun hasPendingOccurrence(alarmId: Long): Boolean = scheduledAlarmIds.contains(alarmId)

        override fun snoozeAlarm(alarmId: Long, minutes: Int) = Unit
    }
}
