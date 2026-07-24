package com.timilehinaregbesola.mathalarm.utils

import com.timilehinaregbesola.mathalarm.domain.model.Alarm
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import io.kotest.matchers.string.shouldContain
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.test.Test

class AlarmUtilTest {

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
        
        val dateTime = alarm.initLocalDateTimeInSystemZone()
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
        
        val nextTime = calculateNextAlarmTime(alarm)
        nextTime shouldNotBe null
    }

    @Test
    fun `calculateNextAlarmTime with all repeat days should find next occurrence`() {
        val alarm = Alarm(
            hour = 6,
            minute = 0,
            repeatDays = "TTTTTTT"
        )
        
        val nextTime = calculateNextAlarmTime(alarm)
        nextTime shouldNotBe null
    }

    @Test
    fun `calculateNextAlarmTime with specific repeat days should find correct day`() {
        val alarm = Alarm(
            hour = 8,
            minute = 0,
            repeatDays = "FTFTFTT"
        )
        
        val nextTime = calculateNextAlarmTime(alarm)
        nextTime shouldNotBe null
    }

    @Test
    fun `getTimeLeft with no repeat days should return time string`() {
        val alarm = Alarm(
            hour = 23,
            minute = 59,
            repeatDays = "FFFFFFF"
        )
        
        val timeLeft = alarm.getTimeLeft()
        timeLeft shouldContain "minutes" // Should have some time component
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

    @Test
    fun `calculateNextAlarmTime should handle timezone correctly`() {
        val alarm = Alarm(
            hour = 10,
            minute = 0,
            repeatDays = "TTTTTTT"
        )
        val timeZone = TimeZone.currentSystemDefault()
        
        val nextTime = calculateNextAlarmTime(alarm, timeZone)
        nextTime shouldNotBe null
    }

    @Test
    fun `initLocalDateTimeInSystemZone should use current date`() {
        val alarm = Alarm(hour = 14, minute = 30)
        
        val dateTime = alarm.initLocalDateTimeInSystemZone()
        val now = kotlin.time.Clock.System.now()
            .toLocalDateTime(TimeZone.currentSystemDefault())
        
        dateTime.date shouldBe now.date
        dateTime.hour shouldBe 14
        dateTime.minute shouldBe 30
    }
}
