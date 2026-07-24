package com.timilehinaregbesola.mathalarm.interactors

import co.touchlab.kermit.Logger
import com.timilehinaregbesola.mathalarm.domain.model.Alarm
import com.timilehinaregbesola.mathalarm.notification.AlarmNotificationScheduler

class AlarmInteractorImpl(
    private val alarmManager: AlarmNotificationScheduler,
    private val logger: Logger
) : AlarmInteractor {

    override fun schedule(alarm: Alarm, timeInMillis: Long) {
        logger.d("AlarmInteractorImpl.schedule: alarmId=${alarm.alarmId}, timeInMillis=$timeInMillis")
        alarmManager.scheduleAlarm(alarm, timeInMillis)
    }

    override fun cancel(alarm: Alarm) {
        logger.d("AlarmInteractorImpl.cancel: alarmId=${alarm.alarmId}")
        alarmManager.cancelAlarm(alarm)
    }

    override fun update(alarm: Alarm) {
        logger.d("AlarmInteractorImpl.update: alarmId=${alarm.alarmId}")
        alarmManager.updateAlarm(alarm)
    }

    override suspend fun hasPendingOccurrence(alarm: Alarm): Boolean {
        logger.d("AlarmInteractorImpl.hasPendingOccurrence: alarmId=${alarm.alarmId}")
        return alarmManager.hasPendingOccurrence(alarm)
    }
}
