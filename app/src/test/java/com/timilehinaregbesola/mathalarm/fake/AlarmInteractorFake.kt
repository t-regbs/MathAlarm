package com.timilehinaregbesola.mathalarm.fake

import com.timilehinaregbesola.mathalarm.domain.model.Alarm
import com.timilehinaregbesola.mathalarm.interactors.AlarmInteractor

class AlarmInteractorFake : AlarmInteractor {
    private val alarmMap: MutableMap<Long, Boolean> = mutableMapOf()
    override fun schedule(alarm: Alarm, reschedule: Boolean): Boolean {
        alarmMap[alarm.alarmId] = reschedule
        return true
    }

    override fun cancel(alarm: Alarm) {
        alarmMap.remove(alarm.alarmId)
    }

    fun isAlarmScheduled(alarm: Alarm): Boolean = alarmMap.contains(alarm.alarmId)

    fun clear() = alarmMap.clear()
}
