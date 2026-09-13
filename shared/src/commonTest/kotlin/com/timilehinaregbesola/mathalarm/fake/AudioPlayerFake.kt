package com.timilehinaregbesola.mathalarm.fake

import com.timilehinaregbesola.mathalarm.interactors.AudioPlayer

class AudioPlayerFake : AudioPlayer {
    var isInitialized = false
    var isReset = false
    var dataSource: String? = null
    private var _isPlaying = false
    val isPlaying: Boolean get() = _isPlaying
    var isStopped = false

    override fun init() {
        isInitialized = true
    }

    override fun reset() {
        isReset = true
        dataSource = null
    }

    override fun setDataSourceFromString(alarmtone: String) {
        dataSource = alarmtone
    }

    override fun startAlarmAudio() {
        _isPlaying = true
        isStopped = false
    }

    override fun stop() {
        isStopped = true
        _isPlaying = false
    }
}
