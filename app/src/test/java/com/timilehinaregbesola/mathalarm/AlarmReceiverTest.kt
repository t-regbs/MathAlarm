package com.timilehinaregbesola.mathalarm

import android.app.AlarmManager
import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.R], application = TestApplication::class)
class AlarmReceiverTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext<Application>()
    }

    @Test
    fun `ALARM_ACTION intent should have correct extras`() {
        val alarmId = 123L
        val intent = createAlarmIntent(AlarmReceiver.ALARM_ACTION, alarmId)

        assertEquals("Action should be ALARM_ACTION", AlarmReceiver.ALARM_ACTION, intent.action)
        assertEquals("Should have correct alarm ID", alarmId, intent.getLongExtra(AlarmReceiver.EXTRA_TASK, 0))
    }

    @Test
    fun `COMPLETE_ACTION intent should have correct extras`() {
        val alarmId = 456L
        val intent = createAlarmIntent(AlarmReceiver.COMPLETE_ACTION, alarmId)

        assertEquals("Action should be COMPLETE_ACTION", AlarmReceiver.COMPLETE_ACTION, intent.action)
        assertEquals("Should have correct alarm ID", alarmId, intent.getLongExtra(AlarmReceiver.EXTRA_TASK, 0))
    }

    @Test
    fun `SNOOZE_ACTION intent should have correct extras`() {
        val alarmId = 789L
        val intent = createAlarmIntent(AlarmReceiver.SNOOZE_ACTION, alarmId)

        assertEquals("Action should be SNOOZE_ACTION", AlarmReceiver.SNOOZE_ACTION, intent.action)
        assertEquals("Should have correct alarm ID", alarmId, intent.getLongExtra(AlarmReceiver.EXTRA_TASK, 0))
    }

    @Test
    fun `DISMISS_ACTION intent should have correct extras`() {
        val alarmId = 111L
        val intent = createAlarmIntent(AlarmReceiver.DISMISS_ACTION, alarmId)

        assertEquals("Action should be DISMISS_ACTION", AlarmReceiver.DISMISS_ACTION, intent.action)
        assertEquals("Should have correct alarm ID", alarmId, intent.getLongExtra(AlarmReceiver.EXTRA_TASK, 0))
    }

    // ==================== Boot Event Intent Tests ====================

    @Test
    fun `BOOT_COMPLETED intent should have correct action`() {
        val intent = Intent(Intent.ACTION_BOOT_COMPLETED)

        assertEquals("Action should be BOOT_COMPLETED", Intent.ACTION_BOOT_COMPLETED, intent.action)
    }

    @Test
    fun `QUICKBOOT_POWERON intent should have correct action`() {
        val intent = Intent("android.intent.action.QUICKBOOT_POWERON")

        assertEquals("Action should be QUICKBOOT_POWERON", 
            "android.intent.action.QUICKBOOT_POWERON", 
            intent.action)
    }

    @Test
    fun `MY_PACKAGE_REPLACED intent should have correct action`() {
        val intent = Intent("android.intent.action.MY_PACKAGE_REPLACED")

        assertEquals("Action should be MY_PACKAGE_REPLACED", 
            "android.intent.action.MY_PACKAGE_REPLACED", 
            intent.action)
    }

    @Test
    fun `SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED intent should have correct action`() {
        val intent = Intent(AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED)

        assertEquals("Action should be SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED", 
            AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED, 
            intent.action)
    }

    @Test
    fun `alarm intent with zero ID should work`() {
        val intent = createAlarmIntent(AlarmReceiver.ALARM_ACTION, 0L)

        assertEquals("Should have zero alarm ID", 0L, intent.getLongExtra(AlarmReceiver.EXTRA_TASK, -1))
    }

    @Test
    fun `alarm intent with large ID should work`() {
        val largeId = Long.MAX_VALUE
        val intent = createAlarmIntent(AlarmReceiver.ALARM_ACTION, largeId)

        assertEquals("Should have large alarm ID", largeId, intent.getLongExtra(AlarmReceiver.EXTRA_TASK, 0))
    }

    private fun createAlarmIntent(action: String, alarmId: Long): Intent {
        return Intent(context, AlarmReceiver::class.java).apply {
            this.action = action
            putExtra(AlarmReceiver.EXTRA_TASK, alarmId)
        }
    }
}
