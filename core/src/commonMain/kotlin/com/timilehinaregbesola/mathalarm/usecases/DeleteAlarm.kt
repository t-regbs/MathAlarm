package com.timilehinaregbesola.mathalarm.usecases

import com.timilehinaregbesola.mathalarm.data.AlarmRepository
import com.timilehinaregbesola.mathalarm.domain.model.Alarm
import com.timilehinaregbesola.mathalarm.interactors.AlarmInteractor
import com.timilehinaregbesola.mathalarm.interactors.NotificationInteractor

class DeleteAlarm(
    private val alarmRepository: AlarmRepository,
    private val alarmInteractor: AlarmInteractor,
    private val notificationInteractor: NotificationInteractor
) {
    suspend operator fun invoke(alarmId: Long) {
        alarmRepository.findAlarm(alarmId)?.let { alarmInteractor.cancel(it) }
        // Playback can outlive its database row, including while waiting in the service queue.
        notificationInteractor.dismiss(alarmId)
        alarmRepository.deleteAlarmWithId(alarmId)
    }

    suspend operator fun invoke(alarm: Alarm) = invoke(alarm.alarmId)
}
