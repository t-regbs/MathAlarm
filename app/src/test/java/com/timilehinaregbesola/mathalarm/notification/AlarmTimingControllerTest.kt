package com.timilehinaregbesola.mathalarm.notification

import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import io.mockk.slot
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class AlarmTimingControllerTest {
    
    private lateinit var onStartRinging: () -> Unit
    private lateinit var onPauseRinging: () -> Unit
    private lateinit var testScheduler: TestTimingScheduler
    private lateinit var controller: AlarmTimingController
    
    // Test scheduler that allows manual time advancement
    class TestTimingScheduler : AlarmTimingController.TimingScheduler {
        private val scheduledTasks = mutableMapOf<Runnable, Long>()
        private var currentTime = 0L
        
        override fun scheduleDelayed(task: Runnable, delayMillis: Long) {
            scheduledTasks[task] = currentTime + delayMillis
        }
        
        override fun cancel(task: Runnable) {
            scheduledTasks.remove(task)
        }
        
        fun advanceTimeBy(millis: Long) {
            currentTime += millis
            // Execute tasks that should have fired
            val toExecute = scheduledTasks.filter { it.value <= currentTime }.keys.toList()
            toExecute.forEach { task ->
                scheduledTasks.remove(task)
                task.run()
            }
        }
        
        fun hasScheduledTasks(): Boolean = scheduledTasks.isNotEmpty()
        
        fun getScheduledTaskCount(): Int = scheduledTasks.size
    }
    
    @Before
    fun setup() {
        onStartRinging = mockk(relaxed = true)
        onPauseRinging = mockk(relaxed = true)
        testScheduler = TestTimingScheduler()
        
        controller = AlarmTimingController(
            ringDurationMillis = 10_000L,  // 10 seconds for testing
            silencePeriodMillis = 2_000L,  // 2 seconds for testing
            onStartRinging = onStartRinging,
            onPauseRinging = onPauseRinging,
            scheduler = testScheduler
        )
    }
    
    @Test
    fun `start should trigger onStartRinging and set state to RINGING`() {
        controller.start()
        
        verify(exactly = 1) { onStartRinging() }
        assertEquals(AlarmTimingController.State.RINGING, controller.currentState)
    }
    
    @Test
    fun `start should schedule auto-pause task`() {
        controller.start()
        
        assert(testScheduler.hasScheduledTasks())
    }
    
    @Test
    fun `after ring duration, should pause and call onPauseRinging`() {
        controller.start()
        
        // Advance time past ring duration
        testScheduler.advanceTimeBy(10_000L)
        
        verify(exactly = 1) { onPauseRinging() }
        assertEquals(AlarmTimingController.State.PAUSED, controller.currentState)
    }
    
    @Test
    fun `after silence period, should restart ringing`() {
        controller.start()
        
        // Advance to pause
        testScheduler.advanceTimeBy(10_000L)
        verify(exactly = 1) { onPauseRinging() }
        
        // Advance past silence period
        testScheduler.advanceTimeBy(2_000L)
        
        // Should have called onStartRinging twice (initial + restart)
        verify(exactly = 2) { onStartRinging() }
        assertEquals(AlarmTimingController.State.RINGING, controller.currentState)
    }
    
    @Test
    fun `full cycle should repeat - ring, pause, ring, pause`() {
        controller.start()
        
        // First ring cycle
        verify(exactly = 1) { onStartRinging() }
        
        // After 10s - pause
        testScheduler.advanceTimeBy(10_000L)
        verify(exactly = 1) { onPauseRinging() }
        
        // After 2s silence - restart
        testScheduler.advanceTimeBy(2_000L)
        verify(exactly = 2) { onStartRinging() }
        
        // After another 10s - pause again
        testScheduler.advanceTimeBy(10_000L)
        verify(exactly = 2) { onPauseRinging() }
        
        // After 2s silence - restart again
        testScheduler.advanceTimeBy(2_000L)
        verify(exactly = 3) { onStartRinging() }
    }
    
    @Test
    fun `stop should cancel all tasks and set state to IDLE`() {
        controller.start()
        
        controller.stop()
        
        assertEquals(AlarmTimingController.State.IDLE, controller.currentState)
        // Advancing time should not trigger any callbacks
        testScheduler.advanceTimeBy(100_000L)
        verify(exactly = 1) { onStartRinging() }  // Only the initial call
        verify(exactly = 0) { onPauseRinging() }
    }
    
    @Test
    fun `stop during paused state should cancel restart`() {
        controller.start()
        
        // Advance to pause
        testScheduler.advanceTimeBy(10_000L)
        assertEquals(AlarmTimingController.State.PAUSED, controller.currentState)
        
        // Stop during pause
        controller.stop()
        
        // Advancing time should not restart
        testScheduler.advanceTimeBy(10_000L)
        verify(exactly = 1) { onStartRinging() }  // Only initial, no restart
    }
    
    @Test
    fun `calling start when already ringing should restart cycle`() {
        controller.start()
        verify(exactly = 1) { onStartRinging() }
        
        // Advance partway through ring duration
        testScheduler.advanceTimeBy(5_000L)
        
        // Call start again
        controller.start()
        
        // Should restart ringing
        verify(exactly = 2) { onStartRinging() }
        assertEquals(AlarmTimingController.State.RINGING, controller.currentState)
    }
    
    @Test
    fun `order of callbacks should be correct`() {
        controller.start()
        
        // Full cycle
        testScheduler.advanceTimeBy(10_000L)  // Pause
        testScheduler.advanceTimeBy(2_000L)   // Restart
        testScheduler.advanceTimeBy(10_000L)  // Pause again
        
        verifyOrder {
            onStartRinging()   // Initial
            onPauseRinging()   // After first ring
            onStartRinging()   // Restart
            onPauseRinging()   // After second ring
        }
    }
    
    @Test
    fun `initial state should be IDLE`() {
        assertEquals(AlarmTimingController.State.IDLE, controller.currentState)
    }
}
