package com.timilehinaregbesola.mathalarm.interactors

import co.touchlab.kermit.Logger
import com.timilehinaregbesola.mathalarm.domain.model.Alarm
import platform.UserNotifications.UNUserNotificationCenter

/**
 * iOS implementation of NotificationInteractor using UNUserNotificationCenter
 */
class NotificationInteractorImpl(
    private val logger: Logger
) : NotificationInteractor {

    private val notificationCenter = UNUserNotificationCenter.currentNotificationCenter()

    override fun show(alarm: Alarm) {
        logger.d { "NotificationInteractorImpl.show - alarmId = ${alarm.alarmId}" }
        // On iOS, notifications are shown automatically by the system when triggered
        // This would be called when the app is in foreground and alarm fires
        // For now, this is a no-op as iOS handles notification display
    }

    override fun dismiss(notificationId: Long) {
        logger.d { "NotificationInteractorImpl.dismiss - alarmId = $notificationId" }
        // Remove delivered notification
        notificationCenter.removeDeliveredNotificationsWithIdentifiers(
            listOf("alarm_$notificationId")
        )
    }
}
