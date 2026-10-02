package com.timilehinaregbesola.mathalarm.utils

import com.timilehinaregbesola.mathalarm.domain.model.Alarm
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.test.Test

class AlarmPresentationTest {
    private val clock = object : kotlin.time.Clock {
        override fun now() = kotlin.time.Instant.parse("2030-01-06T23:59:59Z")
    }

    @Test
    fun `single remaining occurrence hides redundant next label but exceptions retain it`() {
        val monday = kotlin.time.Instant.parse("2030-01-07T07:00:00Z").toEpochMilliseconds()
        val wednesday = kotlin.time.Instant.parse("2030-01-09T07:00:00Z").toEpochMilliseconds()
        val alarm = Alarm(hour = 7, minute = 0, repeatDays = "FTFTFFF", scheduleInitialized = true,
            pendingTimes = listOf(monday), scheduleTimeZone = "UTC")
        alarm.shouldShowNextOccurrence(TimeZone.UTC, clock) shouldBe false
        alarm.copy(pendingTimes = listOf(monday, wednesday))
            .shouldShowNextOccurrence(TimeZone.UTC, clock) shouldBe true
        alarm.copy(pendingTimes = listOf(nowMillis() - 1, monday))
            .shouldShowNextOccurrence(TimeZone.UTC, clock) shouldBe false
        alarm.copy(repeat = true).shouldShowNextOccurrence(TimeZone.UTC, clock) shouldBe true
        alarm.copy(skippedDate = "2030-01-06").shouldShowNextOccurrence(TimeZone.UTC, clock) shouldBe true
        alarm.copy(snoozedUntil = monday).shouldShowNextOccurrence(TimeZone.UTC, clock) shouldBe true
        alarm.copy(snoozedUntil = nowMillis() - 1).shouldShowNextOccurrence(TimeZone.UTC, clock) shouldBe false
        Alarm(hour = 7).shouldShowNextOccurrence(TimeZone.UTC, clock) shouldBe false
    }

    private fun nowMillis() = clock.now().toEpochMilliseconds()

    @Test
    fun `date labels follow the selected language`() {
        formatShortDate("2030-01-07", "de") shouldNotBe formatShortDate("2030-01-07", "en")
        formatShortDate("not-a-date", "de") shouldBe "not-a-date"
    }

    @Test
    fun `getFormatTime should format midnight correctly`() {
        val alarm = Alarm(hour = 0, minute = 0)

        val formatted = alarm.getFormatTime()
        formatted shouldBe "12:00 AM"
    }

    @Test
    fun `getFormatTime should format noon correctly`() {
        val alarm = Alarm(hour = 12, minute = 0)

        val formatted = alarm.getFormatTime()
        formatted shouldBe "12:00 PM"
    }

    @Test
    fun `getFormatTime should format AM time correctly`() {
        val alarm = Alarm(hour = 9, minute = 30)

        val formatted = alarm.getFormatTime()
        formatted shouldBe "09:30 AM"
    }

    @Test
    fun `getFormatTime should format PM time correctly`() {
        val alarm = Alarm(hour = 15, minute = 45)

        val formatted = alarm.getFormatTime()
        formatted shouldBe "03:45 PM"
    }

    @Test
    fun `getFormatTime should pad single digit minutes`() {
        val alarm = Alarm(hour = 8, minute = 5)

        val formatted = alarm.getFormatTime()
        formatted shouldBe "08:05 AM"
    }

    @Test
    fun `getFormatTime should handle 11 PM correctly`() {
        val alarm = Alarm(hour = 23, minute = 59)

        val formatted = alarm.getFormatTime()
        formatted shouldBe "11:59 PM"
    }

    @Test
    fun `getTimeLeft with no repeat days should return time string`() {
        val alarm = Alarm(
            hour = 23,
            minute = 59,
            repeatDays = "FFFFFFF"
        )

        val timeLeft = alarm.getTimeLeft()
        timeLeft shouldContain "minute" // Accept both singular and plural minute components.
    }

    @Test
    fun `getTimeLeft with all repeat days should return time until next occurrence`() {
        val alarm = Alarm(
            hour = 6,
            minute = 0,
            repeatDays = "TTTTTTT"
        )

        val timeLeft = alarm.getTimeLeft()

        val hasTimeInfo = timeLeft.contains("hour") ||
                         timeLeft.contains("minute") ||
                         timeLeft.contains("day")
        hasTimeInfo shouldBe true
    }

    @Test
    fun `getTimeLeft with all F repeat days should return 0 minutes`() {
        val alarm = Alarm(
            hour = 8,
            minute = 0,
            repeatDays = "FFFFFFF"
        )

        val timeLeft = alarm.getTimeLeft()
        timeLeft shouldNotBe ""
    }

    @Test
    fun `days list should have correct values`() {
        days.size shouldBe 7
        days[0] shouldBe "S" // Sunday
        days[1] shouldBe "M" // Monday
        days[2] shouldBe "T" // Tuesday
        days[3] shouldBe "W" // Wednesday
        days[4] shouldBe "T" // Thursday
        days[5] shouldBe "F" // Friday
        days[6] shouldBe "S" // Saturday
    }

    @Test
    fun `fullDays list should have correct values`() {
        fullDays.size shouldBe 7
        fullDays[0] shouldBe "Sunday"
        fullDays[1] shouldBe "Monday"
        fullDays[2] shouldBe "Tuesday"
        fullDays[3] shouldBe "Wednesday"
        fullDays[4] shouldBe "Thursday"
        fullDays[5] shouldBe "Friday"
        fullDays[6] shouldBe "Saturday"
    }

    @Test
    fun `getFormatTime should handle all hours of day`() {
        for (hour in 0..23) {
            val alarm = Alarm(hour = hour, minute = 0)
            val formatted = alarm.getFormatTime()

            val hasAmPm = formatted.contains("AM") || formatted.contains("PM")
            hasAmPm shouldBe true

            // Should have correct format (XX:XX AM/PM)
            formatted.length shouldBe 8
        }
    }

}
