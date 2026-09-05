package com.timilehinaregbesola.mathalarm.review

import android.app.AlarmManager
import android.app.Application
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import co.touchlab.kermit.Logger
import com.timilehinaregbesola.mathalarm.domain.model.Alarm
import com.timilehinaregbesola.mathalarm.notification.AlarmNotificationScheduler
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30], application = Application::class)
class AlarmReliabilityReviewTest {
    private val context = ApplicationProvider.getApplicationContext<Application>()
    private val manager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
    private val scheduler = AlarmNotificationScheduler(context, Logger.withTag("Review"))

    @Test fun editingTimeMustRemoveOriginalOccurrence() {
        val original = Alarm(alarmId = 17, hour = 7, minute = 0, isOn = true)
        scheduler.scheduleAlarm(original, System.currentTimeMillis() + 60_000)
        scheduler.cancelAlarm(original.copy(hour = 8))
        assertTrue("Editing must cancel the old 07:00 occurrence", shadowOf(manager).scheduledAlarms.isEmpty())
    }

    @Test fun cancellingAlarmMustRemoveItsSnooze() {
        val original = Alarm(alarmId = 18, hour = 7, minute = 0, isOn = true)
        scheduler.scheduleAlarm(original.copy(minute = 5), System.currentTimeMillis() + 300_000)
        scheduler.cancelAlarm(original)
        assertTrue("Cancellation must remove the 07:05 snooze", shadowOf(manager).scheduledAlarms.isEmpty())
    }

    @Test fun slightlyLateOccurrenceMustNotBeDeferredAWeek() {
        val due = System.currentTimeMillis() - 1_000
        scheduler.scheduleAlarm(Alarm(alarmId = 19, hour = 7, minute = 0, isOn = true), due)
        assertTrue("A one-second-late occurrence must remain due now", shadowOf(manager).scheduledAlarms.single().triggerAtTime <= System.currentTimeMillis())
    }
}
