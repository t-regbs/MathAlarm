package com.android.example.mathalarm.database

import androidx.lifecycle.LiveData
import androidx.room.*

@Dao
interface AlarmDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    fun addAlarm(alarm: Alarm?)

    @Update
    fun updateAlarm(alarm: Alarm?)

    @Delete
    fun deleteAlarm(alarm: Alarm?)

    @Query("SELECT * FROM alarms WHERE alarmid = :alarmUid LIMIT 1")
    fun getAlarm(alarmUid: Long?): Alarm?

    @Query("SELECT * FROM alarms ORDER BY alarmid DESC LIMIT 1")
    fun getLastAlarm(): Alarm?

    @Query("SELECT * FROM alarms ORDER BY alarmid DESC")
    fun getAlarms(): LiveData<List<Alarm>>

    @Query("SELECT COUNT(*) FROM alarms")
    fun getSize(): Int
}