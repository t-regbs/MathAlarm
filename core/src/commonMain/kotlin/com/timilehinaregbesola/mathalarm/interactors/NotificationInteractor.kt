package com.timilehinaregbesola.mathalarm.interactors

import com.timilehinaregbesola.mathalarm.domain.model.Alarm

/**
 * Interface to interact with the notification feature.
 */
interface NotificationInteractor {

    /**
     * Shows a notification.
     *
     * @param alarm alarm entity to be shown as notification
     */
    fun show(alarm: Alarm)

    /**
     * Dismisses the current notification.
     *
     * @param notificationId notification to be dismissed
     */
    fun dismiss(notificationId: Long)
}
