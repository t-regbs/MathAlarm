package com.timilehinaregbesola.mathalarm.analytics

import com.timilehinaregbesola.mathalarm.domain.model.Alarm

/** Only fixed event names and non-identifying values belong here. */
data class AnalyticsEvent(
    val name: String,
    val labels: Map<String, String> = emptyMap(),
    val counts: Map<String, Long> = emptyMap(),
)

fun interface AnalyticsTracker {
    fun track(event: AnalyticsEvent)
}

object NoopAnalyticsTracker : AnalyticsTracker {
    override fun track(event: AnalyticsEvent) = Unit
}

/** Analytics must never interrupt an alarm or a user action. */
fun AnalyticsTracker.trackSafely(event: AnalyticsEvent) {
    runCatching { track(event) }
}

object AnalyticsEvents {
    fun alarmEditorOpened(isNew: Boolean) = AnalyticsEvent(
        "alarm_editor_opened", labels = mapOf("mode" to if (isNew) "create" else "edit")
    )

    fun alarmSaved(alarm: Alarm, isNew: Boolean) = AnalyticsEvent(
        "alarm_saved",
        labels = mapOf(
            "mode" to if (isNew) "create" else "edit",
            "repeat" to alarm.repeat.toString(),
            "difficulty" to alarm.difficulty.toString(),
            "snooze_enabled" to (alarm.snooze > 0).toString(),
        ),
        counts = mapOf(
            "question_count" to alarm.questionCount.toLong(),
            "snooze_minutes" to alarm.snooze.toLong(),
        ),
    )

    fun alarmSaveFailed(reason: String) = AnalyticsEvent(
        "alarm_save_failed", labels = mapOf("reason" to reason)
    )

    fun permissionPrompted(type: String, route: String) = AnalyticsEvent(
        "permission_prompted", labels = mapOf("permission_type" to type, "route" to route)
    )

    fun permissionResult(type: String, result: String) = AnalyticsEvent(
        "permission_result", labels = mapOf("permission_type" to type, "result" to result)
    )

    fun alarmRingingStarted(snoozed: Boolean) = AnalyticsEvent(
        "alarm_ringing_started", labels = mapOf("from_snooze" to snoozed.toString())
    )

    val alarmCompleted = AnalyticsEvent("alarm_completed")
    val alarmSnoozed = AnalyticsEvent("alarm_snoozed")
    val alarmSkipped = AnalyticsEvent("alarm_skipped")
    val alarmPreviewStarted = AnalyticsEvent("alarm_preview_started")

    fun challengeStarted(preview: Boolean, alarm: Alarm) = AnalyticsEvent(
        "challenge_started",
        labels = mapOf("preview" to preview.toString(), "difficulty" to alarm.difficulty.toString()),
        counts = mapOf("question_count" to alarm.questionCount.toLong()),
    )

    fun challengeCompleted(preview: Boolean, durationSeconds: Long, incorrectAnswers: Int) = AnalyticsEvent(
        "challenge_completed",
        labels = mapOf("preview" to preview.toString()),
        counts = mapOf(
            "duration_seconds" to durationSeconds,
            "incorrect_answers" to incorrectAnswers.toLong(),
        ),
    )

    fun reviewAttempted() = AnalyticsEvent("review_request_attempted")
    fun reviewOutcome(result: String) = AnalyticsEvent(
        "review_request_outcome", labels = mapOf("result" to result)
    )

    fun screenViewed(screen: String, layout: String) = AnalyticsEvent(
        "app_screen_viewed", labels = mapOf("screen" to screen, "layout" to layout)
    )
}
