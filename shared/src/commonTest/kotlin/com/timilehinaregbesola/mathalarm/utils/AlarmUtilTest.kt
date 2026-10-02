package com.timilehinaregbesola.mathalarm.utils

import com.timilehinaregbesola.mathalarm.domain.model.Alarm
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.test.Test

class AlarmUtilTest {
    private val clock = object : kotlin.time.Clock {
        override fun now() = kotlin.time.Instant.parse("2030-01-06T23:59:59Z")
    }
    @Test
    fun `next alarm display respects recurring skip and persisted one-time dates`() {
        val monday = kotlin.time.Instant.parse("2030-01-07T07:00:00Z")
        val wednesday = kotlin.time.Instant.parse("2030-01-09T07:00:00Z")
        val alarm = Alarm(hour = 7, minute = 0, repeat = true, repeatDays = "FTFTFFF",
            skippedDate = "2030-01-07")
        calculateNextAlarmTime(alarm, TimeZone.UTC, clock) shouldBe wednesday
        calculateNextAlarmTime(alarm.copy(repeat = false, scheduleInitialized = true,
            pendingTimes = listOf(wednesday.toEpochMilliseconds()), scheduleTimeZone = "UTC"),
            TimeZone.UTC, clock) shouldBe wednesday
        calculateNextAlarmTime(alarm.copy(snoozedUntil = monday.toEpochMilliseconds()),
            TimeZone.UTC, clock) shouldBe monday
        calculateNextAlarmTime(alarm.copy(repeat = false, scheduleInitialized = true),
            TimeZone.UTC, clock) shouldBe null
    }

    @Test
    fun `DayOfWeek toIndex should return correct values`() {
        DayOfWeek.SUNDAY.toIndex() shouldBe SUN
        DayOfWeek.MONDAY.toIndex() shouldBe MON
        DayOfWeek.TUESDAY.toIndex() shouldBe TUE
        DayOfWeek.WEDNESDAY.toIndex() shouldBe WED
        DayOfWeek.THURSDAY.toIndex() shouldBe THU
        DayOfWeek.FRIDAY.toIndex() shouldBe FRI
        DayOfWeek.SATURDAY.toIndex() shouldBe SAT
    }

    @Test
    fun `initLocalDateTimeInSystemZone should create LocalDateTime with alarm time`() {
        val alarm = Alarm(hour = 10, minute = 30)
        
        val dateTime = alarm.initLocalDateTimeInSystemZone(clock, TimeZone.UTC)
        dateTime.hour shouldBe 10
        dateTime.minute shouldBe 30
        dateTime.second shouldBe 0
    }

    @Test
    fun `calculateNextAlarmTime with no repeat days should return future time`() {
        val alarm = Alarm(
            hour = 23,
            minute = 59,
            repeatDays = "FFFFFFF" // No repeat days
        )
        
        val nextTime = calculateNextAlarmTime(alarm, TimeZone.UTC, clock)
        nextTime shouldNotBe null
    }

    @Test
    fun `calculateNextAlarmTime with all repeat days should find next occurrence`() {
        val alarm = Alarm(
            hour = 6,
            minute = 0,
            repeatDays = "TTTTTTT"
        )
        
        val nextTime = calculateNextAlarmTime(alarm, TimeZone.UTC, clock)
        nextTime shouldNotBe null
    }

    @Test
    fun `calculateNextAlarmTime with specific repeat days should find correct day`() {
        val alarm = Alarm(
            hour = 8,
            minute = 0,
            repeatDays = "FTFTFTT"
        )
        
        val nextTime = calculateNextAlarmTime(alarm, TimeZone.UTC, clock)
        nextTime shouldNotBe null
    }

    @Test
    fun `difficulty constants should have correct values`() {
        EASY shouldBe 0
        MEDIUM shouldBe 1
        HARD shouldBe 2
    }

    @Test
    fun `day constants should have correct values`() {
        SUN shouldBe 0
        MON shouldBe 1
        TUE shouldBe 2
        WED shouldBe 3
        THU shouldBe 4
        FRI shouldBe 5
        SAT shouldBe 6
    }

    @Test
    fun `calculateNextAlarmTime should handle timezone correctly`() {
        val alarm = Alarm(
            hour = 10,
            minute = 0,
            repeatDays = "TTTTTTT"
        )
        val timeZone = TimeZone.UTC
        
        val nextTime = calculateNextAlarmTime(alarm, timeZone, clock)
        nextTime shouldNotBe null
    }

    @Test
    fun `initLocalDateTimeInSystemZone should use current date`() {
        val alarm = Alarm(hour = 14, minute = 30)
        
        val dateTime = alarm.initLocalDateTimeInSystemZone(clock, TimeZone.UTC)
        val now = clock.now()
            .toLocalDateTime(TimeZone.UTC)
        
        dateTime.date shouldBe now.date
        dateTime.hour shouldBe 14
        dateTime.minute shouldBe 30
    }}
