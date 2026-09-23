package com.timilehinaregbesola.mathalarm.presentation.review

import android.app.Activity
import co.touchlab.kermit.Logger
import com.timilehinaregbesola.mathalarm.analytics.AnalyticsEvents
import com.timilehinaregbesola.mathalarm.analytics.AnalyticsTracker
import com.timilehinaregbesola.mathalarm.analytics.NoopAnalyticsTracker
import com.timilehinaregbesola.mathalarm.analytics.trackSafely
import com.google.android.gms.tasks.Task
import com.google.android.play.core.review.ReviewManager
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import java.util.concurrent.Executor

/** Owned by one Activity visit; never retains ReviewInfo for a later screen or visit. */
class InAppReviewCoordinator(
    private val store: ReviewEligibilityStore,
    private val manager: ReviewManager,
    private val analytics: AnalyticsTracker = NoopAnalyticsTracker,
    private val hasPendingAlarm: suspend () -> Boolean,
) {
    private val eligibleAtStart = store.isEligible()
    private val completionAtStart = store.lastCompletionAt()
    private var attempted = false
    private var inFlight = false
    private var generation = 0

    fun invalidatePendingRequest() { generation++ }

    suspend fun request(activity: Activity, canShow: () -> Boolean) {
        if (!eligibleAtStart || attempted || inFlight) return
        inFlight = true
        var outcome = "request_failed"
        var recordedAttempt = false
        val requestGeneration = generation
        fun isSafe() = generation == requestGeneration && canShow() &&
            store.lastCompletionAt() == completionAtStart
        try {
            if (!store.isEligible()) return
            if (!isSafe() || hasPendingAlarm() || !isSafe()) return
            if (!store.isEligible()) return
            attempted = true
            // Apply our cooldown even if Play fails or elects not to display its card.
            store.recordAttempt()
            recordedAttempt = true
            analytics.trackSafely(AnalyticsEvents.reviewAttempted())
            val info = manager.requestReviewFlow().awaitResult()
            // The user may navigate away or an alarm may ring while Play responds.
            if (!isSafe() || hasPendingAlarm() || !isSafe()) {
                outcome = "context_changed"
                return
            }
            outcome = "launch_failed"
            manager.launchReviewFlow(activity, info).awaitResult()
            outcome = "flow_finished"
            // Completion cannot tell us whether a card appeared or a review was submitted.
        } catch (e: CancellationException) {
            outcome = "cancelled"
            throw e
        } catch (e: Exception) {
            Logger.w(e) { "Unable to request in-app review" }
        } finally {
            if (recordedAttempt) analytics.trackSafely(AnalyticsEvents.reviewOutcome(outcome))
            inFlight = false
        }
    }
}

private suspend fun <T> Task<T>.awaitResult(): T = suspendCancellableCoroutine { continuation ->
    addOnCompleteListener(Executor { it.run() }) { task ->
        if (continuation.isActive) {
            if (task.isSuccessful) continuation.resume(task.result)
            else continuation.resumeWithException(task.exception ?: IllegalStateException("Review task cancelled"))
        }
    }
}
