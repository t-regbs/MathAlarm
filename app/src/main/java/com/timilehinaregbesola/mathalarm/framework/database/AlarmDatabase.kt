package com.timilehinaregbesola.mathalarm.framework.database

import androidx.room.Database
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase

@Database(entities = [AlarmEntity::class], version = 4, exportSchema = false)
abstract class AlarmDatabase : RoomDatabase() {
    abstract val alarmDatabaseDao: AlarmDao
}

val MIGRATION_2_3 = object : Migration(2, 3) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            """
                CREATE TABLE new_alarms (
                    alarmId INTEGER PRIMARY KEY NOT NULL,
                    hour INTEGER NOT NULL,
                    minute INTEGER NOT NULL,
                    repeat INTEGER NOT NULL,
                    daysoftheweek TEXT NOT NULL,
                    ison INTEGER NOT NULL,
                    difficulty INTEGER NOT NULL,
                    tone TEXT NOT NULL,
                    vibrate INTEGER NOT NULL,
                    snooze INTEGER NOT NULL,
                    title TEXT NOT NULL DEFAULT ''
                )
            """.trimIndent()
        )
        database.execSQL(
            """
                INSERT INTO new_alarms (alarmId, hour, minute, repeat, daysoftheweek, ison, difficulty, tone, vibrate, snooze)
                SELECT alarmId, hour, minute, repeat, daysoftheweek, ison, difficulty, tone, vibrate, snooze FROM alarms
            """.trimIndent()
        )
        database.execSQL("DROP TABLE alarms")
        database.execSQL("ALTER TABLE new_alarms RENAME TO alarms")
    }
}

val MIGRATION_3_4 = object : Migration(3, 4) {
    override fun migrate(database: SupportSQLiteDatabase) {
        database.execSQL(
            """
                CREATE TABLE new_alarms (
                    alarmId INTEGER PRIMARY KEY NOT NULL,
                    hour INTEGER NOT NULL,
                    minute INTEGER NOT NULL,
                    repeat INTEGER NOT NULL,
                    daysoftheweek TEXT NOT NULL,
                    ison INTEGER NOT NULL,
                    difficulty INTEGER NOT NULL,
                    tone TEXT NOT NULL,
                    vibrate INTEGER NOT NULL,
                    snooze INTEGER NOT NULL,
                    title TEXT NOT NULL DEFAULT '',
                    isSaved INTEGER NOT NULL DEFAULT 1
                )
            """.trimIndent()
        )
        database.execSQL(
            """
                INSERT INTO new_alarms (alarmId, hour, minute, repeat, daysoftheweek, ison, difficulty, tone, vibrate, snooze, title)
                SELECT alarmId, hour, minute, repeat, daysoftheweek, ison, difficulty, tone, vibrate, snooze, title FROM alarms
            """.trimIndent()
        )
        database.execSQL("DROP TABLE alarms")
        database.execSQL("ALTER TABLE new_alarms RENAME TO alarms")
    }
}
