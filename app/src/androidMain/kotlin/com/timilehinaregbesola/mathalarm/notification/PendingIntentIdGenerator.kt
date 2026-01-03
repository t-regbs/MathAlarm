package com.timilehinaregbesola.mathalarm.notification

import com.timilehinaregbesola.mathalarm.domain.model.Alarm

/**
 * Generates unique PendingIntent IDs for alarms.
 */
class PendingIntentIdGenerator {

    /**
     * Generates a unique PendingIntent ID for a specific alarm and day combination.
     * 
     * The ID is generated from: alarmId + dayIndex + hour + minute
     * This ensures each alarm/day combination has a unique ID.
     *
     * @param alarm the alarm
     * @param dayIndex the day index (0-6, Sunday to Saturday)
     * @return unique integer ID for the PendingIntent
     */
    fun generateId(alarm: Alarm, dayIndex: Int): Int {
        val stringId = StringBuilder()
            .append(alarm.alarmId)
            .append(dayIndex)
            .append(alarm.hour)
            .append(alarm.minute)
        return stringId.toString().replace("-", "").toInt()
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
