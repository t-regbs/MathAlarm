package com.timilehinaregbesola.mathalarm.interactors

import com.timilehinaregbesola.mathalarm.domain.model.Alarm
import com.timilehinaregbesola.mathalarm.notification.IosAlarmScheduler

/** Native alerts are displayed by AlarmKit; dismissal clears challenge recovery. */
class NotificationInteractorImpl(private val scheduler: IosAlarmScheduler) : NotificationInteractor {
    override fun show(alarm: Alarm) = Unit

    override fun dismiss(notificationId: Long) = scheduler.cancelRecovery(notificationId)
}
