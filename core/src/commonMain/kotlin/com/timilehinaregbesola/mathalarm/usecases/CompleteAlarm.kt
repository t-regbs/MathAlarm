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
    private val scheduleNextAlarm: ScheduleNextAlarm,
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
        // An alarm is repeating if:
        // 1. It has the repeat flag set (weekly repeat on same day), OR
        // 2. It has multiple days enabled (more than one 'T')
        val enabledDaysCount = alarm.repeatDays.count { it == 'T' }
        val isRepeating = alarm.repeat || enabledDaysCount > 1
        
        // For repeating alarms, dismiss the notification and schedule the next occurrence
        if (isRepeating) {
            notificationInteractor.dismiss(alarm.alarmId)
            // Schedule the next occurrence AFTER completing the current one
            scheduleNextAlarm(alarm)
        } else {
            // For non-repeating one-time alarms, turn off and cancel
            val updatedAlarm = updateAlarmAsCompleted(alarm)
            alarmRepository.updateAlarm(updatedAlarm)
            alarmInteractor.cancel(alarm)
            notificationInteractor.dismiss(alarm.alarmId)
        }
    }

    private fun updateAlarmAsCompleted(alarm: Alarm) =
        alarm.copy(isOn = false)
}
