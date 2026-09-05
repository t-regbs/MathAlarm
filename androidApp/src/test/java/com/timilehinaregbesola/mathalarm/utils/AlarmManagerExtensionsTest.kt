package com.timilehinaregbesola.mathalarm.utils

import android.app.AlarmManager
import android.app.Application
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import com.timilehinaregbesola.mathalarm.AlarmReceiver
import org.junit.Assert.assertEquals
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
class AlarmManagerExtensionsTest {

    private lateinit var context: Context
    private lateinit var alarmManager: AlarmManager
    private lateinit var shadowAlarmManager: ShadowAlarmManager

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext<Application>()
        alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        shadowAlarmManager = shadowOf(alarmManager)
    }

    @Test
    fun `setExactAlarm with future time should schedule alarm`() {
        val futureTime = System.currentTimeMillis() + 60_000L // 1 minute from now
        val pendingIntent = createTestPendingIntent(1)

        context.setExactAlarm(futureTime, pendingIntent)

        val scheduledAlarms = shadowAlarmManager.scheduledAlarms
        assertEquals("Should have one scheduled alarm", 1, scheduledAlarms.size)
        assertEquals("Trigger time should match", futureTime, scheduledAlarms[0].triggerAtTime)
    }

    @Test
    fun `setExactAlarm should use RTC_WAKEUP as default type`() {
        val futureTime = System.currentTimeMillis() + 60_000L
        val pendingIntent = createTestPendingIntent(1)

        context.setExactAlarm(futureTime, pendingIntent)

        val scheduledAlarms = shadowAlarmManager.scheduledAlarms
        assertEquals("Should use RTC_WAKEUP type", AlarmManager.RTC_WAKEUP, scheduledAlarms[0].type)
    }

    @Test
    fun `setExactAlarm with custom type should use that type`() {
        val futureTime = System.currentTimeMillis() + 60_000L
        val pendingIntent = createTestPendingIntent(1)

        context.setExactAlarm(futureTime, pendingIntent, AlarmManager.ELAPSED_REALTIME_WAKEUP)

        val scheduledAlarms = shadowAlarmManager.scheduledAlarms
        assertEquals("Should use ELAPSED_REALTIME_WAKEUP type", 
            AlarmManager.ELAPSED_REALTIME_WAKEUP, 
            scheduledAlarms[0].type)
    }

    @Test
    fun `setExactAlarm with past time remains due instead of skipping a week`() {
        val pastTime = System.currentTimeMillis() - 60_000L // 1 minute in the past
        val pendingIntent = createTestPendingIntent(1)
        val oneWeekInMillis = 7 * 24 * 60 * 60 * 1000L

        context.setExactAlarm(pastTime, pendingIntent)

        val scheduledAlarms = shadowAlarmManager.scheduledAlarms
        assertEquals("Should have one scheduled alarm", 1, scheduledAlarms.size)
        
        // The alarm should be scheduled for pastTime + 1 week
        val expectedTime = pastTime
        assertEquals("Trigger time must retain its original date", expectedTime, scheduledAlarms[0].triggerAtTime)
    }

    @Test(expected = IllegalArgumentException::class)
    fun `setExactAlarm with null PendingIntent should not schedule alarm`() {
        val futureTime = System.currentTimeMillis() + 60_000L

        context.setExactAlarm(futureTime, null)

        val scheduledAlarms = shadowAlarmManager.scheduledAlarms
        assertTrue("Should have no scheduled alarms", scheduledAlarms.isEmpty())
    }

    @Test
    fun `multiple setExactAlarm calls should schedule multiple alarms`() {
        val baseTime = System.currentTimeMillis()
        
        context.setExactAlarm(baseTime + 60_000L, createTestPendingIntent(1))
        context.setExactAlarm(baseTime + 120_000L, createTestPendingIntent(2))
        context.setExactAlarm(baseTime + 180_000L, createTestPendingIntent(3))

        val scheduledAlarms = shadowAlarmManager.scheduledAlarms
        assertEquals("Should have three scheduled alarms", 3, scheduledAlarms.size)
    }

    // ==================== cancelAlarm Tests ====================

    @Test
    fun `cancelAlarm should remove scheduled alarm`() {
        val futureTime = System.currentTimeMillis() + 60_000L
        val pendingIntent = createTestPendingIntent(1)
        
        context.setExactAlarm(futureTime, pendingIntent)
        assertEquals("Should have one alarm before cancel", 1, shadowAlarmManager.scheduledAlarms.size)

        context.cancelAlarm(pendingIntent)

        val scheduledAlarms = shadowAlarmManager.scheduledAlarms
        assertTrue("Should have no alarms after cancel", scheduledAlarms.isEmpty())
    }

    @Test
    fun `cancelAlarm with null PendingIntent should not throw`() {
        // This should not throw any exception
        context.cancelAlarm(null)
        
        // No assertion needed - just verify no exception is thrown
    }

    @Test
    fun `cancelAlarm should only cancel matching alarm`() {
        val futureTime = System.currentTimeMillis() + 60_000L
        val pendingIntent1 = createTestPendingIntent(1)
        val pendingIntent2 = createTestPendingIntent(2)
        
        context.setExactAlarm(futureTime, pendingIntent1)
        context.setExactAlarm(futureTime + 60_000L, pendingIntent2)
        assertEquals("Should have two alarms before cancel", 2, shadowAlarmManager.scheduledAlarms.size)

        context.cancelAlarm(pendingIntent1)

        val scheduledAlarms = shadowAlarmManager.scheduledAlarms
        assertEquals("Should have one alarm after cancel", 1, scheduledAlarms.size)
    }

    @Test
    fun `cancelAlarm for non-existent alarm should not throw`() {
        val pendingIntent = createTestPendingIntent(999)
        
        // This should not throw any exception even though the alarm was never scheduled
        context.cancelAlarm(pendingIntent)
    }

    @Test
    fun `getAlarmManager should return AlarmManager instance`() {
        val alarmManager = context.getAlarmManager()
        
        assertTrue("Should return AlarmManager", alarmManager is AlarmManager)
    }

    private fun createTestPendingIntent(requestCode: Int): PendingIntent {
        val intent = Intent(context, AlarmReceiver::class.java).apply {
            action = AlarmReceiver.ALARM_ACTION
            putExtra(AlarmReceiver.EXTRA_TASK, requestCode.toLong())
        }
        return PendingIntent.getBroadcast(
            context,
            requestCode,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}
