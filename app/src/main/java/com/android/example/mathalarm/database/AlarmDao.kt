package com.android.example.mathalarm.database

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface AlarmDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addAlarm(alarm: Alarm?): Long

    @Update
    suspend fun updateAlarm(alarm: Alarm?)

    @Delete
    suspend fun deleteAlarm(alarm: Alarm?)

    @Query("DELETE FROM alarms")
    suspend fun clear()

    @Query("SELECT * FROM alarms WHERE alarmid = :alarmUid LIMIT 1")
    suspend fun getAlarm(alarmUid: Long?): Alarm

    @Query("SELECT * FROM alarms ORDER BY alarmid DESC LIMIT 1")
    suspend fun getLastAlarm(): Alarm?

    @Query("SELECT * FROM alarms ORDER BY alarmid DESC")
    suspend fun getAlarms(): List<Alarm>

    @Query("SELECT * FROM alarms WHERE ison = :state")
    fun getActiveAlarms(state: Boolean = true): List<Alarm>

    @Query("SELECT COUNT(*) FROM alarms")
    suspend fun getSize(): Int
}