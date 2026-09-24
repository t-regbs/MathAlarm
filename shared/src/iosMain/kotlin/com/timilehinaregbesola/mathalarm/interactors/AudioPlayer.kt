package com.timilehinaregbesola.mathalarm.interactors

import co.touchlab.kermit.Logger

/** Screen playback shares the same owner as native notification entry. */
class IosAudioPlayer(
    private val logger: Logger,
) : AudioPlayer {
    private var soundName = ""
    private var vibrate = false

    override fun init() = Unit

    override fun startAlarmAudio() {
        IosAlarmAudioManager.startAlarm(soundName, vibrate)
        logger.d { "Playing alarm audio: $soundName" }
    }

    override fun stop() = IosAlarmAudioManager.stopAlarm()

    override fun reset() {
        stop()
        soundName = ""
        vibrate = false
    }

    override fun setDataSourceFromString(alarmtone: String) {
        soundName = alarmtone
    }

    override fun setVibrate(enabled: Boolean) {
        vibrate = enabled
    }
}
