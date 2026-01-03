package com.timilehinaregbesola.mathalarm.provider

import com.timilehinaregbesola.mathalarm.domain.model.Alarm

/**
 * Provides alarm time calculations, respecting Inversion of Control.
 */
interface AlarmTimeCalculator {

    /**
     * Calculates all trigger times for an alarm based on its repeat configuration.
     * 
     * For non-repeating alarms, returns a single time.
     * For repeating alarms, returns times for each enabled day.
     *
     * @param alarm the alarm to calculate times for
     * @return list of times in milliseconds when the alarm should trigger
     */
    fun calculateAlarmTimes(alarm: Alarm): List<Long>

    /**
     * Calculates the next single trigger time for an alarm.
     * Useful for snooze and one-time scheduling.
     *
     * @param alarm the alarm
     * @return the next trigger time in milliseconds, or null if no valid time
     */
    fun calculateNextAlarmTime(alarm: Alarm): Long?

    /**
     * Checks if the given time is in the future.
     *
     * @param timeInMillis the time to check
     * @return true if the time is in the future
     */
    fun isInFuture(timeInMillis: Long): Boolean
}
