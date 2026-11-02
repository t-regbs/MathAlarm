package com.timilehinaregbesola.mathalarm.sound

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

class TonePreviewOwnershipTest {
    @Test fun synchronousReplacementCallbackCannotOverwriteItsNewerRequest() {
        val lease = TonePreviewOwnership()
        lease.begin("draft-a") {
            lease.nextRequest()
            lease.begin("draft-c") {}
        }
        val replacementRequest = lease.nextRequest()
        lease.finish(TonePreviewResult.REPLACED)
        assertFalse(lease.isCurrentRequest(replacementRequest))
        assertTrue(lease.owns("draft-c"))
    }

    @Test fun interruptionCallbackCannotAcquirePreviewAfterRealPriorityIsEstablished() {
        val lease = TonePreviewOwnership()
        var realActive = false
        var blocked = false
        lease.begin("draft-a") {
            blocked = lease.rejectWhileAlarmActive(realActive) { result ->
                assertEquals(TonePreviewResult.BLOCKED_BY_ALARM, result)
            }
            if (!blocked) lease.begin("draft-b") {}
        }
        realActive = true
        lease.nextRequest()
        lease.finish(TonePreviewResult.INTERRUPTED_BY_ALARM)
        assertTrue(blocked)
        assertFalse(lease.owns("draft-b"))
    }

    @Test fun realAudioPriorityRejectsTonePlaybackWithoutClaimingAnOwner() {
        val lease = TonePreviewOwnership()
        val outcomes = mutableListOf<TonePreviewResult>()
        assertTrue(lease.rejectWhileAlarmActive(true, outcomes::add))
        assertFalse(lease.owns("draft"))
        assertEquals(listOf(TonePreviewResult.BLOCKED_BY_ALARM), outcomes)
        assertFalse(lease.rejectWhileAlarmActive(false, outcomes::add))
        assertEquals(listOf(TonePreviewResult.BLOCKED_BY_ALARM), outcomes)
    }

    @Test fun outgoingPickerCannotReleaseAnotherPickersPreview() {
        val lease = TonePreviewOwnership()
        val outcomes = mutableListOf<TonePreviewResult>()
        lease.begin("draft-a", outcomes::add)
        lease.finish(TonePreviewResult.REPLACED)
        lease.begin("draft-b", outcomes::add)
        if (lease.owns("draft-a")) lease.finish(TonePreviewResult.STOPPED)
        assertTrue(lease.owns("draft-b"))
        assertEquals(listOf(TonePreviewResult.REPLACED), outcomes)
        lease.finish(TonePreviewResult.FINISHED)
        assertFalse(lease.owns("draft-b"))
    }

    @Test fun realAlarmInterruptionReleasesPreviewOnce() {
        val lease = TonePreviewOwnership()
        val outcomes = mutableListOf<TonePreviewResult>()
        lease.begin("draft-a", outcomes::add)
        lease.finish(TonePreviewResult.INTERRUPTED_BY_ALARM)
        if (lease.owns("draft-a")) lease.finish(TonePreviewResult.STOPPED)
        lease.finish(TonePreviewResult.STOPPED)
        assertEquals(listOf(TonePreviewResult.INTERRUPTED_BY_ALARM), outcomes)
    }

    @Test fun playbackFailureLeavesNoPreviewOwner() {
        val lease = TonePreviewOwnership()
        var outcome: TonePreviewResult? = null
        lease.begin("draft", { outcome = it })
        lease.finish(TonePreviewResult.UNAVAILABLE)
        assertEquals(TonePreviewResult.UNAVAILABLE, outcome)
        assertFalse(lease.owns("draft"))
    }

    @Test fun completionCanInstallANewOwnerWithoutOldCleanupErasingIt() {
        val lease = TonePreviewOwnership()
        lease.begin("draft-a") { lease.begin("draft-b") {} }
        lease.finish(TonePreviewResult.FINISHED)
        assertTrue(lease.owns("draft-b"))
        assertFalse(lease.owns("draft-a"))
    }
}
