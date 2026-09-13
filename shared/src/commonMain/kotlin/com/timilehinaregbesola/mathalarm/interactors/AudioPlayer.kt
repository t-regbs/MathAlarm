package com.timilehinaregbesola.mathalarm.interactors

/** Screen-owned playback. Android's ringing service manages scheduled alarm audio separately. */
interface AudioPlayer {
    fun init()
    fun startAlarmAudio()
    fun stop()
    fun reset()
    fun setDataSourceFromString(alarmtone: String)
}
