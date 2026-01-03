package com.timilehinaregbesola.mathalarm.usecases

import com.timilehinaregbesola.mathalarm.data.AlarmRepository
import com.timilehinaregbesola.mathalarm.domain.model.Alarm
import com.timilehinaregbesola.mathalarm.interactors.AlarmInteractor
import com.timilehinaregbesola.mathalarm.provider.AlarmTimeCalculator

class ScheduleAlarm(
    private val alarmRepository: AlarmRepository,
    private val alarmInteractor: AlarmInteractor,
    private val alarmTimeCalculator: AlarmTimeCalculator
) {

    /**
     * Schedules a new alarm.
     *
     * @param alarm the alarm
     * @param reschedule whether alarm should reschedule (cancels existing before scheduling)
     */
    suspend operator fun invoke(alarm: Alarm, reschedule: Boolean) {
        val foundAlarm = if (alarm.alarmId == 0L) {
            alarmRepository.getLatestAlarm()
        } else {
            alarmRepository.findAlarm(alarm.alarmId) ?: return
        } ?: return

        // Cancel existing alarm if rescheduling
        if (reschedule) {
            alarmInteractor.cancel(foundAlarm)
        }

        // Calculate all alarm times and schedule each one
        val alarmTimes = alarmTimeCalculator.calculateAlarmTimes(foundAlarm)
        
        if (alarmTimes.isEmpty()) {
            // No valid times to schedule
            val updatedAlarm = foundAlarm.copy(isOn = false)
            alarmRepository.updateAlarm(updatedAlarm)
            return
        }

        // Schedule each alarm time
        alarmTimes.forEach { timeInMillis ->
            alarmInteractor.schedule(foundAlarm, timeInMillis)
        }

        val updatedAlarm = foundAlarm.copy(isOn = true)
        alarmRepository.updateAlarm(updatedAlarm)
        alarmRepository.getAlarms()
    }
}
