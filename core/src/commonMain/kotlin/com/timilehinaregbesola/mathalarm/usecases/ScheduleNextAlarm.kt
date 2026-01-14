package com.timilehinaregbesola.mathalarm.usecases

import com.timilehinaregbesola.mathalarm.domain.model.Alarm
import com.timilehinaregbesola.mathalarm.interactors.AlarmInteractor
import com.timilehinaregbesola.mathalarm.provider.AlarmTimeCalculator

/**
 * Schedules the next alarm entry or the missing ones in a repeating alarm.
 */
class ScheduleNextAlarm(
    private val alarmInteractor: AlarmInteractor,
    private val alarmTimeCalculator: AlarmTimeCalculator,
) {

    /**
     * Schedules the next alarm.
     *
     * @param alarm task to be rescheduled
     */
    operator fun invoke(alarm: Alarm) {
        // An alarm is repeating if:
        // - It has the repeat flag set (weekly repeat on same day), OR
        // - It has multiple days enabled (more than one 'T')
        val enabledDaysCount = alarm.repeatDays.count { it == 'T' }
        val isRepeating = alarm.repeat || enabledDaysCount > 1
        
        require(isRepeating) { "Alarm is not repeating (repeat=${{alarm.repeat}}, enabled days=$enabledDaysCount)" }
        require(alarm.isOn) { "Alarm is not on" }

        val alarmTimes = alarmTimeCalculator.calculateAlarmTimes(alarm)
        alarmTimes.forEach { timeInMillis ->
            alarmInteractor.schedule(alarm, timeInMillis)
        }
    }
}
