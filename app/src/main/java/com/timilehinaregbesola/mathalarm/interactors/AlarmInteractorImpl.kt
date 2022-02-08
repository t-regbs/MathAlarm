package com.timilehinaregbesola.mathalarm.interactors

import com.timilehinaregbesola.mathalarm.domain.model.Alarm
import com.timilehinaregbesola.mathalarm.notification.AlarmNotificationScheduler
import timber.log.Timber

class AlarmInteractorImpl(private val alarmManager: AlarmNotificationScheduler) :
    AlarmInteractor {

    override fun schedule(alarm: Alarm, reschedule: Boolean): Boolean {
        Timber.d("schedule - alarmId = ${alarm.alarmId}")
        return alarmManager.scheduleAlarm(alarm, reschedule)
    }

    override fun cancel(alarm: Alarm) {
        Timber.d("cancel - alarmId = ${alarm.alarmId}")
        alarmManager.cancelAlarm(alarm)
    }
}
