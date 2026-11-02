package com.timilehinaregbesola.mathalarm.interactors

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class IosAlarmPlaybackRecoveryTest {
    @Test fun rejectedActivationRetainsRequestUntilForegroundRetry() {
        var ready = false
        var starts = 0
        val recovery = IosAlarmPlaybackRecovery({ false }, { ready }, { ++starts; true })
        val token = recovery.begin()
        assertFalse(recovery.attempt(token))
        assertEquals(0, starts)
        ready = true
        assertTrue(recovery.attempt(token))
        assertEquals(1, starts)
    }

    @Test fun rejectedPlayerStartDoesNotCountAsAcceptedPlayback() {
        var attempts = 0
        val recovery = IosAlarmPlaybackRecovery({ false }, { true }, { ++attempts >= 2 })
        val token = recovery.begin()
        assertFalse(recovery.attempt(token))
        assertTrue(recovery.attempt(token))
        assertEquals(2, attempts)
    }

    @Test fun healthyPlaybackIsNotRestartedByForegroundOrRouteEvents() {
        var playing = false
        var activations = 0
        var starts = 0
        val recovery = IosAlarmPlaybackRecovery({ playing }, { ++activations; true }, { ++starts; playing = true; true })
        val token = recovery.begin()
        repeat(4) { assertTrue(recovery.attempt(token)) }
        assertEquals(1, activations)
        assertEquals(1, starts)
    }

    @Test fun interruptedLoopWaitsForInterruptionEndBeforeResuming() {
        var starts = 0
        val recovery = IosAlarmPlaybackRecovery({ false }, { true }, { ++starts; true })
        val token = recovery.begin()
        recovery.interruptionBegan()
        assertFalse(recovery.attempt(token))
        assertEquals(0, starts)
        recovery.interruptionEnded()
        assertTrue(recovery.attempt(token))
        assertEquals(1, starts)
    }

    @Test fun completionOrSnoozeStopRejectsLateRecoveryEvents() {
        var starts = 0
        val recovery = IosAlarmPlaybackRecovery({ false }, { true }, { ++starts; true })
        val token = recovery.begin()
        recovery.interruptionBegan()
        recovery.stop()
        recovery.interruptionEnded()
        assertFalse(recovery.attempt(token))
        assertEquals(null, recovery.request)
        assertEquals(0, starts)
    }

    @Test fun replacedOrStoppedRequestCannotStartAfterSessionActivation() {
        var starts = 0
        lateinit var recovery: IosAlarmPlaybackRecovery
        recovery = IosAlarmPlaybackRecovery({ false }, { recovery.stop(); recovery.begin(); true }, { ++starts; true })
        val token = recovery.begin()
        assertFalse(recovery.attempt(token))
        assertFalse(recovery.owns(token))
        assertEquals(0, starts)
    }
}
