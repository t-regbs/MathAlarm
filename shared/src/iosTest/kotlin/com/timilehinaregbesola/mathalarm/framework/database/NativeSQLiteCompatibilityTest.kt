package com.timilehinaregbesola.mathalarm.framework.database

import com.timilehinaregbesola.mathalarm.di.buildIosAlarmDatabase
import kotlinx.cinterop.toKString
import kotlinx.coroutines.test.runTest
import platform.Foundation.NSFileManager
import platform.Foundation.NSTemporaryDirectory
import platform.Foundation.NSUUID
import platform.posix.getenv
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class NativeSQLiteCompatibilityTest {
    @Test
    fun legacySchemaNineRowsAndDurableFieldsSurviveNativeDriverReopen() = runTest {
        val fixture = requireNotNull(getenv("MATHALARM_M8_LEGACY_SQLITE_FIXTURE")?.toKString())
        val path = NSTemporaryDirectory() + "mathalarm-native-" + NSUUID().UUIDString + ".sqlite3"
        val files = NSFileManager.defaultManager
        assertTrue(files.copyItemAtPath(fixture, toPath = path, error = null))
        var database: AlarmDatabase? = null
        try {
            database = buildIosAlarmDatabase(path)
            val original = AlarmEntity(
                alarmId = 1, hour = 15, minute = 38, repeat = false,
                repeatDays = "FFFFFFT", isOn = false, difficulty = 0,
                alarmTone = "alarm_orbit", vibrate = false, snooze = 5,
                maxSnoozes = 3, title = "M8_upgrade_preserved", isSaved = true,
            )
            assertEquals(original, database.alarmDatabaseDao.getAlarm(1))
            assertEquals(1, database.alarmDatabaseDao.getSize())

            val unresolved = original.copy(
                title = "M8 native + maths \"kept\"", isOn = true, repeat = true,
                repeatDays = "TFTFFFF", questionCount = 3, snoozeCount = 1,
                pendingTimes = "1791050822441,1791655622441", scheduleInitialized = true,
                activeAt = 1791050822441, scheduleTimeZone = "Europe/London",
            )
            database.alarmDatabaseDao.updateAlarm(unresolved)
            database.close()
            database = buildIosAlarmDatabase(path)
            assertEquals(unresolved, database.alarmDatabaseDao.getAlarm(1))
            assertEquals(1, database.alarmDatabaseDao.getSize())
        } finally {
            database?.close()
            for (suffix in listOf("", "-wal", "-shm", ".lck")) {
                files.removeItemAtPath(path + suffix, error = null)
            }
        }
    }
}
