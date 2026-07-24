package com.timilehinaregbesola.mathalarm.fake

import com.timilehinaregbesola.mathalarm.domain.model.Alarm
import com.timilehinaregbesola.mathalarm.interactors.NotificationInteractor

class NotificationInteractorFake : NotificationInteractor {
    private val notificationMap: MutableMap<Long, Boolean> = mutableMapOf()
    override fun show(alarm: Alarm) {
        notificationMap[alarm.alarmId] = true
    }

    override fun dismiss(notificationId: Long) {
        notificationMap[notificationId] = false
    }

    fun isNotificationShown(notificationId: Long): Boolean =
        notificationMap[notificationId] ?: false

    fun clear() = notificationMap.clear()
}
