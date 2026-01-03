package com.timilehinaregbesola.mathalarm.notification

import co.touchlab.kermit.Logger
import com.timilehinaregbesola.mathalarm.coroutines.AppCoroutineScope
import com.timilehinaregbesola.mathalarm.framework.Usecases
import platform.UserNotifications.UNNotificationResponse

/**
 * Delegate class to handle iOS notification actions.
 * 
 * Following Alkaa's pattern of separating notification action handling
 * from the scheduler for cleaner code organization.
 */
class NotificationActionDelegate(
    private val appCoroutineScope: AppCoroutineScope,
    private val usecases: Usecases,
    private val logger: Logger
) {

    /**
     * Handles the notification response when user interacts with notification actions.
     *
     * @param response the notification response from iOS
     * @param onCompletion callback to execute after handling is complete
     */
    fun handleNotificationResponse(response: UNNotificationResponse, onCompletion: () -> Unit) {
        logger.d { "NotificationActionDelegate - handling response" }
        
        val content = response.notification.request.content
        val userInfo = content.userInfo
        
        val alarmId = (userInfo[IosNotificationConstants.USER_INFO_ALARM_ID] as? Long) ?: run {
            logger.e { "NotificationActionDelegate - alarmId not found in userInfo" }
            onCompletion()
            return
        }
        
        logger.d { "NotificationActionDelegate - alarmId: $alarmId, action: ${response.actionIdentifier}" }

        when (response.actionIdentifier) {
            IosNotificationConstants.ACTION_IDENTIFIER_SNOOZE -> {
                snoozeAlarm(alarmId, onCompletion)
            }
            IosNotificationConstants.ACTION_IDENTIFIER_DISMISS -> {
                dismissAlarm(alarmId, onCompletion)
            }
            // Default action when user taps the notification itself
            "com.apple.UNNotificationDefaultActionIdentifier" -> {
                logger.d { "NotificationActionDelegate - default action (notification tapped)" }
                onCompletion()
            }
            else -> {
                logger.w { "NotificationActionDelegate - Unknown action: ${response.actionIdentifier}" }
                onCompletion()
            }
        }
    }

    private fun snoozeAlarm(alarmId: Long, onCompletion: () -> Unit) {
        logger.d { "NotificationActionDelegate - snoozing alarm $alarmId" }
        appCoroutineScope.launch {
            try {
                usecases.snoozeAlarm(alarmId)
                logger.d { "NotificationActionDelegate - alarm $alarmId snoozed successfully" }
            } catch (e: Exception) {
                logger.e { "NotificationActionDelegate - failed to snooze alarm: ${e.message}" }
            }
            onCompletion()
        }
    }

    private fun dismissAlarm(alarmId: Long, onCompletion: () -> Unit) {
        logger.d { "NotificationActionDelegate - dismissing alarm $alarmId" }
        appCoroutineScope.launch {
            try {
                usecases.completeAlarm(alarmId)
                logger.d { "NotificationActionDelegate - alarm $alarmId dismissed successfully" }
            } catch (e: Exception) {
                logger.e { "NotificationActionDelegate - failed to dismiss alarm: ${e.message}" }
            }
            onCompletion()
        }
    }
}
