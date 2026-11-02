package com.timilehinaregbesola.mathalarm.interactors

/** Platform playback intent survives a rejected start, but never an owner stop. */
internal class IosAlarmPlaybackRecovery(
    private val isPlaying: () -> Boolean,
    private val activate: () -> Boolean,
    private val play: () -> Boolean,
) {
    private var revision = 0L
    var request: Long? = null
        private set
    var interrupted = false
        private set

    fun begin(): Long = (++revision).also { request = it }
    fun stop() { ++revision; request = null }
    fun interruptionBegan() { interrupted = true }
    fun interruptionEnded() { interrupted = false }
    fun owns(token: Long): Boolean = request == token

    fun attempt(token: Long): Boolean {
        if (!owns(token) || interrupted) return false
        if (isPlaying()) return true // A lifecycle event must not restart a healthy loop.
        if (!activate() || !owns(token) || interrupted) return false
        return play() && owns(token)
    }
}
