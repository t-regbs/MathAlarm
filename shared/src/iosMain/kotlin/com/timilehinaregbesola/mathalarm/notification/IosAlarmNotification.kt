package com.timilehinaregbesola.mathalarm.notification

import co.touchlab.kermit.Logger
import platform.UserNotifications.UNUserNotificationCenter

/**
 * iOS implementation for showing and dismissing alarm notifications.
 * 
 * Following Alkaa's pattern of separating notification display concerns
 * from scheduling concerns for better code organization.
 * 
 * On iOS, the notification scheduler handles showing notifications via triggers,
 * so the show methods are no-ops. This class primarily handles dismissing
 * delivered notifications.
 */
class IosAlarmNotification(
    private val logger: Logger
) {
    private val notificationCenter = UNUserNotificationCenter.currentNotificationCenter()

    /**
     * Show an alarm notification.
     * On iOS, notifications are shown by the system when the trigger fires,
     * so this is a no-op.
     */
    fun show(alarmId: Long) {
        logger.d { "IosAlarmNotification.show - no-op on iOS (handled by trigger)" }
        // On iOS, the notification scheduler is responsible for showing notifications
        // via UNCalendarNotificationTrigger. This method exists for API consistency.
    }

    /**
     * Show a repeating alarm notification.
     * On iOS, repeating notifications are handled by the trigger configuration,
     * so this is a no-op.
     */
    fun showRepeating(alarmId: Long) {
        logger.d { "IosAlarmNotification.showRepeating - no-op on iOS (handled by trigger)" }
        // Repeating notifications are configured in the trigger, not shown manually
    }

    /**
     * Dismiss a delivered notification for the given alarm.
     * 
     * @param alarmId the alarm ID to dismiss notifications for
     */
    fun dismiss(alarmId: Long) {
        logger.d { "IosAlarmNotification.dismiss - alarmId: $alarmId" }
        
        val identifiers = buildNotificationIdentifiers(alarmId)
        notificationCenter.removeDeliveredNotificationsWithIdentifiers(identifiers)
        
        logger.d { "IosAlarmNotification.dismiss - removed ${identifiers.size} delivered notifications" }
    }

    /**
     * Dismiss all delivered notifications.
     */
    fun dismissAll() {
        logger.d { "IosAlarmNotification.dismissAll" }
        notificationCenter.removeAllDeliveredNotifications()
    }

    /**
     * Build list of notification identifiers for an alarm.
     * Includes main alarm identifier and day-specific identifiers for repeating alarms.
     */
    private fun buildNotificationIdentifiers(alarmId: Long): List<String> {
        val identifiers = mutableListOf("alarm_$alarmId", "alarm_${alarmId}_snooze")
        
        // Include day-specific identifiers for repeating alarms
        for (i in 0..6) {
            identifiers.add("alarm_${alarmId}_day_$i")
        }
        
        return identifiers
    }
}
