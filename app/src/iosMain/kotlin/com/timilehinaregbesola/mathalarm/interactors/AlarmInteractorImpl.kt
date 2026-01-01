package com.timilehinaregbesola.mathalarm.interactors

import co.touchlab.kermit.Logger
import com.timilehinaregbesola.mathalarm.domain.model.Alarm
import com.timilehinaregbesola.mathalarm.notification.IosAlarmScheduler

/**
 * iOS implementation of AlarmInteractor using IosAlarmScheduler
 */
class AlarmInteractorImpl(
    private val logger: Logger
) : AlarmInteractor {

    private val scheduler = IosAlarmScheduler(logger)

    override fun schedule(alarm: Alarm, timeInMillis: Long) {
        logger.d { "AlarmInteractorImpl.schedule: alarmId=${alarm.alarmId}, timeInMillis=$timeInMillis" }
        // iOS scheduler handles time internally via UNCalendarNotificationTrigger
        // We pass the alarm and it schedules based on alarm's hour/minute
        scheduler.scheduleAlarm(alarm, reschedule = false)
    }

    override fun cancel(alarm: Alarm) {
        logger.d { "AlarmInteractorImpl.cancel: alarmId=${alarm.alarmId}" }
        scheduler.cancelAlarm(alarm)
    }

    override fun update(alarm: Alarm) {
        logger.d { "AlarmInteractorImpl.update: alarmId=${alarm.alarmId}" }
        // On iOS, we need to cancel and reschedule to update
        scheduler.cancelAlarm(alarm)
        scheduler.scheduleAlarm(alarm, reschedule = true)
    }
}
