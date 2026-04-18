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
    fun schedule(alarm: Alarm, timeInMillis: Long)

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
    fun update(alarm: Alarm)

    /**
     * Returns true when the platform still has a future occurrence pending for this alarm.
     * Implementations that can't determine this can fall back to false.
     */
    suspend fun hasPendingOccurrence(alarm: Alarm): Boolean = false
}
