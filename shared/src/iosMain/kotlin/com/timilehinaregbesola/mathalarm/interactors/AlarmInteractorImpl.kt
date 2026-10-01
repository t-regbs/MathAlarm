package com.timilehinaregbesola.mathalarm.interactors

import kotlin.time.Clock
import com.timilehinaregbesola.mathalarm.domain.model.Alarm
import com.timilehinaregbesola.mathalarm.notification.IosAlarmScheduler
import com.timilehinaregbesola.mathalarm.alarm.AlarmSchedulerBridge

class AlarmInteractorImpl(
    private val scheduler: IosAlarmScheduler,
) : AlarmInteractor {

    override suspend fun schedule(alarm: Alarm, timeInMillis: Long) =
        scheduler.scheduleOccurrence(alarm, timeInMillis)

    override suspend fun scheduleRepeating(alarm: Alarm, times: List<Long>) {
        times.forEach { scheduler.scheduleOccurrence(alarm, it, repeating = true) }
    }

    override suspend fun scheduleNextRepeating(alarm: Alarm, times: List<Long>) {
        times.forEach { scheduler.ensureRepeatingOccurrence(alarm, it) }
    }

    override suspend fun scheduleSnooze(alarm: Alarm, timeInMillis: Long) {
        scheduler.scheduleOccurrence(alarm, timeInMillis, snooze = true)
        // Only stop recovery after the replacement snooze was accepted.
        AlarmSchedulerBridge.cancelOccurrence(alarm.alarmId, "recovery")
    }

    override fun cancel(alarm: Alarm) = scheduler.cancelAlarm(alarm)

    override fun cancelRegularOccurrences(alarm: Alarm) = scheduler.cancelRegularOccurrences(alarm)

    override fun cancelSnooze(alarm: Alarm) {
        // Completion calls this before clearing its active occurrence in the database.
        AlarmSchedulerBridge.cancelOccurrence(alarm.alarmId, "recovery")
        scheduler.cancelSnooze(alarm)
    }

    override suspend fun update(alarm: Alarm) {
        AlarmSchedulerBridge.cancelOccurrence(alarm.alarmId, "recovery")
        // Replacing an identifier updates metadata without changing concrete one-time dates.
        val now = Clock.System.now().toEpochMilliseconds()
        if (alarm.repeat) {
            scheduleRepeating(alarm, alarm.pendingTimes)
        } else {
            alarm.pendingTimes.filter { it > now }.forEach { schedule(alarm, it) }
        }
        alarm.snoozedUntil?.takeIf { it > now }?.let { scheduleSnooze(alarm, it) }
    }

    override suspend fun hasPendingOccurrence(alarm: Alarm): Boolean = scheduler.hasPendingOccurrence(alarm)
}
