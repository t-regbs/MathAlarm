package com.timilehinaregbesola.mathalarm.interactors

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Handler
import android.os.HandlerThread
import android.os.Looper
import android.os.PowerManager
import androidx.core.net.toUri
import java.util.concurrent.atomic.AtomicLong

/** Owns every MediaPlayer call on one Looper; notifications and application state stay on main. */
class AndroidAudioPlayback(context: Context) {
    private val context = context.applicationContext
    private val main = Handler(Looper.getMainLooper())
    private val generation = AtomicLong()
    // Start lazily: constructing an inactive service must not allocate a playback thread.
    private var thread: HandlerThread? = null
    private var handler: Handler? = null
    private var closed = false
    // Accessed only on the playback thread.
    private var player: MediaPlayer? = null

    @Synchronized
    fun play(
        sources: List<String>,
        onStarted: () -> Unit = {},
        onSourceFailed: (Throwable) -> Unit = {},
        onFailure: (Throwable) -> Unit = {},
    ) {
        check(!closed)
        val token = generation.incrementAndGet()
        val worker = handler ?: HandlerThread("MathAlarm-audio").also { it.start(); thread = it }
            .let { Handler(it.looper).also { handler = it } }
        val candidates = sources.filter { it.isNotBlank() }.distinct()
        fun deliver(callback: () -> Unit) {
            main.post { if (generation.get() == token) callback() }
        }
        fun candidate(index: Int, previous: Throwable? = null) {
            if (generation.get() != token) return
            if (index >= candidates.size) {
                deliver { onFailure(previous ?: IllegalArgumentException("No alarm audio source")) }
                return
            }
            var active: MediaPlayer? = null
            fun failed(error: Throwable) {
                if (active !== player) return
                releasePlayer()
                if (generation.get() != token) return
                deliver { onSourceFailed(error) }
                candidate(index + 1, error)
            }
            try {
                active = MediaPlayer()
                val current = active
                player = current
                current.setAudioAttributes(AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_ALARM).build())
                current.setWakeMode(context, PowerManager.PARTIAL_WAKE_LOCK)
                current.isLooping = true
                current.setOnPreparedListener {
                    if (generation.get() != token || player !== it) return@setOnPreparedListener
                    try {
                        it.start()
                        deliver(onStarted)
                    } catch (error: Exception) { failed(error) }
                }
                current.setOnErrorListener { _, what, extra ->
                    failed(IllegalStateException("MediaPlayer error $what/$extra"))
                    true
                }
                current.setDataSource(context, candidates[index].toUri())
                if (generation.get() == token) current.prepareAsync()
                else releasePlayer()
            } catch (error: Exception) {
                // Construction can fail before player is assigned.
                if (active == null) {
                    deliver { onFailure(error) }
                } else failed(error)
            }
        }
        worker.post {
            releasePlayer()
            candidate(0)
        }
    }

    @Synchronized
    fun stop() {
        // Invalidate callbacks immediately, even if a source-opening Binder call is still blocked.
        generation.incrementAndGet()
        handler?.post { releasePlayer() }
    }

    @Synchronized
    fun close() {
        if (closed) return
        closed = true
        stop()
        thread?.quitSafely() // Queued release runs before the Looper exits; never join on main.
    }

    private fun releasePlayer() {
        val previous = player
        player = null
        // release is valid in every state, including Error and Preparing; stop is unnecessary.
        try { previous?.release() } catch (error: Exception) {
            co.touchlab.kermit.Logger.w(error) { "Unable to release alarm player" }
        }
    }
}
