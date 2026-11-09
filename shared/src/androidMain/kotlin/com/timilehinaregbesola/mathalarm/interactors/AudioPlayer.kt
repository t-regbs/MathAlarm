package com.timilehinaregbesola.mathalarm.interactors

import android.content.Context
import android.media.RingtoneManager
import co.touchlab.kermit.Logger

class PlayerWrapper(
    context: Context,
    private val logger: Logger,
    private val bundledTone: String,
) : AudioPlayer {
    private val context = context.applicationContext
    private var playback: AndroidAudioPlayback? = null
    private var source = ""

    override fun init() = stop()
    override fun reset() { stop(); source = "" }
    override fun setDataSourceFromString(alarmtone: String) { source = alarmtone }

    override fun startAlarmAudio() = startAlarmAudio { error ->
        logger.e(error) { "Unable to play alarm preview" }
    }

    override fun startAlarmAudio(onFailure: (Throwable) -> Unit) {
        val owner = playback ?: AndroidAudioPlayback(context).also { playback = it }
        owner.play(
            listOfNotNull(source, RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)?.toString(), bundledTone),
            onSourceFailed = { logger.w(it) { "Preview audio source unavailable" } },
            onFailure = onFailure,
        )
    }

    override fun stop() {
        playback?.close()
        playback = null
    }
}
