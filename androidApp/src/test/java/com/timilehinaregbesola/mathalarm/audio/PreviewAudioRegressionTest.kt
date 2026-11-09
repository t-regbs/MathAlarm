package com.timilehinaregbesola.mathalarm.audio

import android.app.Application
import android.media.RingtoneManager
import android.net.Uri
import android.os.Looper
import co.touchlab.kermit.Logger
import com.timilehinaregbesola.mathalarm.interactors.PlayerWrapper
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowMediaPlayer
import org.robolectric.shadows.util.DataSource
import java.io.IOException
import java.util.concurrent.CopyOnWriteArrayList

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30], application = Application::class, shadows = [RecordingMediaPlayer::class])
class PreviewAudioRegressionTest {
    private val context get() = RuntimeEnvironment.getApplication()
    private val players = CopyOnWriteArrayList<ShadowMediaPlayer>()
    private lateinit var audio: PlayerWrapper
    @Before fun setup() {
        RecordingMediaPlayer.calls.clear()
        ShadowMediaPlayer.setCreateListener { _, shadow -> players += shadow }
        audio = PlayerWrapper(context, Logger.withTag("preview-test"),
            "android.resource://${context.packageName}/${com.timilehinaregbesola.mathalarm.R.raw.alarm_fallback}")
    }
    @After fun cleanup() { audio.stop() }

    @Test fun unavailableSelectedToneStillPlaysDefaultTone() {
        val missing = Uri.parse("content://missing/selected-tone")
        ShadowMediaPlayer.addException(DataSource.toDataSource(context, missing), IOException("unavailable"))
        val fallback = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
        ShadowMediaPlayer.addMediaInfo(DataSource.toDataSource(context, fallback), ShadowMediaPlayer.MediaInfo(20_000, 0))
        audio.init()
        audio.setDataSourceFromString(missing.toString())
        audio.startAlarmAudio()
        awaitPlayback()
        assertTrue("An inaccessible selected tone must not silently mute the preview", players.any { it.isReallyPlaying })
    }

    @Test fun sourceOpeningAndReleaseDoNotRunOnMainThread() {
        val tone = Uri.parse("content://test/valid-tone")
        ShadowMediaPlayer.addMediaInfo(DataSource.toDataSource(context, tone), ShadowMediaPlayer.MediaInfo(20_000, 0))
        audio.init()
        audio.setDataSourceFromString(tone.toString())
        audio.startAlarmAudio()
        awaitPlayback()
        audio.stop()
        val deadline = System.nanoTime() + 2_000_000_000
        while (players.any { it.state != ShadowMediaPlayer.State.END } && System.nanoTime() < deadline) Thread.sleep(10)
        assertTrue(players.all { it.state == ShadowMediaPlayer.State.END })
        assertTrue(RecordingMediaPlayer.calls.any { it.first == "release" })
        assertFalse("Media calls on main: ${RecordingMediaPlayer.calls}", RecordingMediaPlayer.calls.any { it.second })
    }

    private fun awaitPlayback() {
        val deadline = System.nanoTime() + 1_000_000_000
        do {
            shadowOf(Looper.getMainLooper()).idle()
            if (players.any { it.isReallyPlaying }) return
            Thread.sleep(10)
        } while (System.nanoTime() < deadline)
    }
}
