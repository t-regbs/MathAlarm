package com.timilehinaregbesola.mathalarm.audio

import android.app.Application
import android.net.Uri
import android.os.Looper
import com.timilehinaregbesola.mathalarm.interactors.AndroidAudioPlayback
import org.junit.After
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowMediaPlayer
import org.robolectric.shadows.util.DataSource
import java.io.IOException
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.CopyOnWriteArrayList

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30], application = Application::class, shadows = [RecordingMediaPlayer::class])
class AndroidAudioPlaybackTest {
    private val context = RuntimeEnvironment.getApplication()
    private val playback = AndroidAudioPlayback(context)
    private val releaseSource = CountDownLatch(1)
    @After fun cleanup() {
        releaseSource.countDown()
        RecordingMediaPlayer.beforeSource = null
        playback.close()
    }

    @Test fun blockedSourceDoesNotBlockMainAndStopPreventsLatePlayback() {
        val enteredSource = CountDownLatch(1)
        val players = CopyOnWriteArrayList<ShadowMediaPlayer>()
        ShadowMediaPlayer.setCreateListener { _, shadow -> players += shadow }
        RecordingMediaPlayer.calls.clear()
        RecordingMediaPlayer.beforeSource = {
            enteredSource.countDown()
            check(releaseSource.await(5, TimeUnit.SECONDS))
        }
        val source = "content://test/slow-tone"
        ShadowMediaPlayer.addMediaInfo(DataSource.toDataSource(context, Uri.parse(source)), ShadowMediaPlayer.MediaInfo(20_000, 0))
        var starts = 0
        playback.play(listOf(source), onStarted = { starts++ })
        assertTrue("Worker must reach the blocked platform call", enteredSource.await(2, TimeUnit.SECONDS))
        // The test is running on main. Reaching here proves source opening did not block it.
        playback.stop()
        releaseSource.countDown()
        await { RecordingMediaPlayer.calls.any { it.first == "release" } }
        shadowOf(Looper.getMainLooper()).idle()
        assertEquals(0, starts)
        assertFalse(players.any { it.isReallyPlaying })
        assertFalse(RecordingMediaPlayer.calls.any { it.second })
        // A subsequent alarm must still be able to play after the cancelled operation unwinds.
        playback.play(listOf(source), onStarted = { starts++ })
        await { starts == 1 }
    }

    @Test fun exhaustedSourcesReportOneFailureOnMain() {
        val source = "content://test/missing"
        ShadowMediaPlayer.addException(DataSource.toDataSource(context, Uri.parse(source)), IOException("missing"))
        var failures = 0
        playback.play(listOf(source), onFailure = {
            assertSame(Looper.getMainLooper(), Looper.myLooper())
            failures++
        })
        await { failures == 1 }
        shadowOf(Looper.getMainLooper()).idle()
        assertEquals(1, failures)
    }

    @Test fun permissionDeniedSourceFallsBackToBundledTone() {
        val denied = "content://test/denied"
        val bundled = "android.resource://${context.packageName}/${com.timilehinaregbesola.mathalarm.R.raw.alarm_fallback}"
        ShadowMediaPlayer.addException(DataSource.toDataSource(context, Uri.parse(denied)), SecurityException("permission denied"))
        ShadowMediaPlayer.addMediaInfo(DataSource.toDataSource(context, Uri.parse(bundled)), ShadowMediaPlayer.MediaInfo(20_000, 0))
        var started = false
        var errors = 0
        playback.play(listOf(denied, bundled), onStarted = { started = true }, onSourceFailed = { errors++ })
        await { started }
        assertEquals(1, errors)
    }

    @Test fun asynchronousMediaErrorFallsBackAndStaleCallbacksCannotAffectReplacement() {
        val players = CopyOnWriteArrayList<ShadowMediaPlayer>()
        ShadowMediaPlayer.setCreateListener { _, shadow -> players += shadow }
        val sources = listOf("content://test/first", "content://test/fallback")
        sources.forEach { source ->
            ShadowMediaPlayer.addMediaInfo(DataSource.toDataSource(context, Uri.parse(source)), ShadowMediaPlayer.MediaInfo(20_000, 0))
        }
        var starts = 0
        var failures = 0
        playback.play(sources, onStarted = { starts++ }, onSourceFailed = { failures++ })
        await { starts == 1 }
        val first = players.first()
        first.handler.post { first.invokeErrorListener(100, 0) }
        await { starts == 2 }
        assertEquals(1, failures)
        val staleDelivered = CountDownLatch(1)
        first.handler.post {
            first.invokePreparedListener()
            first.invokeErrorListener(100, 0)
            staleDelivered.countDown()
        }
        assertTrue(staleDelivered.await(2, TimeUnit.SECONDS))
        shadowOf(Looper.getMainLooper()).idle()
        assertEquals(2, starts)
        assertTrue(players.last().isReallyPlaying)
    }

    @Test fun preparedCallbackAfterStopCannotRestartAudio() {
        val players = CopyOnWriteArrayList<ShadowMediaPlayer>()
        ShadowMediaPlayer.setCreateListener { _, shadow -> players += shadow }
        RecordingMediaPlayer.calls.clear()
        val source = "content://test/preparing"
        ShadowMediaPlayer.addMediaInfo(DataSource.toDataSource(context, Uri.parse(source)), ShadowMediaPlayer.MediaInfo(20_000, 10_000))
        var starts = 0
        playback.play(listOf(source), onStarted = { starts++ })
        await { players.firstOrNull()?.state == ShadowMediaPlayer.State.PREPARING }
        val old = players.first()
        playback.stop()
        await { RecordingMediaPlayer.calls.any { it.first == "release" } }
        val delivered = CountDownLatch(1)
        old.handler.post { old.invokePreparedListener(); delivered.countDown() }
        assertTrue(delivered.await(2, TimeUnit.SECONDS))
        shadowOf(Looper.getMainLooper()).idle()
        assertEquals(0, starts)
        assertFalse(old.isReallyPlaying)
    }

    private fun await(condition: () -> Boolean) {
        val deadline = System.nanoTime() + 2_000_000_000
        while (!condition() && System.nanoTime() < deadline) {
            shadowOf(Looper.getMainLooper()).idle()
            Thread.sleep(10)
        }
        assertTrue("Timed out waiting for audio", condition())
    }
}
