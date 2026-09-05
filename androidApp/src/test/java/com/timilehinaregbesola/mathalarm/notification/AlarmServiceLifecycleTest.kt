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
    private fun start(id: Long, at: Long = 123) {
        val alarm = Alarm(alarmId = id, isOn = true, activeAt = at)
        service.onStartCommand(Intent(service, AlarmService::class.java).apply {
            action = AlarmService.ACTION_START_ALARM
            putExtra(AlarmService.EXTRA_ALARM_JSON, Json.encodeToString(AlarmMapper().mapFromDomainModel(alarm)))
        }, 0, id.toInt())
    }
    private fun dismiss(id: Long) {
        service.onStartCommand(Intent(service, AlarmService::class.java).apply {
            action = AlarmService.ACTION_STOP_ALARM
            putExtra(AlarmService.EXTRA_ALARM_ID, id)
        }, 0, 100)
    }
    @After fun cleanup() { controller.destroy() }

    @Test fun overlappingAlarmWaitsUntilCurrentAlarmIsDismissed() {
        start(1)
        start(2)
        assertEquals(1L, ActiveAlarmManager.activeAlarmId)
        dismiss(1)
        assertEquals(2L, ActiveAlarmManager.activeAlarmId)
        dismiss(1)
        assertEquals(2L, ActiveAlarmManager.activeAlarmId)
    }

    @Test fun duplicateDeliveryDoesNotCreateAnotherTimingController() {
        start(1)
        val field = AlarmService::class.java.getDeclaredField("timingController").apply { isAccessible = true }
        val original = field.get(service)
        start(1)
        assertSame(original, field.get(service))
        assertEquals(1L, ActiveAlarmManager.activeAlarmId)
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
