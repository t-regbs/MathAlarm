package com.timilehinaregbesola.mathalarm.notification

import com.timilehinaregbesola.mathalarm.domain.model.Alarm

/**
 * Generates unique PendingIntent IDs for alarms.
 */
class PendingIntentIdGenerator {

    /**
     * Generates a unique PendingIntent ID for a specific alarm and day combination.
     * 
     * The ID is generated using a hash of: alarmId, dayIndex, hour, and minute.
     * This ensures each alarm/day combination has a unique ID while avoiding
     * integer overflow issues with large alarm IDs.
     *
     * @param alarm the alarm
     * @param dayIndex the day index (0-6, Sunday to Saturday)
     * @return unique integer ID for the PendingIntent
     */
    fun generateId(alarm: Alarm, dayIndex: Int): Int {
        // Use hashCode to combine the values, which handles large numbers gracefully
        var result = alarm.alarmId.hashCode()
        result = 31 * result + dayIndex
        result = 31 * result + alarm.hour
        result = 31 * result + alarm.minute
        return result
    }

    /**
     * Generates a simple PendingIntent ID using just the alarm ID.
     * Useful for one-time alarms or snooze operations.
     *
     * @param alarmId the alarm ID
     * @return integer ID for the PendingIntent
     */
    fun generateSimpleId(alarmId: Long): Int = alarmId.toInt()
}
