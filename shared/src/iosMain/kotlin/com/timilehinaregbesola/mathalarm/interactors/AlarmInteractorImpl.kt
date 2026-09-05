package com.timilehinaregbesola.mathalarm.interactors

import co.touchlab.kermit.Logger
import com.timilehinaregbesola.mathalarm.domain.model.Alarm
import com.timilehinaregbesola.mathalarm.notification.IosAlarmScheduler

class AlarmInteractorImpl(private val logger: Logger) : AlarmInteractor {
    private val scheduler = IosAlarmScheduler(logger)
    override suspend fun schedule(alarm: Alarm, timeInMillis: Long) = scheduler.scheduleOccurrence(alarm, timeInMillis)
    override suspend fun scheduleRepeating(alarm: Alarm, times: List<Long>) {
        times.forEach { scheduler.scheduleOccurrence(alarm, it, repeating = true) }
    }
    override suspend fun scheduleSnooze(alarm: Alarm, timeInMillis: Long) =
        scheduler.scheduleOccurrence(alarm, timeInMillis, snooze = true)
    override fun cancel(alarm: Alarm) = scheduler.cancelAlarm(alarm)
    override fun cancelSnooze(alarm: Alarm) = scheduler.cancelSnooze(alarm)
    override suspend fun update(alarm: Alarm) {
        // Replacing an identifier updates metadata without changing concrete one-time dates.
        if (alarm.repeat) scheduleRepeating(alarm, alarm.pendingTimes)
        else alarm.pendingTimes.filter { it > kotlin.time.Clock.System.now().toEpochMilliseconds() }.forEach { schedule(alarm, it) }
        alarm.snoozedUntil?.takeIf { it > kotlin.time.Clock.System.now().toEpochMilliseconds() }?.let { scheduleSnooze(alarm, it) }
    }
    override suspend fun hasPendingOccurrence(alarm: Alarm): Boolean = scheduler.hasPendingOccurrence(alarm)
}
