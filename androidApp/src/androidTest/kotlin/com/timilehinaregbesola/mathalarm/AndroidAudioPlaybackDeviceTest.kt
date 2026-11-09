package com.timilehinaregbesola.mathalarm

import androidx.test.platform.app.InstrumentationRegistry
import com.timilehinaregbesola.mathalarm.interactors.AndroidAudioPlayback
import org.junit.Assert.*
import org.junit.Test
import java.util.concurrent.CountDownLatch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

/** Real MediaPlayer/ContentResolver verification; no UI automation or synthetic alarm delivery. */
class AndroidAudioPlaybackDeviceTest {
    @Test fun missingContentUriFallsBackToBundledAudio() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val playback = AndroidAudioPlayback(context)
        val started = CountDownLatch(1)
        val failures = AtomicInteger()
        val sourceFailures = AtomicInteger()
        try {
            instrumentation.runOnMainSync {
                playback.play(listOf("content://missing/reliability-tone",
                    "android.resource://${context.packageName}/${R.raw.alarm_fallback}"),
                    onStarted = { started.countDown() },
                    onSourceFailed = { sourceFailures.incrementAndGet() },
                    onFailure = { failures.incrementAndGet() })
            }
            assertTrue("Bundled fallback must reach MediaPlayer.start", started.await(10, TimeUnit.SECONDS))
            assertEquals(1, sourceFailures.get())
            assertEquals(0, failures.get())
        } finally { instrumentation.runOnMainSync { playback.close() } }
    }

    @Test fun stoppingQueuedPlaybackPreventsItsStartAndAllowsNextAlarm() {
        val instrumentation = InstrumentationRegistry.getInstrumentation()
        val context = instrumentation.targetContext
        val playback = AndroidAudioPlayback(context)
        val source = "android.resource://${context.packageName}/${R.raw.alarm_fallback}"
        val cancelledStarts = AtomicInteger()
        val nextStarted = CountDownLatch(1)
        try {
            instrumentation.runOnMainSync {
                repeat(20) {
                    playback.play(listOf(source), onStarted = { cancelledStarts.incrementAndGet() })
                    playback.stop()
                }
                playback.play(listOf(source), onStarted = { nextStarted.countDown() })
            }
            assertTrue("Next alarm must play after cancellation", nextStarted.await(10, TimeUnit.SECONDS))
            assertEquals(0, cancelledStarts.get())
        } finally { instrumentation.runOnMainSync { playback.close() } }
    }
}
