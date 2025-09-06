package com.timilehinaregbesola.mathalarm.fake

import com.timilehinaregbesola.mathalarm.domain.model.Alarm
import com.timilehinaregbesola.mathalarm.interactors.AlarmInteractor
import com.timilehinaregbesola.mathalarm.utils.initLocalDateTimeInSystemZone
import kotlinx.datetime.LocalDateTime

class AlarmInteractorFake : AlarmInteractor {
    private val alarmMap: MutableMap<Long, FakeData> = mutableMapOf()
    override fun schedule(alarm: Alarm, reschedule: Boolean): Boolean {
        alarmMap[alarm.alarmId] = FakeData(reschedule, alarm.initLocalDateTimeInSystemZone())
        return true
    }

    override fun cancel(alarm: Alarm) {
        alarmMap.remove(alarm.alarmId)
    }

    fun isAlarmScheduled(alarm: Alarm): Boolean = alarmMap.contains(alarm.alarmId)

    fun clear() = alarmMap.clear()

    fun getAlarmTime(alarmId: Long): LocalDateTime? =
        alarmMap[alarmId]?.time
}

data class FakeData(val reschedule: Boolean, val time: LocalDateTime)
