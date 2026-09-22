package com.timilehinaregbesola.mathalarm.notification

import android.content.Intent
import com.timilehinaregbesola.mathalarm.TestApplication
import com.timilehinaregbesola.mathalarm.domain.model.Alarm
import com.timilehinaregbesola.mathalarm.framework.database.AlarmMapper
import kotlinx.serialization.json.Json
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class,
    androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.ui.ExperimentalComposeUiApi::class,
    kotlinx.coroutines.InternalCoroutinesApi::class, androidx.compose.animation.ExperimentalAnimationApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30], application = TestApplication::class)
class AlarmServiceLifecycleTest {
    private val controller = Robolectric.buildService(AlarmService::class.java).create()
    private val service = controller.get()
    private val savedAlarms = mutableMapOf<Long, Alarm>()
    private var lookupGate: kotlinx.coroutines.CompletableDeferred<Unit>? = null

    @org.junit.Before fun setupRepository() {
        val find = io.mockk.mockk<com.timilehinaregbesola.mathalarm.usecases.FindAlarm>()
        io.mockk.coEvery { find.invoke(any()) } coAnswers {
            lookupGate?.await()
            savedAlarms[firstArg()]
        }
        val koin = org.koin.core.context.GlobalContext.get()
        val original = koin.get<com.timilehinaregbesola.mathalarm.framework.Usecases>()
        koin.loadModules(listOf(org.koin.dsl.module {
            single { original.copy(findAlarm = find) }
        }))
    }

    private fun start(id: Long, at: Long = 123) {
        val alarm = Alarm(alarmId = id, isOn = true, activeAt = at)
        savedAlarms[id] = alarm
        deliverStart(alarm)
    }
    private fun deliverStart(alarm: Alarm) {
        service.onStartCommand(Intent(service, AlarmService::class.java).apply {
            action = AlarmService.ACTION_START_ALARM
            putExtra(AlarmService.EXTRA_ALARM_JSON, Json.encodeToString(AlarmMapper().mapFromDomainModel(alarm)))
        }, 0, alarm.alarmId.toInt())
        org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
    }
    private fun dismiss(id: Long) {
        savedAlarms.remove(id)
        service.onStartCommand(Intent(service, AlarmService::class.java).apply {
            action = AlarmService.ACTION_STOP_ALARM
            putExtra(AlarmService.EXTRA_ALARM_ID, id)
        }, 0, 100)
        org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
    }
    @After fun cleanup() { controller.destroy() }

    @Test fun delayedStartCannotRingDeletedDisabledCompletedOrReplacedOccurrence() {
        val snapshot = Alarm(alarmId = 9, isOn = true, activeAt = 123)
        for (saved in listOf(null, snapshot.copy(isOn = false),
            snapshot.copy(activeAt = null), snapshot.copy(activeAt = 456))) {
            savedAlarms.clear()
            if (saved != null) savedAlarms[9] = saved
            deliverStart(snapshot)
            assertFalse(ActiveAlarmManager.hasActiveAlarm())
            val player = AlarmService::class.java.getDeclaredField("mediaPlayer").apply { isAccessible = true }
            assertNull(player.get(service))
        }
    }

    @Test fun deletionWhileValidationWaitsCannotRingOrDiscardAnotherPendingStart() {
        val gate = kotlinx.coroutines.CompletableDeferred<Unit>()
        lookupGate = gate
        start(1)
        start(2)
        assertFalse(ActiveAlarmManager.hasActiveAlarm())
        savedAlarms.remove(1)
        gate.complete(Unit)
        org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
        assertEquals(2L, ActiveAlarmManager.activeAlarmId)
        assertFalse(org.robolectric.Shadows.shadowOf(service).isStoppedBySelf)
    }

    @Test fun serviceRestartDoesNotRestoreDeletedAlarmSnapshot() {
        val snapshot = Alarm(alarmId = 9, isOn = true, activeAt = 123)
        service.getSharedPreferences("active_alarm_playback", android.content.Context.MODE_PRIVATE)
            .edit().putString("alarms", Json.encodeToString(listOf(AlarmMapper().mapFromDomainModel(snapshot)))).commit()
        service.onStartCommand(null, 0, 1)
        org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
        assertFalse(ActiveAlarmManager.hasActiveAlarm())
        assertEquals("[]", service.getSharedPreferences("active_alarm_playback", android.content.Context.MODE_PRIVATE)
            .getString("alarms", null))
    }

    @Test fun deletedQueuedAlarmIsNotPromotedAndNextValidAlarmStillRings() {
        start(1)
        start(2)
        start(3)
        savedAlarms.remove(2)
        dismiss(1)
        assertEquals(3L, ActiveAlarmManager.activeAlarmId)
    }

    @Test fun overlappingAlarmWaitsUntilCurrentAlarmIsDismissed() {
        start(1)
        start(2)
        assertEquals(1L, ActiveAlarmManager.activeAlarmId)
        dismiss(1)
        assertEquals(2L, ActiveAlarmManager.activeAlarmId)
        dismiss(1)
        assertEquals(2L, ActiveAlarmManager.activeAlarmId)
    }

    @Test fun metadataUpdateRemovesSnoozeActionWithoutStartingAnotherAlarm() {
        start(1)
        val updated = Alarm(alarmId = 1, isOn = true, activeAt = 123, snooze = 0)
        service.onStartCommand(Intent(service, AlarmService::class.java).apply {
            action = AlarmService.ACTION_UPDATE_ALARM
            putExtra(AlarmService.EXTRA_ALARM_JSON, Json.encodeToString(AlarmMapper().mapFromDomainModel(updated)))
        }, 0, 2)
        val notification = org.robolectric.Shadows.shadowOf(service).lastForegroundNotification
        assertTrue(notification.actions.isNullOrEmpty())
        assertEquals(1L, ActiveAlarmManager.activeAlarmId)
    }

    @Test fun duplicateDeliveryDoesNotCreateAnotherTimingController() {
        start(1)
        val field = AlarmService::class.java.getDeclaredField("timingController").apply { isAccessible = true }
        val original = field.get(service)
        start(1)
        assertSame(original, field.get(service))
        assertEquals(1L, ActiveAlarmManager.activeAlarmId)
    }
    private fun restoreNotification(id: Long) {
        service.onStartCommand(Intent(service, AlarmService::class.java).apply {
            action = AlarmService.ACTION_RESTORE_NOTIFICATION
            putExtra(AlarmService.EXTRA_ALARM_ID, id)
        }, 0, 101)
    }

    @Test fun removingTaskRestoresNotificationWithoutStoppingOrRestartingPlayback() {
        start(1)
        val field = AlarmService::class.java.getDeclaredField("timingController").apply { isAccessible = true }
        val original = field.get(service)
        service.onTaskRemoved(Intent())
        val notification = org.robolectric.Shadows.shadowOf(service).lastForegroundNotification
        assertNotNull(notification.contentIntent)
        assertNull(notification.fullScreenIntent)
        assertSame(original, field.get(service))
        assertEquals(1L, ActiveAlarmManager.activeAlarmId)
    }

    @Test fun restoringNotificationKeepsPlaybackAndProvidesReopenAction() {
        start(1)
        val field = AlarmService::class.java.getDeclaredField("timingController").apply { isAccessible = true }
        val original = field.get(service)
        restoreNotification(1)
        val notification = org.robolectric.Shadows.shadowOf(service).lastForegroundNotification
        assertNotNull(notification.contentIntent)
        assertNotNull(notification.deleteIntent)
        assertNull(notification.fullScreenIntent)
        assertTrue(notification.flags and android.app.Notification.FLAG_ONGOING_EVENT != 0)
        assertTrue(notification.flags and android.app.Notification.FLAG_AUTO_CANCEL == 0)
        assertSame(original, field.get(service))
        assertEquals(1L, ActiveAlarmManager.activeAlarmId)
    }

    @Test fun lateNotificationDismissalCannotRestartCompletedAlarmOrReplaceNextAlarm() {
        start(1)
        start(2)
        dismiss(1)
        val notification = org.robolectric.Shadows.shadowOf(service).lastForegroundNotification
        restoreNotification(1)
        assertSame(notification, org.robolectric.Shadows.shadowOf(service).lastForegroundNotification)
        assertEquals(2L, ActiveAlarmManager.activeAlarmId)
        dismiss(2)
        restoreNotification(2)
        val field = AlarmService::class.java.getDeclaredField("currentAlarm").apply { isAccessible = true }
        assertNull(field.get(service))
    }

    @Test fun unreadableDeviceToneFallsBackToBundledAudio() {
        val uri = android.net.Uri.parse("android.resource://${service.packageName}/${com.timilehinaregbesola.mathalarm.R.raw.alarm_fallback}")
        org.robolectric.shadows.ShadowMediaPlayer.addMediaInfo(
            org.robolectric.shadows.util.DataSource.toDataSource(service, uri),
            org.robolectric.shadows.ShadowMediaPlayer.MediaInfo(20_000, 0))
        start(1)
        org.robolectric.Shadows.shadowOf(android.os.Looper.getMainLooper()).idle()
        val field = AlarmService::class.java.getDeclaredField("mediaPlayer").apply { isAccessible = true }
        val player = field.get(service) as android.media.MediaPlayer
        assertTrue(player.isPlaying)
        assertEquals(org.robolectric.shadows.util.DataSource.toDataSource(service, uri),
            org.robolectric.Shadows.shadowOf(player).dataSource)
    }

}
