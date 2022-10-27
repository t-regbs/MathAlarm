package com.timilehinaregbesola.mathalarm.interactors

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import timber.log.Timber

interface AudioPlayer {
    fun init()
    fun startAlarmAudio()

//    fun setDataSourceFromResource(res: Int)
    fun setPerceivedVolume(perceived: Float)

    /**
     * Stops alarm audio
     */
    fun stop()

    fun reset()
    fun setDataSource(alarmtone: Uri)
}

class PlayerWrapper(
//    val resources: Resources,
    val context: Context
) : AudioPlayer {
    override fun setDataSource(alarmtone: Uri) {
        // Fall back on the default alarm if the database does not have an
        // alarm stored.
        player?.setDataSource(context, alarmtone)
    }

    private var player: MediaPlayer? = null
        private set

    override fun init() {
        player = MediaPlayer().apply {
            setOnErrorListener { mp, _, _ ->
                Timber.e("Error occurred while playing audio.")
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
                    .build()
            )
            prepare()
            isLooping = true
            start()
        }
    }

//    override fun setDataSourceFromResource(res: Int) {
//        resources.openRawResourceFd(res)?.run {
//            player?.setDataSource(fileDescriptor, startOffset, length)
//            close()
//        }
//    }

    override fun setPerceivedVolume(perceived: Float) {
        val volume = perceived.squared()
        player?.setVolume(volume, volume)
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

    private fun Float.squared() = this * this
}
