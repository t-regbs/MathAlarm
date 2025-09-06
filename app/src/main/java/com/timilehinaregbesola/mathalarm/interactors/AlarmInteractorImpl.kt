package com.timilehinaregbesola.mathalarm.interactors

import com.timilehinaregbesola.mathalarm.domain.model.Alarm
import com.timilehinaregbesola.mathalarm.notification.AlarmNotificationScheduler
import timber.log.Timber

class AlarmInteractorImpl(private val alarmManager: AlarmNotificationScheduler) :
    AlarmInteractor {

    override fun schedule(alarm: Alarm, reschedule: Boolean): Boolean {
        Timber.d("AlarmInteractorImpl.schedule called: alarmId=${alarm.alarmId}, time=${alarm.hour}:${alarm.minute}, repeat=${alarm.repeat}, repeatDays=${alarm.repeatDays}, reschedule=$reschedule")
        val result = alarmManager.scheduleAlarm(alarm, reschedule)
        Timber.d("AlarmInteractorImpl.schedule result for alarmId=${alarm.alarmId}: $result")
        return result
    }

    override fun cancel(alarm: Alarm) {
        Timber.d("AlarmInteractorImpl.cancel called: alarmId=${alarm.alarmId}, time=${alarm.hour}:${alarm.minute}, repeat=${alarm.repeat}, repeatDays=${alarm.repeatDays}")
        alarmManager.cancelAlarm(alarm)
        Timber.d("AlarmInteractorImpl.cancel completed for alarmId=${alarm.alarmId}")
    }
}
