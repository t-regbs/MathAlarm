package com.timilehinaregbesola.mathalarm.notification

/**
 * Constants for iOS notification configuration.
 * Centralized to avoid magic strings and ensure consistency.
 */
object IosNotificationConstants {
    
    /**
     * Category identifier for alarm notifications.
     * Used to associate actions with the notification.
     */
    const val CATEGORY_IDENTIFIER_ALARM = "ALARM_CATEGORY"
    
    /**
     * Action identifier for snooze button.
     */
    const val ACTION_IDENTIFIER_SNOOZE = "SNOOZE_ACTION"
    
    /**
     * Action identifier for dismiss button.
     */
    const val ACTION_IDENTIFIER_DISMISS = "DISMISS_ACTION"
    
    /**
     * Key for storing alarm ID in notification userInfo.
     */
    const val USER_INFO_ALARM_ID = "alarmId"
    
    /**
     * Key for storing alarm hour in notification userInfo.
     */
    const val USER_INFO_HOUR = "hour"
    
    /**
     * Key for storing alarm minute in notification userInfo.
     */
    const val USER_INFO_MINUTE = "minute"
    
    /**
     * Key for storing repeat flag in notification userInfo.
     */
    const val USER_INFO_REPEAT = "repeat"
    
    /**
     * Key for storing repeat days in notification userInfo.
     */
    const val USER_INFO_REPEAT_DAYS = "repeatDays"
    
    /**
     * Key for storing difficulty in notification userInfo.
     */
    const val USER_INFO_DIFFICULTY = "difficulty"
    
    /**
     * Key for storing snooze duration in notification userInfo.
     */
    const val USER_INFO_SNOOZE = "snooze"
    
    /**
     * Key for storing vibrate flag in notification userInfo.
     */
    const val USER_INFO_VIBRATE = "vibrate"
    
    /**
     * Key for storing alarm tone in notification userInfo.
     */
    const val USER_INFO_ALARM_TONE = "alarmTone"
    
    /**
     * Key for storing alarm title in notification userInfo.
     */
    const val USER_INFO_TITLE = "title"
    
    /**
     * Key for storing isOn flag in notification userInfo.
     */
    const val USER_INFO_IS_ON = "isOn"
}
