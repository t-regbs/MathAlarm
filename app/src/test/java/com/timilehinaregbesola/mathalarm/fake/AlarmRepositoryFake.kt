package com.timilehinaregbesola.mathalarm.fake

import com.timilehinaregbesola.mathalarm.data.AlarmDataSource
import com.timilehinaregbesola.mathalarm.domain.model.Alarm
import java.util.*

class AlarmRepositoryFake : AlarmDataSource {

    private val alarmMap: TreeMap<Long, Alarm> = TreeMap()

    override suspend fun addAlarm(alarm: Alarm): Long {
        val id = if (alarm.alarmId == 0L) {
            alarmMap.lastKey() + 1
        } else {
            alarm.alarmId
        }

        alarmMap[id] = alarm
        return id
    }

    override suspend fun deleteAlarm(alarm: Alarm) {
        alarmMap.remove(alarm.alarmId)
    }

    override suspend fun deleteAlarmFromId(id: Long) {
        alarmMap.remove(id)
    }

    override suspend fun updateAlarm(alarm: Alarm) {
        alarmMap[alarm.alarmId] = alarm
    }

    override suspend fun getAlarms(): List<Alarm> = alarmMap.values.toList()

    override suspend fun getLatestAlarmFromDatabase(): Alarm? = alarmMap[alarmMap.lastKey()]

    override suspend fun findAlarm(id: Long): Alarm? = alarmMap[id]

    override suspend fun clear() = alarmMap.clear()
}
