package com.timilehinaregbesola.mathalarm

import android.app.Application
import android.content.Context
import androidx.room.Room
import androidx.sqlite.driver.AndroidSQLiteDriver
import androidx.test.core.app.ApplicationProvider
import com.timilehinaregbesola.mathalarm.framework.database.AlarmDatabase
import com.timilehinaregbesola.mathalarm.framework.database.MIGRATION_4_5
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30], application = Application::class)
class AlarmDatabaseMigrationTest {
    @Test fun versionFourAlarmSurvivesOccurrenceStateMigration() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val name = "alarm-migration-test.db"
        context.deleteDatabase(name)
        context.openOrCreateDatabase(name, Context.MODE_PRIVATE, null).use { db ->
            db.execSQL("""CREATE TABLE alarms (alarmId INTEGER PRIMARY KEY NOT NULL,
                hour INTEGER NOT NULL, minute INTEGER NOT NULL, repeat INTEGER NOT NULL,
                daysoftheweek TEXT NOT NULL, ison INTEGER NOT NULL, difficulty INTEGER NOT NULL,
                tone TEXT NOT NULL, vibrate INTEGER NOT NULL, snooze INTEGER NOT NULL,
                title TEXT NOT NULL DEFAULT '', isSaved INTEGER NOT NULL DEFAULT 1)""")
            db.execSQL("INSERT INTO alarms VALUES (1, 7, 30, 1, 'FTTTTTF', 1, 2, 'tone', 1, 5, 'Work', 1)")
            db.version = 4
        }
        val database = Room.databaseBuilder<AlarmDatabase>(context, context.getDatabasePath(name).absolutePath)
            .setDriver(AndroidSQLiteDriver()).setQueryCoroutineContext(Dispatchers.IO)
            .addMigrations(MIGRATION_4_5).build()
        try {
            val alarm = database.alarmDatabaseDao.getAlarm(1)!!
            assertEquals("Work", alarm.title)
            assertEquals(7, alarm.hour)
            assertEquals("FTTTTTF", alarm.repeatDays)
            assertTrue(alarm.isOn)
            assertFalse(alarm.scheduleInitialized)
            assertEquals("", alarm.pendingTimes)
            assertNull(alarm.snoozedUntil)
        } finally { database.close(); context.deleteDatabase(name) }
    }
}
