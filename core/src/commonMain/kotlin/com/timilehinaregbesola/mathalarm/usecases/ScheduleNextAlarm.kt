package com.timilehinaregbesola.mathalarm.usecases

import com.timilehinaregbesola.mathalarm.domain.model.Alarm
import com.timilehinaregbesola.mathalarm.interactors.AlarmInteractor

/**
 * Schedules the next alarm entry or the missing ones in a repeating alarm.
 */
class ScheduleNextAlarm(
    private val alarmInteractor: AlarmInteractor,
) {

    /**
     * Schedules the next alarm.
     *
     * @param alarm task to be rescheduled
     */
    operator fun invoke(alarm: Alarm) {
        require(alarm.repeat) { "Alarm is not repeating" }
        require(alarm.isOn) { "Alarm is not on" }

        alarmInteractor.schedule(alarm, alarm.repeat)
//        logger.debug("ScheduleNextAlarm = Task = '${alarm.title}' at ${alarm.dueDate.time} ")
    }
}
