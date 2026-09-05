package com.timilehinaregbesola.mathalarm.usecases

import com.timilehinaregbesola.mathalarm.domain.model.Alarm
import com.timilehinaregbesola.mathalarm.interactors.AlarmInteractor
import com.timilehinaregbesola.mathalarm.provider.AlarmTimeCalculator

class ScheduleNextAlarm(
    private val alarmInteractor: AlarmInteractor,
    private val alarmTimeCalculator: AlarmTimeCalculator,
) {
    suspend operator fun invoke(alarm: Alarm): List<Long> {
        require(alarm.repeat && alarm.isOn)
        val times = alarmTimeCalculator.calculateAlarmTimes(alarm).sorted()
        alarmInteractor.scheduleRepeating(alarm, times)
        return times
    }
}
