package com.timilehinaregbesola.mathalarm.data

import com.timilehinaregbesola.mathalarm.domain.Alarm

interface AlarmDataSource {
    suspend fun addAlarm(alarm: Alarm): Long

    suspend fun deleteAlarm(alarm: Alarm)

    suspend fun updateAlarm(alarm: Alarm)

    suspend fun getAlarms(): List<Alarm>

    suspend fun getLatestAlarmFromDatabase(): Alarm?

    suspend fun findAlarm(id: Long): Alarm

    suspend fun clear()
}
