package com.timilehinaregbesola.mathalarm

import android.app.AlarmManager
import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import co.touchlab.kermit.Logger
import com.timilehinaregbesola.mathalarm.domain.model.Alarm
import com.timilehinaregbesola.mathalarm.notification.AlarmNotificationScheduler
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [31], application = Application::class)
class AlarmSchedulingPermissionTest {
    @Test fun deniedPermissionIsNotReportedAsPendingOrSuccessful() {
        val context = ApplicationProvider.getApplicationContext<Application>()
        val manager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        org.robolectric.shadows.ShadowAlarmManager.setCanScheduleExactAlarms(false)
        val scheduler = AlarmNotificationScheduler(context, Logger.withTag("Test"))
        val alarm = Alarm(alarmId = 5, hour = 7)
        assertThrows(IllegalStateException::class.java) { scheduler.scheduleAlarm(alarm, System.currentTimeMillis() + 60_000) }
        assertFalse(scheduler.hasPendingOccurrence(alarm))
        assertTrue(shadowOf(manager).scheduledAlarms.isEmpty())
    }
}
