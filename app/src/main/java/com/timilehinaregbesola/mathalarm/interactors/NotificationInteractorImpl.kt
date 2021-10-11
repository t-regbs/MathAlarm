package com.timilehinaregbesola.mathalarm.interactors

import com.timilehinaregbesola.mathalarm.domain.model.Alarm
import com.timilehinaregbesola.mathalarm.notification.MathAlarmNotification
import timber.log.Timber

internal class NotificationInteractorImpl(
    private val alarmNotification: MathAlarmNotification,
) : NotificationInteractor {

    override fun show(alarm: Alarm) {
        Timber.d("show - alarmId = ${alarm.alarmId}")
        if (alarm.repeat) {
            alarmNotification.showRepeating(alarm)
        } else {
            alarmNotification.show(alarm)
        }
    }

    override fun dismiss(notificationId: Long) {
        Timber.d("dismiss - alarmId = $notificationId")
        alarmNotification.dismiss(notificationId)
    }
}
