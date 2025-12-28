package com.timilehinaregbesola.mathalarm.usecases

import com.timilehinaregbesola.mathalarm.data.AlarmRepository
import com.timilehinaregbesola.mathalarm.domain.model.Alarm
import com.timilehinaregbesola.mathalarm.interactors.AlarmInteractor
import com.timilehinaregbesola.mathalarm.interactors.NotificationInteractor

/**
 * Use case to set an alarm as completed in the database.
 */
class CompleteAlarm(
    private val alarmRepository: AlarmRepository,
    private val alarmInteractor: AlarmInteractor,
    private val notificationInteractor: NotificationInteractor,
) {

    /**
     * Completes the given alarm.
     *
     * @param alarmId the alarm id
     *
     */
    suspend operator fun invoke(alarmId: Long) {
        val alarm = alarmRepository.findAlarm(alarmId) ?: return
        invoke(alarm)
    }

    /**
     * Completes the given alarm.
     *
     * @param alarm the alarm to be updated
     *
     */
    suspend operator fun invoke(alarm: Alarm) {
        val updatedAlarm = updateAlarmAsCompleted(alarm)
        alarmRepository.updateAlarm(updatedAlarm)
        alarmInteractor.cancel(alarm)
        notificationInteractor.dismiss(alarm.alarmId)
    }

    private fun updateAlarmAsCompleted(alarm: Alarm) =
        alarm.copy(isOn = false)
}
