package com.timilehinaregbesola.mathalarm.provider

import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.test.Test
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class DateTimeProviderTest {

    @OptIn(ExperimentalTime::class)
    @Test
    fun `getCurrentDateTime should return current time`() {
        val provider = DateTimeProviderImpl()
        val beforeCall = Clock.System.now()
        
        val dateTime = provider.getCurrentDateTime()
        dateTime shouldNotBe null
        
        val beforeLocal = beforeCall.toLocalDateTime(TimeZone.currentSystemDefault())

        // Year, month, day should match
        dateTime.year shouldBe beforeLocal.year
        @Suppress("DEPRECATION")
        (dateTime.monthNumber == beforeLocal.monthNumber) shouldBe true
        dateTime.dayOfMonth shouldBe beforeLocal.dayOfMonth
    }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `getCurrentDateTime should use system default timezone`() {
        val provider = DateTimeProviderImpl()
        
        val dateTime = provider.getCurrentDateTime()
        val now = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
        
        dateTime.hour shouldBe now.hour
        dateTime.minute shouldBe now.minute
    }

    @OptIn(ExperimentalTime::class)
    @Test
    fun `multiple calls should return increasing time`() {
        val provider = DateTimeProviderImpl()
        
        val time1 = provider.getCurrentDateTime()
        val time2 = provider.getCurrentDateTime()
        
        @Suppress("DEPRECATION")
        val isSameOrLater = time2.year >= time1.year &&
                           time2.monthNumber >= time1.monthNumber &&
                           time2.dayOfMonth >= time1.dayOfMonth
        isSameOrLater shouldBe true
    }

    @Test
    fun `getCurrentDateTime should return valid LocalDateTime`() {
        val provider = DateTimeProviderImpl()
        
        val dateTime = provider.getCurrentDateTime()

        with(dateTime) {
            (year in 2024..2100) shouldBe true // Reasonable range
            @Suppress("DEPRECATION")
            (monthNumber in 1..12) shouldBe true
            (dayOfMonth in 1..31) shouldBe true
            (hour in 0..23) shouldBe true
            (minute in 0..59) shouldBe true
            (second in 0..59) shouldBe true
        }
    }
}
