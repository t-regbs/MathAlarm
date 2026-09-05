package com.timilehinaregbesola.mathalarm.notification

import android.app.AlarmManager
import android.app.Application
import android.content.Context
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import co.touchlab.kermit.Logger
import com.timilehinaregbesola.mathalarm.AlarmReceiver
import com.timilehinaregbesola.mathalarm.domain.model.Alarm
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowAlarmManager

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.R], application = com.timilehinaregbesola.mathalarm.TestApplication::class)
class AlarmNotificationSchedulerTest {

    private lateinit var context: Context
    private lateinit var alarmManager: AlarmManager
    private lateinit var shadowAlarmManager: ShadowAlarmManager
    private lateinit var scheduler: AlarmNotificationScheduler
    private lateinit var logger: Logger

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext<Application>()
        alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        shadowAlarmManager = shadowOf(alarmManager)
        logger = Logger.withTag("AlarmNotificationSchedulerTest")
        scheduler = AlarmNotificationScheduler(context, logger)
    }

    @Test
    fun `scheduleAlarm should schedule alarm with correct time`() {
        val alarm = createAlarm(id = 1L)
        val triggerTime = System.currentTimeMillis() + 60_000L // 1 minute from now

        scheduler.scheduleAlarm(alarm, triggerTime)

        val scheduledAlarms = shadowAlarmManager.scheduledAlarms
        assertEquals("Should have one scheduled alarm", 1, scheduledAlarms.size)
        assertEquals("Trigger time should match", triggerTime, scheduledAlarms[0].triggerAtTime)
    }

    @Test
    fun `scheduleAlarm should create correct PendingIntent targeting AlarmReceiver`() {
        val alarmId = 123L
        val alarm = createAlarm(id = alarmId)
        val triggerTime = System.currentTimeMillis() + 60_000L

        scheduler.scheduleAlarm(alarm, triggerTime)

        val scheduledAlarms = shadowAlarmManager.scheduledAlarms
        assertNotNull("Should have scheduled alarm", scheduledAlarms.firstOrNull())
        assertEquals("Should use RTC_WAKEUP type", AlarmManager.RTC_WAKEUP, scheduledAlarms[0].type)
        
        val pendingIntent = scheduledAlarms[0].operation
        val shadowPendingIntent = shadowOf(pendingIntent)
        val savedIntent = shadowPendingIntent.savedIntent
        
        assertEquals("Intent should have alarm ID", alarmId, savedIntent.getLongExtra(AlarmReceiver.EXTRA_TASK, -1))
        assertEquals("Intent action should be ALARM_ACTION", AlarmReceiver.ALARM_ACTION, savedIntent.action)
        assertEquals("Intent should target AlarmReceiver", AlarmReceiver::class.java.name, savedIntent.component?.className)
        assertTrue("Should be a broadcast PendingIntent", shadowPendingIntent.isBroadcastIntent)
    }

    @Test
    fun `scheduleAlarm with different alarms should schedule multiple alarms`() {
        val alarm1 = createAlarm(id = 1L)
        val alarm2 = createAlarm(id = 2L)
        val alarm3 = createAlarm(id = 3L)
        val baseTime = System.currentTimeMillis()

        scheduler.scheduleAlarm(alarm1, baseTime + 60_000L)
        scheduler.scheduleAlarm(alarm2, baseTime + 120_000L)
        scheduler.scheduleAlarm(alarm3, baseTime + 180_000L)

        val scheduledAlarms = shadowAlarmManager.scheduledAlarms
        assertEquals("Should have three scheduled alarms", 3, scheduledAlarms.size)
    }

    @Test
    fun `scheduleAlarm with same alarm ID should replace previous alarm`() {
        val alarm = createAlarm(id = 1L)
        val firstTime = System.currentTimeMillis() + 60_000L
        val secondTime = System.currentTimeMillis() + 120_000L

        scheduler.scheduleAlarm(alarm, firstTime)
        scheduler.scheduleAlarm(alarm, secondTime)

        // Due to FLAG_CANCEL_CURRENT, the second schedule should replace the first
        val scheduledAlarms = shadowAlarmManager.scheduledAlarms
        // Note: Robolectric may keep both or only the latest depending on version
        // The important thing is that when the alarm fires, it uses the latest time
        assertEquals("Exactly one occurrence should remain", 1, scheduledAlarms.size)
    }


    @Test
    fun `cancelAlarm should remove scheduled alarm`() {
        val alarm = createAlarm(id = 1L)
        val triggerTime = System.currentTimeMillis() + 60_000L

        scheduler.scheduleAlarm(alarm, triggerTime)
        assertEquals("Should have one scheduled alarm before cancel", 1, shadowAlarmManager.scheduledAlarms.size)

        scheduler.cancelAlarm(alarm)
        
        val scheduledAlarms = shadowAlarmManager.scheduledAlarms
        assertTrue("Scheduled alarms should be empty after cancel", scheduledAlarms.isEmpty())
    }

    @Test
    fun `cancelAlarm for repeating alarm should cancel all day-specific alarms`() {
        // Repeating alarm on Mon, Wed, Fri (indices 1, 3, 5)
        val alarm = createAlarm(id = 1L, repeatDays = "FTFTFTF")

        // Schedule the base alarm
        scheduler.scheduleAlarm(alarm, System.currentTimeMillis() + 60_000L)

        // Cancel should attempt to cancel the base alarm and all day-specific ones
        scheduler.cancelAlarm(alarm)

        val scheduledAlarms = shadowAlarmManager.scheduledAlarms
        assertTrue("All alarms should be canceled", scheduledAlarms.isEmpty())
    }

    @Test
    fun `cancelAlarm should not affect other alarms`() {
        val alarm1 = createAlarm(id = 1L)
        val alarm2 = createAlarm(id = 2L)
        val triggerTime = System.currentTimeMillis() + 60_000L

        scheduler.scheduleAlarm(alarm1, triggerTime)
        scheduler.scheduleAlarm(alarm2, triggerTime + 60_000L)
        assertEquals("Should have two scheduled alarms", 2, shadowAlarmManager.scheduledAlarms.size)

        scheduler.cancelAlarm(alarm1)

        val remainingAlarms = shadowAlarmManager.scheduledAlarms
        assertEquals("Should have one remaining alarm", 1, remainingAlarms.size)
        
        // Verify the remaining alarm is for alarm2
        val pendingIntent = remainingAlarms[0].operation
        val shadowPendingIntent = shadowOf(pendingIntent)
        val savedIntent = shadowPendingIntent.savedIntent
        assertEquals("Remaining alarm should be alarm2", 2L, savedIntent.getLongExtra(AlarmReceiver.EXTRA_TASK, -1))
    }

    @Test
    fun `different alarms have different PendingIntent identities`() {
        val alarm1 = createAlarm(id = 1L)
        val alarm2 = createAlarm(id = 2L)
        val triggerTime = System.currentTimeMillis() + 60_000L

        scheduler.scheduleAlarm(alarm1, triggerTime)
        scheduler.scheduleAlarm(alarm2, triggerTime)

        val scheduledAlarms = shadowAlarmManager.scheduledAlarms
        assertEquals("Should have two scheduled alarms", 2, scheduledAlarms.size)

        val requestCode1 = shadowOf(scheduledAlarms[0].operation).requestCode
        val requestCode2 = shadowOf(scheduledAlarms[1].operation).requestCode
        
        assertTrue("Alarm identities must differ", scheduledAlarms[0].operation != scheduledAlarms[1].operation)
    }

    // ==================== Edge Case Tests ====================

    @Test
    fun `scheduleAlarm with zero ID should still work`() {
        val alarm = createAlarm(id = 0L)
        val triggerTime = System.currentTimeMillis() + 60_000L

        scheduler.scheduleAlarm(alarm, triggerTime)

        val scheduledAlarms = shadowAlarmManager.scheduledAlarms
        assertEquals("Should have one scheduled alarm", 1, scheduledAlarms.size)
    }

    @Test
    fun `scheduleAlarm with very large ID should work`() {
        val alarm = createAlarm(id = Long.MAX_VALUE)
        val triggerTime = System.currentTimeMillis() + 60_000L

        scheduler.scheduleAlarm(alarm, triggerTime)

        val scheduledAlarms = shadowAlarmManager.scheduledAlarms
        assertEquals("Should have one scheduled alarm", 1, scheduledAlarms.size)
    }

    @Test
    fun `updateAlarm should not throw exception`() {
        val alarm = createAlarm(id = 1L)
        
        // updateAlarm is a no-op on Android but should not throw
        scheduler.updateAlarm(alarm)
        
        // No assertions needed - just verify no exception is thrown
    }

    @Test
    fun `cancelAlarm with all days enabled should attempt to cancel 7 day-specific alarms`() {
        val alarm = createAlarm(id = 1L, repeatDays = "TTTTTTT")
        
        // Schedule the alarm
        scheduler.scheduleAlarm(alarm, System.currentTimeMillis() + 60_000L)
        
        // Cancel should handle all 7 days
        scheduler.cancelAlarm(alarm)
        
        assertTrue("All alarms should be canceled", shadowAlarmManager.scheduledAlarms.isEmpty())
    }

    @Test
    fun `cancelAlarm with no repeat days should only cancel base alarm`() {
        val alarm = createAlarm(id = 1L, repeatDays = "FFFFFFF")
        
        scheduler.scheduleAlarm(alarm, System.currentTimeMillis() + 60_000L)
        scheduler.cancelAlarm(alarm)
        
        assertTrue("Alarm should be canceled", shadowAlarmManager.scheduledAlarms.isEmpty())
    }


    @Test
    fun `scheduleAlarm for same alarm on different days should create separate alarms`() {
        val alarm = createAlarm(id = 1L, hour = 7, minute = 0, repeatDays = "FFTFTFF") // Tuesday and Friday
        val baseTime = System.currentTimeMillis()
        
        // Simulate scheduling for Tuesday (day index 2)
        val tuesdayTime = baseTime + (2 * 24 * 60 * 60 * 1000L) // 2 days from now
        scheduler.scheduleAlarm(alarm, tuesdayTime)
        
        // Simulate scheduling for Friday (day index 5)
        val fridayTime = baseTime + (5 * 24 * 60 * 60 * 1000L) // 5 days from now
        scheduler.scheduleAlarm(alarm, fridayTime)
        
        val scheduledAlarms = shadowAlarmManager.scheduledAlarms
        assertEquals("Should have two scheduled alarms for different days", 2, scheduledAlarms.size)
        
        // Verify both times are present
        val scheduledTimes = scheduledAlarms.map { it.triggerAtTime }.toSet()
        assertTrue("Tuesday time should be scheduled", scheduledTimes.contains(tuesdayTime))
        assertTrue("Friday time should be scheduled", scheduledTimes.contains(fridayTime))
    }

    @Test
    fun `scheduleAlarm for same day twice should replace previous alarm`() {
        val alarm = createAlarm(id = 1L, hour = 7, minute = 0)
        val baseTime = System.currentTimeMillis()
        
        // Schedule Tuesday at 7:00 AM (day index 2)
        val tuesdayTime1 = baseTime + (2 * 24 * 60 * 60 * 1000L)
        scheduler.scheduleAlarm(alarm, tuesdayTime1)
        
        // Reschedule Tuesday at a different time (simulates rescheduling)
        val tuesdayTime2 = baseTime + (9 * 24 * 60 * 60 * 1000L) // Next Tuesday
        scheduler.scheduleAlarm(alarm, tuesdayTime2)
        
        val scheduledAlarms = shadowAlarmManager.scheduledAlarms
        assertEquals("Should have only one alarm (replaced)", 1, scheduledAlarms.size)
        assertEquals("Should have the new time", tuesdayTime2, scheduledAlarms[0].triggerAtTime)
    }

    @Test
    fun `scheduleAlarm for all seven days should create seven separate alarms`() {
        val alarm = createAlarm(id = 1L, hour = 8, minute = 30, repeatDays = "TTTTTTT") // All days
        
        // Create unique times that fall on different days of the week
        // Using Calendar to ensure we get actual different days
        val calendar = java.util.Calendar.getInstance()
        calendar.set(java.util.Calendar.YEAR, 2025)
        calendar.set(java.util.Calendar.MONTH, java.util.Calendar.JANUARY)
        calendar.set(java.util.Calendar.DAY_OF_MONTH, 5) // Jan 5, 2025 is a Sunday
        calendar.set(java.util.Calendar.HOUR_OF_DAY, 8)
        calendar.set(java.util.Calendar.MINUTE, 30)
        calendar.set(java.util.Calendar.SECOND, 0)
        calendar.set(java.util.Calendar.MILLISECOND, 0)
        
        val times = mutableListOf<Long>()
        
        for (dayIndex in 0..6) {
            val timeInMillis = calendar.timeInMillis
            times.add(timeInMillis)
            scheduler.scheduleAlarm(alarm, timeInMillis)
            calendar.add(java.util.Calendar.DAY_OF_MONTH, 1) // Move to next day
        }
        
        val scheduledAlarms = shadowAlarmManager.scheduledAlarms
        
        // We expect 7 alarms, one for each day of the week
        assertEquals("Should have seven scheduled alarms for all days", 7, scheduledAlarms.size)
    }

    @Test
    fun `different alarms on same day should not interfere`() {
        val alarm1 = createAlarm(id = 1L, hour = 7, minute = 0)
        val alarm2 = createAlarm(id = 2L, hour = 8, minute = 30)
        val tuesdayTime = System.currentTimeMillis() + (2 * 24 * 60 * 60 * 1000L)
        
        scheduler.scheduleAlarm(alarm1, tuesdayTime)
        scheduler.scheduleAlarm(alarm2, tuesdayTime)
        
        val scheduledAlarms = shadowAlarmManager.scheduledAlarms
        assertEquals("Should have two alarms for different alarm IDs", 2, scheduledAlarms.size)
    }

    @Test
    fun `cancelAlarm should cancel all day-specific alarms for multi-day alarm`() {
        val alarm = createAlarm(id = 1L, hour = 7, minute = 0, repeatDays = "FFTFTFF") // Tuesday and Friday
        val baseTime = System.currentTimeMillis()
        
        // Schedule for Tuesday and Friday
        val tuesdayTime = baseTime + (2 * 24 * 60 * 60 * 1000L)
        val fridayTime = baseTime + (5 * 24 * 60 * 60 * 1000L)
        scheduler.scheduleAlarm(alarm, tuesdayTime)
        scheduler.scheduleAlarm(alarm, fridayTime)
        
        assertEquals("Should have two scheduled alarms", 2, shadowAlarmManager.scheduledAlarms.size)
        
        // Cancel the alarm
        scheduler.cancelAlarm(alarm)
        
        assertTrue("All alarms should be canceled", shadowAlarmManager.scheduledAlarms.isEmpty())
    }

    @Test
    fun `cancelAlarm for one alarm should not affect other alarms`() {
        val alarm1 = createAlarm(id = 1L, hour = 7, minute = 0, repeatDays = "FFTFTFF") // Tuesday and Friday
        val alarm2 = createAlarm(id = 2L, hour = 8, minute = 0, repeatDays = "FFTFTFF") // Tuesday and Friday
        val baseTime = System.currentTimeMillis()
        
        val tuesdayTime = baseTime + (2 * 24 * 60 * 60 * 1000L)
        val fridayTime = baseTime + (5 * 24 * 60 * 60 * 1000L)
        
        // Schedule both alarms for Tuesday and Friday
        scheduler.scheduleAlarm(alarm1, tuesdayTime)
        scheduler.scheduleAlarm(alarm1, fridayTime)
        scheduler.scheduleAlarm(alarm2, tuesdayTime)
        scheduler.scheduleAlarm(alarm2, fridayTime)
        
        assertEquals("Should have four scheduled alarms", 4, shadowAlarmManager.scheduledAlarms.size)
        
        // Cancel only alarm1
        scheduler.cancelAlarm(alarm1)
        
        val remainingAlarms = shadowAlarmManager.scheduledAlarms
        assertEquals("Should have two remaining alarms for alarm2", 2, remainingAlarms.size)
        
        // Verify remaining alarms are for alarm2
        remainingAlarms.forEach { scheduledAlarm ->
            val pendingIntent = scheduledAlarm.operation
            val shadowPendingIntent = shadowOf(pendingIntent)
            val savedIntent = shadowPendingIntent.savedIntent
            assertEquals("Remaining alarms should be for alarm2", 2L, savedIntent.getLongExtra(AlarmReceiver.EXTRA_TASK, -1))
        }
    }

    @Test
    fun `weekday alarm should create five separate alarms`() {
        val alarm = createAlarm(id = 1L, hour = 6, minute = 30, repeatDays = "FTTTTTF") // Mon-Fri
        val baseTime = System.currentTimeMillis()
        
        // Schedule for weekdays (indices 1-5)
        for (day in 1..5) {
            val dayTime = baseTime + (day * 24 * 60 * 60 * 1000L)
            scheduler.scheduleAlarm(alarm, dayTime)
        }
        
        val scheduledAlarms = shadowAlarmManager.scheduledAlarms
        assertEquals("Should have five scheduled alarms for weekdays", 5, scheduledAlarms.size)
    }

    @Test
    fun `weekend alarm should create two separate alarms`() {
        val alarm = createAlarm(id = 1L, hour = 9, minute = 0, repeatDays = "TFFFFFT") // Sun and Sat
        val baseTime = System.currentTimeMillis()
        
        // Schedule for Sunday (0) and Saturday (6)
        val sundayTime = baseTime
        val saturdayTime = baseTime + (6 * 24 * 60 * 60 * 1000L)
        scheduler.scheduleAlarm(alarm, sundayTime)
        scheduler.scheduleAlarm(alarm, saturdayTime)
        
        val scheduledAlarms = shadowAlarmManager.scheduledAlarms
        assertEquals("Should have two scheduled alarms for weekend", 2, scheduledAlarms.size)
    }


    private fun createAlarm(
        id: Long,
        hour: Int = 7,
        minute: Int = 0,
        repeatDays: String = "FFFFFFF",
        repeat: Boolean = repeatDays.contains('T'),
        title: String = "Test Alarm"
    ): Alarm {
        return Alarm(
            alarmId = id,
            hour = hour,
            minute = minute,
            repeatDays = repeatDays,
            repeat = repeat,
            title = title,
            isOn = true,
            isSaved = true
        )
    }
}
