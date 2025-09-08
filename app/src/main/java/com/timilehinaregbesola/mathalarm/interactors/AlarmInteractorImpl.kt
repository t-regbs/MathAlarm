package com.timilehinaregbesola.mathalarm.interactors

import co.touchlab.kermit.Logger
import com.timilehinaregbesola.mathalarm.domain.model.Alarm
import com.timilehinaregbesola.mathalarm.notification.AlarmNotificationScheduler
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.parameter.parametersOf

class AlarmInteractorImpl(
    private val alarmManager: AlarmNotificationScheduler,
    private val logger: Logger
) :
    AlarmInteractor, KoinComponent {

    override fun schedule(alarm: Alarm, reschedule: Boolean): Boolean {
        logger.d("AlarmInteractorImpl.schedule called: alarmId=${alarm.alarmId}, time=${alarm.hour}:${alarm.minute}, repeat=${alarm.repeat}, repeatDays=${alarm.repeatDays}, reschedule=$reschedule")
        val result = alarmManager.scheduleAlarm(alarm, reschedule)
        logger.d("AlarmInteractorImpl.schedule result for alarmId=${alarm.alarmId}: $result")
        return result
    }

    override fun cancel(alarm: Alarm) {
        logger.d("AlarmInteractorImpl.cancel called: alarmId=${alarm.alarmId}, time=${alarm.hour}:${alarm.minute}, repeat=${alarm.repeat}, repeatDays=${alarm.repeatDays}")
        alarmManager.cancelAlarm(alarm)
        logger.d("AlarmInteractorImpl.cancel completed for alarmId=${alarm.alarmId}")
    }
}
