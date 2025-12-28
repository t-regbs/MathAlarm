package com.timilehinaregbesola.mathalarm.interactors

// Multiplatform expect for audio player used by MathScreen/AlarmMathViewModel
// Minimal surface to match current usages
expect interface AudioPlayer {
    val currentPosition: Int
    val duration: Int

    fun init()
    fun startAlarmAudio()
    fun setPerceivedVolume(perceived: Float)
    fun stop()
    fun reset()
    // Source as platform-agnostic string (URI as String)
    fun setDataSourceFromString(alarmtone: String)
}
