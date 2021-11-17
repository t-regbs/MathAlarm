package com.timilehinaregbesola.mathalarm.data

import com.timilehinaregbesola.mathalarm.domain.model.Alarm

class AlarmRepository(private val dataSource: AlarmDataSource) {
    suspend fun addAlarm(alarm: Alarm) = dataSource.addAlarm(alarm)

    suspend fun deleteAlarm(alarm: Alarm) = dataSource.deleteAlarm(alarm)

    suspend fun deleteAlarmWithId(id: Long) = dataSource.deleteAlarmFromId(id)

    suspend fun updateAlarm(alarm: Alarm) = dataSource.updateAlarm(alarm)

    fun getAlarms() = dataSource.getAlarms()

    suspend fun getLatestAlarmFromDatabase() = dataSource.getLatestAlarmFromDatabase()

    suspend fun findAlarm(id: Long) = dataSource.findAlarm(id)

    suspend fun clear() = dataSource.clear()
}
