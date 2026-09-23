package com.timilehinaregbesola.mathalarm.presentation.review

import com.russhwolf.settings.MapSettings
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Instant

class ReviewEligibilityStoreTest {
    private val settings = MapSettings()
    private val start = Instant.parse("2026-01-01T10:00:00Z").toEpochMilliseconds()
    private var current = start
    private fun store(zone: TimeZone = TimeZone.UTC) = ReviewEligibilityStore(
        settings, { Instant.fromEpochMilliseconds(current) }, { zone },
    )

    @Test
    fun waitsForSevenDaysAndThreeDistinctCompletionDays() {
        val store = store()
        store.recordFirstUse()
        repeat(3) { store.recordAlarmCompleted() }
        current += 7 * DAY
        assertFalse(store.isEligible(), "Repeated completions on one day count once")
        store.recordAlarmCompleted()
        assertFalse(store.isEligible())
        current += DAY
        store.recordAlarmCompleted()
        assertTrue(store.isEligible())
    }

    @Test
    fun firstUseAndEligibilitySurviveRecreation() {
        val original = store()
        original.recordFirstUse()
        repeat(3) { original.recordAlarmCompleted(); current += DAY }
        current = start + 7 * DAY - 1
        val restored = store()
        restored.recordFirstUse()
        assertFalse(restored.isEligible())
        current++
        assertTrue(restored.isEligible())
    }

    @Test
    fun attemptSurvivesRestartAndRequiresFullNinetyDayCooldown() {
        val store = store()
        store.recordFirstUse()
        repeat(3) { store.recordAlarmCompleted(); current += DAY }
        current = start + 7 * DAY
        store.recordAttempt()
        val attemptedAt = current
        current += DAY
        store.recordAlarmCompleted()
        val restored = store()
        current = attemptedAt + 90 * DAY - 1
        assertFalse(restored.isEligible())
        current++
        assertTrue(restored.isEligible())
    }

    @Test
    fun completionDatesUseLocalCalendarAndRemainDistinctAfterClockMovesBack() {
        val store = store(TimeZone.of("America/Los_Angeles"))
        store.recordFirstUse()
        current = Instant.parse("2026-01-02T01:00:00Z").toEpochMilliseconds()
        store.recordAlarmCompleted() // Jan 1 local
        current = Instant.parse("2026-01-02T07:00:00Z").toEpochMilliseconds()
        store.recordAlarmCompleted() // Still Jan 1 local
        current = Instant.parse("2026-01-02T09:00:00Z").toEpochMilliseconds()
        store.recordAlarmCompleted() // Jan 2 local
        current = start
        store.recordAlarmCompleted() // Jan 1 again
        current = start + 8 * DAY
        assertFalse(store.isEligible())
        store.recordAlarmCompleted()
        assertTrue(store.isEligible())
        assertEquals(current, store.lastCompletionAt())
    }

    @Test
    fun trackingIsInactiveUntilInitializedAndCorruptStateRestartsWaitingPeriod() {
        val store = store()
        repeat(3) { store.recordAlarmCompleted(); current += DAY }
        assertFalse(store.isEligible())
        settings.putString("mathalarm_review_eligibility", "invalid")
        assertFalse(store.isEligible())
        store.recordFirstUse()
        assertFalse(store.isEligible())
    }

    private companion object { const val DAY = 86_400_000L }
}
