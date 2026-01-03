package com.timilehinaregbesola.mathalarm.notification

/**
 * Controls the timing state machine for alarm ringing behavior.
 * Extracted from AlarmService for testability.
 * 
 * State flow:
 * IDLE -> RINGING (start) -> PAUSED (after ringDuration) -> RINGING (after silencePeriod) -> ...
 */
class AlarmTimingController(
    private val ringDurationMillis: Long = DEFAULT_RING_DURATION_MILLIS,
    private val silencePeriodMillis: Long = DEFAULT_SILENCE_PERIOD_MILLIS,
    private val onStartRinging: () -> Unit,
    private val onPauseRinging: () -> Unit,
    private val scheduler: TimingScheduler
) {
    
    enum class State {
        IDLE,
        RINGING,
        PAUSED
    }
    
    var currentState: State = State.IDLE
        private set
    
    private var ringTimeoutTask: Runnable? = null
    private var restartTask: Runnable? = null
    
    /**
     * Start the alarm - begins ringing and schedules auto-pause.
     */
    fun start() {
        if (currentState != State.IDLE) {
            // Already running, just restart the ringing
            restartRinging()
            return
        }
        
        currentState = State.RINGING
        onStartRinging()
        scheduleAutoPause()
    }
    
    /**
     * Stop the alarm completely - cancels all scheduled tasks.
     */
    fun stop() {
        cancelAllTasks()
        currentState = State.IDLE
    }
    
    /**
     * Called when ring duration expires - pauses audio and schedules restart.
     */
    private fun pauseRinging() {
        if (currentState != State.RINGING) return
        
        currentState = State.PAUSED
        onPauseRinging()
        scheduleRestart()
    }
    
    /**
     * Called after silence period - restarts audio and schedules next pause.
     */
    private fun restartRinging() {
        currentState = State.RINGING
        onStartRinging()
        scheduleAutoPause()
    }
    
    private fun scheduleAutoPause() {
        ringTimeoutTask?.let { scheduler.cancel(it) }
        ringTimeoutTask = Runnable { pauseRinging() }
        scheduler.scheduleDelayed(ringTimeoutTask!!, ringDurationMillis)
    }
    
    private fun scheduleRestart() {
        restartTask?.let { scheduler.cancel(it) }
        restartTask = Runnable { restartRinging() }
        scheduler.scheduleDelayed(restartTask!!, silencePeriodMillis)
    }
    
    private fun cancelAllTasks() {
        ringTimeoutTask?.let { scheduler.cancel(it) }
        restartTask?.let { scheduler.cancel(it) }
        ringTimeoutTask = null
        restartTask = null
    }
    
    /**
     * Interface for scheduling - allows injection of test scheduler.
     */
    interface TimingScheduler {
        fun scheduleDelayed(task: Runnable, delayMillis: Long)
        fun cancel(task: Runnable)
    }
    
    companion object {
        const val DEFAULT_RING_DURATION_MILLIS = 10 * 60 * 1000L  // 10 minutes
        const val DEFAULT_SILENCE_PERIOD_MILLIS = 1 * 60 * 1000L  // 1 minute
    }
}
