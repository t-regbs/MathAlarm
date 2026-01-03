package com.timilehinaregbesola.mathalarm.provider

import com.timilehinaregbesola.mathalarm.domain.model.Alarm
import com.timilehinaregbesola.mathalarm.fake.DateTimeProviderFake
import io.kotest.matchers.collections.shouldHaveSize
import io.kotest.matchers.shouldBe
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.Instant
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.test.BeforeTest
import kotlin.test.Test

class AlarmTimeCalculatorImplTest {

    private lateinit var dateTimeProvider: DateTimeProviderFake
    private lateinit var calculator: AlarmTimeCalculatorImpl

    @BeforeTest
    fun setup() {
        dateTimeProvider = DateTimeProviderFake()
        calculator = AlarmTimeCalculatorImpl(dateTimeProvider)
    }

    @Test
    fun `non-repeating alarm in the future today should be scheduled for today`() {
        // Wednesday, January 8, 2025 at 9:00 AM
        dateTimeProvider.setFixedDateTime(2025, 1, 8, 9, 0)
        
        // Alarm set for 10:00 AM (1 hour in future)
        val alarm = Alarm(hour = 10, minute = 0, repeatDays = "FFFFFFF")
        
        val times = calculator.calculateAlarmTimes(alarm)
        
        times shouldHaveSize 1
        val alarmDateTime = instantToLocalDateTime(times.first())
        alarmDateTime.dayOfMonth shouldBe 8
        alarmDateTime.hour shouldBe 10
        alarmDateTime.minute shouldBe 0
    }

    @Test
    fun `non-repeating alarm in the past today should be scheduled for tomorrow`() {
        // Wednesday, January 8, 2025 at 11:00 AM
        dateTimeProvider.setFixedDateTime(2025, 1, 8, 11, 0)
        
        // Alarm set for 10:00 AM (already passed)
        val alarm = Alarm(hour = 10, minute = 0, repeatDays = "FFFFFFF")
        
        val times = calculator.calculateAlarmTimes(alarm)
        
        times shouldHaveSize 1
        val alarmDateTime = instantToLocalDateTime(times.first())
        alarmDateTime.dayOfMonth shouldBe 9 // Tomorrow
        alarmDateTime.hour shouldBe 10
        alarmDateTime.minute shouldBe 0
    }

    @Test
    fun `non-repeating alarm at midnight should handle day rollover`() {
        // Saturday at 11:30 PM
        dateTimeProvider.setFixedDateTime(2025, 1, 11, 23, 30)
        
        // Alarm set for midnight (00:00) - should be tomorrow
        val alarm = Alarm(hour = 0, minute = 0, repeatDays = "FFFFFFF")
        
        val times = calculator.calculateAlarmTimes(alarm)
        
        times shouldHaveSize 1
        val alarmDateTime = instantToLocalDateTime(times.first())
        alarmDateTime.dayOfMonth shouldBe 12 // Sunday
        alarmDateTime.hour shouldBe 0
        alarmDateTime.minute shouldBe 0
    }

    @Test
    fun `repeating alarm for weekdays only should return 5 times`() {
        // Sunday, January 5, 2025 at 8:00 AM
        dateTimeProvider.setFixedDateTime(2025, 1, 5, 8, 0)
        
        // Alarm for weekdays: Mon-Fri (indices 1-5)
        // repeatDays format: "FTTTTFF" (Sun=F, Mon=T, Tue=T, Wed=T, Thu=T, Fri=F, Sat=F)
        // Wait, let me check the format - indices are SUN=0, MON=1, etc.
        // So weekdays Mon-Fri = indices 1,2,3,4,5 = "FTTTTTF"
        val alarm = Alarm(hour = 7, minute = 0, repeat = true, repeatDays = "FTTTTTF")
        
        val times = calculator.calculateAlarmTimes(alarm)
        
        times shouldHaveSize 5
    }

    @Test
    fun `repeating alarm for weekends only should return 2 times`() {
        // Monday, January 6, 2025 at 8:00 AM
        dateTimeProvider.setFixedDateTime(2025, 1, 6, 8, 0)
        
        // Alarm for weekends: Sat and Sun (indices 0 and 6)
        // repeatDays = "TFFFFFT"
        val alarm = Alarm(hour = 9, minute = 0, repeat = true, repeatDays = "TFFFFFT")
        
        val times = calculator.calculateAlarmTimes(alarm)
        
        times shouldHaveSize 2
    }

    @Test
    fun `repeating alarm for all days should return 7 times`() {
        // Monday, January 6, 2025 at 8:00 AM
        dateTimeProvider.setFixedDateTime(2025, 1, 6, 8, 0)
        
        val alarm = Alarm(hour = 6, minute = 30, repeat = true, repeatDays = "TTTTTTT")
        
        val times = calculator.calculateAlarmTimes(alarm)
        
        times shouldHaveSize 7
    }

    @Test
    fun `repeating alarm for single day in future this week`() {
        // Monday, January 6, 2025 at 8:00 AM
        dateTimeProvider.setFixedDateTime(2025, 1, 6, 8, 0)
        
        // Alarm only on Wednesday (index 3)
        val alarm = Alarm(hour = 7, minute = 0, repeat = true, repeatDays = "FFFTFFF")
        
        val times = calculator.calculateAlarmTimes(alarm)
        
        times shouldHaveSize 1
        val alarmDateTime = instantToLocalDateTime(times.first())
        alarmDateTime.dayOfWeek shouldBe DayOfWeek.WEDNESDAY
        alarmDateTime.dayOfMonth shouldBe 8 // Wednesday Jan 8
    }

    @Test
    fun `repeating alarm for single day already passed this week should schedule next week`() {
        // Wednesday, January 8, 2025 at 8:00 AM
        dateTimeProvider.setFixedDateTime(2025, 1, 8, 8, 0)
        
        // Alarm only on Monday (index 1) - already passed this week
        val alarm = Alarm(hour = 7, minute = 0, repeat = true, repeatDays = "FTFFFFF")
        
        val times = calculator.calculateAlarmTimes(alarm)
        
        times shouldHaveSize 1
        val alarmDateTime = instantToLocalDateTime(times.first())
        alarmDateTime.dayOfWeek shouldBe DayOfWeek.MONDAY
        alarmDateTime.dayOfMonth shouldBe 13 // Next Monday
    }

    @Test
    fun `repeating alarm for today but time already passed should schedule next week`() {
        // Wednesday, January 8, 2025 at 10:00 AM
        dateTimeProvider.setFixedDateTime(2025, 1, 8, 10, 0)
        
        // Alarm only on Wednesday (index 3) at 7:00 AM - already passed
        val alarm = Alarm(hour = 7, minute = 0, repeat = true, repeatDays = "FFFTFFF")
        
        val times = calculator.calculateAlarmTimes(alarm)
        
        times shouldHaveSize 1
        val alarmDateTime = instantToLocalDateTime(times.first())
        alarmDateTime.dayOfWeek shouldBe DayOfWeek.WEDNESDAY
        alarmDateTime.dayOfMonth shouldBe 15 // Next Wednesday
    }

    @Test
    fun `repeating alarm for today in the future should schedule today`() {
        // Wednesday, January 8, 2025 at 6:00 AM
        dateTimeProvider.setFixedDateTime(2025, 1, 8, 6, 0)
        
        // Alarm only on Wednesday (index 3) at 7:00 AM - in the future
        val alarm = Alarm(hour = 7, minute = 0, repeat = true, repeatDays = "FFFTFFF")
        
        val times = calculator.calculateAlarmTimes(alarm)
        
        times shouldHaveSize 1
        val alarmDateTime = instantToLocalDateTime(times.first())
        alarmDateTime.dayOfWeek shouldBe DayOfWeek.WEDNESDAY
        alarmDateTime.day shouldBe 8 // Today
    }

    @Test
    fun `saturday to sunday rollover for non-repeating alarm`() {
        // Saturday at 11:00 PM
        dateTimeProvider.setFixedDateTime(2025, 1, 11, 23, 0)
        
        // Alarm set for 8:00 AM (already passed today, so tomorrow)
        val alarm = Alarm(hour = 8, minute = 0, repeatDays = "FFFFFFF")
        
        val times = calculator.calculateAlarmTimes(alarm)
        
        times shouldHaveSize 1
        val alarmDateTime = instantToLocalDateTime(times.first())
        alarmDateTime.dayOfWeek shouldBe DayOfWeek.SUNDAY
        alarmDateTime.day shouldBe 12
    }

    @Test
    fun `calculateNextAlarmTime returns soonest time`() {
        // Monday, January 6, 2025 at 8:00 AM
        dateTimeProvider.setFixedDateTime(2025, 1, 6, 8, 0)
        
        // Alarm for Tuesday and Thursday
        val alarm = Alarm(hour = 7, minute = 0, repeat = true, repeatDays = "FFTFTFF")
        
        val nextTime = calculator.calculateNextAlarmTime(alarm)
        
        // Should return Tuesday (soonest)
        val nextDateTime = instantToLocalDateTime(nextTime!!)
        nextDateTime.dayOfWeek shouldBe DayOfWeek.TUESDAY
    }

    @Test
    fun `isInFuture returns true for future time`() {
        dateTimeProvider.setFixedDateTime(2025, 1, 8, 10, 0)
        
        val futureTime = LocalDateTime(2025, 1, 8, 11, 0)
            .toInstant(TimeZone.currentSystemDefault())
            .toEpochMilliseconds()
        
        calculator.isInFuture(futureTime) shouldBe true
    }

    @Test
    fun `isInFuture returns false for past time`() {
        dateTimeProvider.setFixedDateTime(2025, 1, 8, 10, 0)
        
        val pastTime = LocalDateTime(2025, 1, 8, 9, 0)
            .toInstant(TimeZone.currentSystemDefault())
            .toEpochMilliseconds()
        
        calculator.isInFuture(pastTime) shouldBe false
    }

    @Test
    fun `alarm at same minute should be treated as passed`() {
        // Wednesday at 7:00:30 AM
        dateTimeProvider.setFixedDateTime(2025, 1, 8, 7, 0, 30)
        
        // Alarm at 7:00 AM - same minute but seconds past
        val alarm = Alarm(hour = 7, minute = 0, repeatDays = "FFFFFFF")
        
        val times = calculator.calculateAlarmTimes(alarm)
        
        // Should schedule for tomorrow since the time has effectively passed
        val alarmDateTime = instantToLocalDateTime(times.first())
        alarmDateTime.day shouldBe 9
    }

    @Test
    fun `alarm crossing month boundary`() {
        // January 31st at 10:00 PM
        dateTimeProvider.setFixedDateTime(2025, 1, 31, 22, 0)
        
        // Alarm at 8:00 AM (already passed, should be February 1st)
        val alarm = Alarm(hour = 8, minute = 0, repeatDays = "FFFFFFF")
        
        val times = calculator.calculateAlarmTimes(alarm)
        
        val alarmDateTime = instantToLocalDateTime(times.first())
        alarmDateTime.monthNumber shouldBe 2
        alarmDateTime.dayOfMonth shouldBe 1
    }

    @Test
    fun `alarm crossing year boundary`() {
        // December 31st at 10:00 PM
        dateTimeProvider.setFixedDateTime(2024, 12, 31, 22, 0)
        
        // Alarm at 8:00 AM (already passed, should be January 1st next year)
        val alarm = Alarm(hour = 8, minute = 0, repeatDays = "FFFFFFF")
        
        val times = calculator.calculateAlarmTimes(alarm)
        
        val alarmDateTime = instantToLocalDateTime(times.first())
        alarmDateTime.year shouldBe 2025
        alarmDateTime.monthNumber shouldBe 1
        alarmDateTime.dayOfMonth shouldBe 1
    }

    private fun instantToLocalDateTime(epochMillis: Long): LocalDateTime {
        return Instant.fromEpochMilliseconds(epochMillis)
            .toLocalDateTime(TimeZone.currentSystemDefault())
    }
}
