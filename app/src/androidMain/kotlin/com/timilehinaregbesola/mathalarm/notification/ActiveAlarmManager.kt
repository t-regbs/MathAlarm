package com.timilehinaregbesola.mathalarm.notification

/**
 * Singleton to track the currently active alarm being displayed on the math screen.
 * Used to re-show the alarm notification if the user force-closes the app without
 * solving the math problem.
 */
object ActiveAlarmManager {
    
    /**
     * The ID of the currently active alarm, or null if no alarm is active.
     */
    @Volatile
    var activeAlarmId: Long? = null
        private set
    
    /**
     * Sets the active alarm when the math screen is displayed.
     */
    fun setActiveAlarm(alarmId: Long) {
        activeAlarmId = alarmId
    }
    
    /**
     * Clears the active alarm when it's properly dismissed (solved or snoozed).
     */
    fun clearActiveAlarm() {
        activeAlarmId = null
    }
    
    /**
     * Checks if there's an active alarm that wasn't properly dismissed.
     */
    fun hasActiveAlarm(): Boolean = activeAlarmId != null
}
