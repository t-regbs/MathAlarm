package com.timilehinaregbesola.mathalarm.framework.database

import androidx.room.Database
import androidx.room.RoomDatabase

@Database(entities = [AlarmEntity::class], version = 2, exportSchema = false)
abstract class AlarmDatabase : RoomDatabase() {
    abstract val alarmDatabaseDao: AlarmDao
}
