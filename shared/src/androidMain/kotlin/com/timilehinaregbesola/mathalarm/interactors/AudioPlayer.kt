package com.timilehinaregbesola.mathalarm.interactors

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import androidx.core.net.toUri
import co.touchlab.kermit.Logger

class PlayerWrapper(
    val context: Context,
    private val logger: Logger
) : AudioPlayer {

    private var player: MediaPlayer? = null
        private set
    override fun init() {
        player = MediaPlayer().apply {
            setOnErrorListener { mp, _, _ ->
                logger.e("Error occurred while playing audio.")
                mp.stop()
                mp.release()
                player = null
                true
            }
        }
    }

    override fun startAlarmAudio() {
        player?.runCatching {
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .build(),
            )
            prepare()
            isLooping = true
            start()
        }
    }

    /**
     * Stops alarm audio
     */
    override fun stop() {
        try {
            player?.run {
                if (isPlaying) stop()
                release()
            }
        } finally {
            player = null
        }
    }

    override fun reset() {
        player?.reset()
    }

    override fun setDataSourceFromString(alarmtone: String) {
        try {
            player?.setDataSource(context, alarmtone.toUri())
        } catch (e: Exception) {
            logger.e(e) { "Failed to parse alarm tone URI: $alarmtone" }
        }
    }
}
