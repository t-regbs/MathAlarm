package com.timilehinaregbesola.mathalarm.platform

/** Application-owned Test Alarm vibration; renderer disposal does not own this work. */
internal interface PreviewVibration {
    fun start()
    fun stop()
}
internal class PlatformPreviewVibration : PreviewVibration {
    private val vibrator = PlatformVibrator()
    override fun start() = vibrator.startWaveform(longArrayOf(0, 1000, 3000), 0)
    override fun stop() = vibrator.cancel()
}
internal object NoPreviewVibration : PreviewVibration {
    override fun start() = Unit
    override fun stop() = Unit
}
