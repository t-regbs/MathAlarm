package com.timilehinaregbesola.mathalarm.presentation.review

import com.russhwolf.settings.Settings
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import kotlin.time.Clock
import kotlin.time.Instant

/** Local review eligibility. A request attempt never implies that a review was submitted. */
class ReviewEligibilityStore(
    private val settings: Settings,
    private val now: () -> Instant = { Clock.System.now() },
    private val timeZone: () -> TimeZone = { TimeZone.currentSystemDefault() },
) {
    @Serializable
    private data class State(
        val firstUseAt: Long,
        val completedDates: Set<String> = emptySet(),
        val lastCompletionAt: Long? = null,
    )

    fun recordFirstUse() {
        if (read() == null) write(State(firstUseAt = now().toEpochMilliseconds()))
    }

    fun recordAlarmCompleted() {
        // Only platforms that enable review tracking initialize this store.
        val state = read() ?: return
        val instant = now()
        val date = instant.toLocalDateTime(timeZone()).date.toString()
        write(state.copy(
            // Three distinct dates are sufficient; don't retain a full usage history.
            completedDates = if (state.completedDates.size < REQUIRED_DAYS) state.completedDates + date
                else state.completedDates,
            lastCompletionAt = instant.toEpochMilliseconds(),
        ))
    }

    fun isEligible(): Boolean {
        val state = read() ?: return false
        val current = now().toEpochMilliseconds()
        val lastAttempt = settings.getLongOrNull(ATTEMPT_KEY)
        return current - state.firstUseAt >= FIRST_USE_DELAY &&
            state.completedDates.size >= REQUIRED_DAYS &&
            (lastAttempt == null || current - lastAttempt >= ATTEMPT_COOLDOWN)
    }

    fun lastCompletionAt(): Long? = read()?.lastCompletionAt

    fun recordAttempt() {
        // A receiver can record a completion concurrently. Keep the attempt in its own key
        // so neither write can overwrite the other; completion writes use the command mutex.
        settings.putLong(ATTEMPT_KEY, now().toEpochMilliseconds())
    }

    private fun read(): State? = settings.getStringOrNull(KEY)?.let {
        try { Json.decodeFromString<State>(it) } catch (_: IllegalArgumentException) { null }
    }

    private fun write(state: State) = settings.putString(KEY, Json.encodeToString(state))

    private companion object {
        const val KEY = "mathalarm_review_eligibility"
        const val ATTEMPT_KEY = "mathalarm_review_last_attempt"
        const val REQUIRED_DAYS = 3
        const val DAY = 24 * 60 * 60 * 1000L
        const val FIRST_USE_DELAY = 7 * DAY
        const val ATTEMPT_COOLDOWN = 90 * DAY
    }
}
