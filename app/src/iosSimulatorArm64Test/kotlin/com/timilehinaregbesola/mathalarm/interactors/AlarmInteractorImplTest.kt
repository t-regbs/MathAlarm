package com.timilehinaregbesola.mathalarm.interactors

import co.touchlab.kermit.Logger
import com.timilehinaregbesola.mathalarm.alarm.AlarmScheduleRequest
import com.timilehinaregbesola.mathalarm.alarm.AlarmSchedulerBridge
import com.timilehinaregbesola.mathalarm.alarm.NativeAlarmScheduler
import com.timilehinaregbesola.mathalarm.domain.model.Alarm
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertTrue

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

        interactor.schedule(alarm, 0L)

        assertTrue(interactor.hasPendingOccurrence(alarm))
    }

    private class NativeAlarmSchedulerFake : NativeAlarmScheduler {
        private val scheduledAlarmIds = mutableSetOf<Long>()

        override fun scheduleAlarm(request: AlarmScheduleRequest): Boolean {
            scheduledAlarmIds.add(request.alarmId)
            return true
        }

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
