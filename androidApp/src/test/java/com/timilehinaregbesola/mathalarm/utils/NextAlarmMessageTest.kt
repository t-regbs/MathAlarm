package com.timilehinaregbesola.mathalarm.utils

import com.timilehinaregbesola.mathalarm.utils.strings.*
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue
import kotlin.time.Duration
import kotlin.time.Duration.Companion.hours
import kotlin.time.Duration.Companion.minutes
import kotlin.time.Duration.Companion.seconds
import kotlin.time.Instant

class NextAlarmMessageTest {
    private val now = Instant.parse("2030-01-06T23:30:00Z")
    private fun message(delay: Duration) = nextAlarmMessage(now + delay, now, TimeZone.UTC, EnMathAlarmStrings)

    @Test fun countdownHandlesMinuteHourAndDayBoundaries() {
        assertEquals("Next alarm in less than a minute", message(59.seconds))
        assertEquals("Next alarm in 1 minute", message(1.minutes))
        assertEquals("Next alarm in 5 minutes", message(5.minutes))
        assertEquals("Next alarm in 59 minutes", message(1.hours - 1.seconds))
        assertEquals("Next alarm in 1 hour", message(1.hours))
        assertEquals("Next alarm in 1 hour 1 minute", message(1.hours + 1.minutes))
        assertEquals("Next alarm in 3 hours 20 minutes", message(3.hours + 20.minutes))
        assertEquals("Next alarm in 24 hours", message(24.hours))
        val next = now + 24.hours + 1.seconds
        assertEquals("Next alarm: ${formatShortDate("2030-01-07", "en")} · 23:30",
            nextAlarmMessage(next, now, TimeZone.UTC, EnMathAlarmStrings))
    }

    @Test fun advancesFromDateToCountdownWithoutChangingTheScheduledInstant() {
        val next = now + 25.hours
        assertTrue(nextAlarmMessage(next, now, TimeZone.UTC, EnMathAlarmStrings).startsWith("Next alarm:"))
        assertEquals("Next alarm in 24 hours",
            nextAlarmMessage(next, now + 1.hours, TimeZone.UTC, EnMathAlarmStrings))
        assertEquals("Next alarm in 5 minutes",
            nextAlarmMessage(next, next - 5.minutes, TimeZone.UTC, EnMathAlarmStrings))
        assertEquals(EnMathAlarmStrings.noUpcomingAlarms,
            nextAlarmMessage(next, next, TimeZone.UTC, EnMathAlarmStrings))
        assertEquals(EnMathAlarmStrings.noUpcomingAlarms,
            nextAlarmMessage(null, now, TimeZone.UTC, EnMathAlarmStrings))
    }

    @Test fun thresholdUsesElapsedHoursAcrossDaylightSavingChange() {
        val beforeJump = Instant.parse("2030-03-31T00:30:00Z")
        assertEquals("Next alarm in 24 hours", nextAlarmMessage(
            beforeJump + 24.hours, beforeJump, TimeZone.of("Europe/London"), EnMathAlarmStrings))
    }

    @Test fun countdownSentencesAreLocalizedIncludingPluralForms() {
        assertEquals("Nächster Alarm in 1 Stunde 5 Minuten", DeMathAlarmStrings.nextAlarmIn(1, 5))
        assertEquals("Следующий будильник через 2 часа 21 минуту", RuMathAlarmStrings.nextAlarmIn(2, 21))
        assertEquals("Следующий будильник через 11 часов 12 минут", RuMathAlarmStrings.nextAlarmIn(11, 12))
        assertEquals("下一个闹钟将在3小时20分钟后响起", ZhMathAlarmStrings.nextAlarmIn(3, 20))
        listOf(DeMathAlarmStrings, EsMathAlarmStrings, PtMathAlarmStrings, RuMathAlarmStrings,
            HiMathAlarmStrings, BnMathAlarmStrings, PaMathAlarmStrings, ZhMathAlarmStrings).forEach {
            assertTrue(it.nextAlarmIn(0, 0).isNotBlank())
            assertFalse(it.nextAlarmIn(0, 5).contains("Next alarm"))
            assertTrue(it.nextAlarmIn(0, 5).contains("5"))
        }
    }
}
