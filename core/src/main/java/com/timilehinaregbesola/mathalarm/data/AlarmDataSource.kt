package com.timilehinaregbesola.mathalarm.data

import com.timilehinaregbesola.mathalarm.domain.model.Alarm
import kotlinx.coroutines.flow.Flow

interface AlarmDataSource {
    suspend fun addAlarm(alarm: Alarm): Long

    suspend fun deleteAlarm(alarm: Alarm)

    suspend fun deleteAlarmFromId(id: Long)

    suspend fun updateAlarm(alarm: Alarm)

    fun getAlarms(): Flow<List<Alarm>>

    fun getSavedAlarms(): Flow<List<Alarm>>

//    suspend fun getLatestAlarmFromDatabase(): Alarm?

    suspend fun findAlarm(id: Long): Alarm?

    suspend fun clear()
}
