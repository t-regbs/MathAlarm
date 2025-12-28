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

    override fun schedule(alarm: Alarm, reschedule: Boolean): Boolean {
        logger.d { "AlarmInteractorImpl.schedule called: alarmId=${alarm.alarmId}, time=${alarm.hour}:${alarm.minute}, reschedule=$reschedule" }
        return scheduler.scheduleAlarm(alarm, reschedule)
    }

    override fun cancel(alarm: Alarm) {
        logger.d { "AlarmInteractorImpl.cancel called: alarmId=${alarm.alarmId}" }
        scheduler.cancelAlarm(alarm)
    }
}
