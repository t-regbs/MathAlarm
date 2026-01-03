package com.timilehinaregbesola.mathalarm.notification

import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionBadge
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNNotificationAction
import platform.UserNotifications.UNNotificationActionOptionDestructive
import platform.UserNotifications.UNNotificationActionOptionForeground
import platform.UserNotifications.UNNotificationCategory
import platform.UserNotifications.UNNotificationCategoryOptionCustomDismissAction
import platform.UserNotifications.UNUserNotificationCenter

/**
 * Sets up iOS notification categories and actions for alarms
 * Should be called at app startup
 */
object IosNotificationSetup {
    
    private const val ALARM_CATEGORY = "ALARM_CATEGORY"
    private const val ACTION_SNOOZE = "SNOOZE_ACTION"
    private const val ACTION_DISMISS = "DISMISS_ACTION"
    private const val ACTION_SOLVE = "SOLVE_ACTION"
    
    /**
     * Configure notification categories with actions
     */
    fun setupNotificationCategories() {
        val notificationCenter = UNUserNotificationCenter.currentNotificationCenter()
        
        // Create actions
        val snoozeAction = UNNotificationAction.actionWithIdentifier(
            identifier = ACTION_SNOOZE,
            title = "Snooze",
            options = 0UL // No special options
        )
        
        val dismissAction = UNNotificationAction.actionWithIdentifier(
            identifier = ACTION_DISMISS,
            title = "Dismiss",
            options = UNNotificationActionOptionDestructive
        )
        
        val solveAction = UNNotificationAction.actionWithIdentifier(
            identifier = ACTION_SOLVE,
            title = "Solve Math",
            options = UNNotificationActionOptionForeground // Opens app
        )
        
        // Create category with actions
        val alarmCategory = UNNotificationCategory.categoryWithIdentifier(
            identifier = ALARM_CATEGORY,
            actions = listOf(solveAction, snoozeAction, dismissAction),
            intentIdentifiers = emptyList<String>(),
            options = UNNotificationCategoryOptionCustomDismissAction
        )
        
        notificationCenter.setNotificationCategories(setOf(alarmCategory))
    }
    
    /**
     * Request notification permissions
     * Call this when appropriate in the app flow
     * 
     * Note: Critical alerts require special Apple entitlement (not available to most apps).
     * We use standard notification permissions - actual alarm sound is played by
     * AlarmAudioController when the notification arrives.
     */
    fun requestPermissions(onResult: (Boolean) -> Unit) {
        val notificationCenter = UNUserNotificationCenter.currentNotificationCenter()
        
        // Request standard notification authorization
        // Critical alerts require Apple approval - we handle alarm sounds via AlarmAudioController
        notificationCenter.requestAuthorizationWithOptions(
            options = UNAuthorizationOptionAlert or 
                     UNAuthorizationOptionSound or 
                     UNAuthorizationOptionBadge
        ) { granted, error ->
            onResult(granted)
        }
    }
    
    /**
     * Check if notifications are authorized
     */
    fun checkNotificationStatus(callback: (NotificationStatus) -> Unit) {
        val notificationCenter = UNUserNotificationCenter.currentNotificationCenter()
        
        notificationCenter.getNotificationSettingsWithCompletionHandler { settings ->
            val status = when (settings?.authorizationStatus) {
                platform.UserNotifications.UNAuthorizationStatusAuthorized -> NotificationStatus.AUTHORIZED
                platform.UserNotifications.UNAuthorizationStatusDenied -> NotificationStatus.DENIED
                platform.UserNotifications.UNAuthorizationStatusNotDetermined -> NotificationStatus.NOT_DETERMINED
                platform.UserNotifications.UNAuthorizationStatusProvisional -> NotificationStatus.PROVISIONAL
                else -> NotificationStatus.NOT_DETERMINED
            }
            callback(status)
        }
    }
}

enum class NotificationStatus {
    AUTHORIZED,
    DENIED,
    NOT_DETERMINED,
    PROVISIONAL
}
