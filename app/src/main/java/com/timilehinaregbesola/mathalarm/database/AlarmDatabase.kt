package com.timilehinaregbesola.mathalarm.database

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [Alarm::class], version = 2, exportSchema = false)
abstract class AlarmDatabase : RoomDatabase() {
    abstract val alarmDatabaseDao: AlarmDao
}
