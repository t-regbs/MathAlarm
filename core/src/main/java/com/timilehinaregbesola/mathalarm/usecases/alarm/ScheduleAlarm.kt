package com.timilehinaregbesola.mathalarm.usecases.alarm

import com.timilehinaregbesola.mathalarm.data.AlarmRepository
import com.timilehinaregbesola.mathalarm.domain.model.Alarm
import com.timilehinaregbesola.mathalarm.interactors.AlarmInteractor

class ScheduleAlarm(
    private val alarmRepository: AlarmRepository,
    private val alarmInteractor: AlarmInteractor
) {

    /**
     * Schedules a new alarm.
     *
     * @param alarm the alarm
     * @param reschedule whether alarm should reschedule
     */
    suspend operator fun invoke(alarm: Alarm, reschedule: Boolean) {
        val foundAlarm = alarmRepository.findAlarm(alarm.alarmId) ?: return
        val isOn = alarmInteractor.schedule(alarm, reschedule)
        val updatedAlarm = foundAlarm.copy(isOn = isOn)
        alarmRepository.updateAlarm(updatedAlarm)
        alarmRepository.getAlarms()
    }
}
