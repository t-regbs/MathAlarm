package com.timilehinaregbesola.mathalarm.interactors

import com.timilehinaregbesola.mathalarm.domain.model.Alarm

/**
 * Contract to interact with the Alarm layer.
 */
interface AlarmInteractor {

    /**
     * Schedules a new alarm.
     *
     * @param alarm the alarm
     * @param timeInMillis the time to schedule the alarm in milliseconds
     */
    suspend fun schedule(alarm: Alarm, timeInMillis: Long)

    /** All scheduling methods return only after OS acceptance and throw on failure. */
    suspend fun scheduleRepeating(alarm: Alarm, times: List<Long>) {
        times.forEach { schedule(alarm, it) }
    }

    suspend fun scheduleSnooze(alarm: Alarm, timeInMillis: Long) = schedule(alarm, timeInMillis)

    fun cancelSnooze(alarm: Alarm) = Unit

    /**
     * Cancels an alarm.
     *
     * @param alarm the alarm
     */
    fun cancel(alarm: Alarm)

    /**
     * Updates an existing alarm.
     * On Android, the notification will trigger a BroadcastReceiver which will always get the
     * most recent Alarm data from the database, so this may be a no-op on some platforms.
     *
     * @param alarm the alarm to be updated
     */
    suspend fun update(alarm: Alarm)

    /**
     * Returns true when the platform still has a future occurrence pending for this alarm.
     * Implementations that can't determine this can fall back to false.
     */
    suspend fun hasPendingOccurrence(alarm: Alarm): Boolean = false
}

/** Install the normal occurrences without changing a separate snooze. */
suspend fun AlarmInteractor.scheduleOccurrences(alarm: Alarm, times: List<Long>) {
    if (alarm.repeat) scheduleRepeating(alarm, times)
    else times.forEach { schedule(alarm, it) }
}
