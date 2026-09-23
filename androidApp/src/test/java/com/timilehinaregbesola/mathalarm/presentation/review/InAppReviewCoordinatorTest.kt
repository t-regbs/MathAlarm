package com.timilehinaregbesola.mathalarm.presentation.review

import android.app.Activity
import com.google.android.gms.tasks.TaskCompletionSource
import com.google.android.gms.tasks.Tasks
import com.google.android.play.core.review.ReviewInfo
import com.google.android.play.core.review.ReviewManager
import com.timilehinaregbesola.mathalarm.TestApplication
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import io.mockk.verifyOrder
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [30], application = TestApplication::class)
class InAppReviewCoordinatorTest {
    private val store = mockk<ReviewEligibilityStore>(relaxed = true) {
        every { isEligible() } returns true
        every { lastCompletionAt() } returns 1000L
    }
    private val info = mockk<ReviewInfo>()
    private val activity = mockk<Activity>()
    private val manager = mockk<ReviewManager> {
        every { requestReviewFlow() } returns Tasks.forResult(info)
        every { launchReviewFlow(any(), any()) } returns Tasks.forResult(null)
    }
    private var pendingAlarm = false
    private fun coordinator() = InAppReviewCoordinator(store, manager) { pendingAlarm }

    @Test
    fun `visit opened for an alarm can become a normal visit after backgrounding`() = runTest {
        val session = ReviewVisitViewModel()
        session.onStart { null }
        session.onStop(changingConfiguration = true)
        session.onStart { error("An alarm visit stays suppressed across rotation") }
        org.junit.Assert.assertNull(session.coordinator)
        session.onStop(changingConfiguration = false)
        session.onStart { coordinator() }
        session.coordinator!!.request(activity) { true }
        verify(exactly = 1) { manager.requestReviewFlow() }
    }

    @Test
    fun `rotation retains eligibility snapshot while returning from background starts a visit`() = runTest {
        val session = ReviewVisitViewModel()
        every { store.isEligible() } returns false
        session.onStart { coordinator() }
        val original = session.coordinator
        every { store.isEligible() } returns true
        session.onStop(changingConfiguration = true)
        session.onStart { error("Rotation must keep the existing visit") }
        org.junit.Assert.assertSame(original, session.coordinator)
        org.junit.Assert.assertEquals(1, session.visit)
        session.coordinator!!.request(activity) { true }
        verify(exactly = 0) { manager.requestReviewFlow() }

        session.onStop(changingConfiguration = false)
        session.onStart { coordinator() }
        org.junit.Assert.assertEquals(2, session.visit)
        session.coordinator!!.request(activity) { true }
        verify(exactly = 1) { manager.requestReviewFlow() }
    }

    @Test
    fun `records attempt before Play and only requests once per visit`() = runTest {
        val coordinator = coordinator()
        coordinator.request(activity) { true }
        coordinator.request(activity) { true }
        verifyOrder {
            store.recordAttempt()
            manager.requestReviewFlow()
            manager.launchReviewFlow(activity, info)
        }
        verify(exactly = 1) { manager.requestReviewFlow() }
    }

    @Test
    fun `eligibility gained during this visit waits for another visit`() = runTest {
        every { store.isEligible() } returns false
        val coordinator = coordinator()
        every { store.isEligible() } returns true
        coordinator.request(activity) { true }
        verify(exactly = 0) { manager.requestReviewFlow() }
    }

    @Test
    fun `active or snoozed alarm and blocked UI prevent requests`() = runTest {
        val coordinator = coordinator()
        pendingAlarm = true
        coordinator.request(activity) { true }
        pendingAlarm = false
        coordinator.request(activity) { false }
        verify(exactly = 0) { store.recordAttempt() }
        verify(exactly = 0) { manager.requestReviewFlow() }
    }

    @Test
    fun `alarm completed during this visit prevents prompting`() = runTest {
        val coordinator = coordinator()
        every { store.lastCompletionAt() } returns 2000L
        coordinator.request(activity) { true }
        verify(exactly = 0) { manager.requestReviewFlow() }
    }

    @Test
    fun `navigation while Play loads prevents launch`() = runTest {
        val response = TaskCompletionSource<ReviewInfo>()
        every { manager.requestReviewFlow() } returns response.task
        val coordinator = coordinator()
        var safe = true
        val job = launch { coordinator.request(activity) { safe } }
        runCurrent()
        safe = false
        response.setResult(info)
        job.join()
        verify(exactly = 0) { manager.launchReviewFlow(any(), any()) }
    }

    @Test
    fun `alarm starting while Play loads prevents launch`() = runTest {
        val response = TaskCompletionSource<ReviewInfo>()
        every { manager.requestReviewFlow() } returns response.task
        val coordinator = coordinator()
        val job = launch { coordinator.request(activity) { true } }
        runCurrent()
        pendingAlarm = true
        response.setResult(info)
        job.join()
        verify(exactly = 0) { manager.launchReviewFlow(any(), any()) }
    }

    @Test
    fun `backgrounding invalidates pending response even after returning`() = runTest {
        val response = TaskCompletionSource<ReviewInfo>()
        every { manager.requestReviewFlow() } returns response.task
        val coordinator = coordinator()
        val job = launch { coordinator.request(activity) { true } }
        runCurrent()
        coordinator.invalidatePendingRequest()
        response.setResult(info)
        job.join()
        verify(exactly = 0) { manager.launchReviewFlow(any(), any()) }
    }

    @Test
    fun `Play failure returns normally and does not retry`() = runTest {
        every { manager.requestReviewFlow() } returns Tasks.forException(IllegalStateException("Unavailable"))
        val coordinator = coordinator()
        coordinator.request(activity) { true }
        coordinator.request(activity) { true }
        verify(exactly = 1) { store.recordAttempt() }
        verify(exactly = 1) { manager.requestReviewFlow() }
        verify(exactly = 0) { manager.launchReviewFlow(any(), any()) }
    }
}
