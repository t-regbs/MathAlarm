package com.timilehinaregbesola.mathalarm.fake

import com.timilehinaregbesola.mathalarm.data.AlarmDataSource
import com.timilehinaregbesola.mathalarm.domain.model.Alarm
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow

class AlarmRepositoryFake : AlarmDataSource {

    private val alarmMap: MutableMap<Long, Alarm> = mutableMapOf()

    override suspend fun addAlarm(alarm: Alarm): Long {
        val id = if (alarm.alarmId == 0L) {
            (alarmMap.keys.maxOrNull() ?: 0L) + 1
        } else {
            alarm.alarmId
        }

        alarmMap[id] = alarm.copy(alarmId = id)
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

    override fun getAlarms(): Flow<List<Alarm>> = flow {
        emit(alarmMap.values.sortedByDescending { it.alarmId })
    }

    override fun getSavedAlarms(): Flow<List<Alarm>> = flow {
        emit(alarmMap.values.filter { it.isSaved }.sortedByDescending { it.alarmId })
    }

    override suspend fun getLatestAlarmFromDatabase(): Alarm? = alarmMap.values.maxByOrNull { it.alarmId }

    override suspend fun findAlarm(id: Long): Alarm? = alarmMap[id]

    override suspend fun clear() = alarmMap.clear()
}
