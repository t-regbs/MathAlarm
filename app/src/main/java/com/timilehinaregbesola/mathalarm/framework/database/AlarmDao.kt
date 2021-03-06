package com.timilehinaregbesola.mathalarm.framework.database

import androidx.room.* // ktlint-disable no-wildcard-imports

@Dao
interface AlarmDao {
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addAlarm(alarm: AlarmEntity?): Long

    @Update
    suspend fun updateAlarm(alarm: AlarmEntity?)

    @Delete
    suspend fun deleteAlarm(alarm: AlarmEntity?)

    @Query("DELETE FROM alarms")
    suspend fun clear()

    @Query("SELECT * FROM alarms WHERE alarmid = :alarmUid LIMIT 1")
    suspend fun getAlarm(alarmUid: Long?): AlarmEntity?

    @Query("SELECT * FROM alarms WHERE alarmid = :alarmUid LIMIT 1")
    fun search(alarmUid: Long?): AlarmEntity?

    @Query("SELECT * FROM alarms ORDER BY alarmid DESC LIMIT 1")
    suspend fun getLastAlarm(): AlarmEntity?

    @Query("SELECT * FROM alarms ORDER BY alarmid DESC")
    suspend fun getAlarms(): List<AlarmEntity>

    @Query("SELECT * FROM alarms WHERE ison = :state")
    fun getActiveAlarms(state: Boolean = true): List<AlarmEntity>

    @Query("SELECT COUNT(*) FROM alarms")
    suspend fun getSize(): Int
}
