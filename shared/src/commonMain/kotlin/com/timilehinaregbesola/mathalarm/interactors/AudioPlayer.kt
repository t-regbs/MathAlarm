package com.timilehinaregbesola.mathalarm.interactors

/** Application/preview playback. Android's ringing service owns scheduled alarm audio separately. */
interface AudioPlayer {
    fun init()
    fun startAlarmAudio()
    fun startAlarmAudio(onFailure: (Throwable) -> Unit) { startAlarmAudio() }
    fun stop()
    fun reset()
    fun setDataSourceFromString(alarmtone: String)
    fun setVibrate(enabled: Boolean) = Unit
}
