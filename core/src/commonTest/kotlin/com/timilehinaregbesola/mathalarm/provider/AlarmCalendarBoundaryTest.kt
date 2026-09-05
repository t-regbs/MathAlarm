package com.timilehinaregbesola.mathalarm.provider

import com.timilehinaregbesola.mathalarm.domain.model.Alarm
import com.timilehinaregbesola.mathalarm.fake.DateTimeProviderFake
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlin.test.Test
import kotlin.test.assertEquals

class AlarmCalendarBoundaryTest {
    private fun next(now: LocalDateTime, zone: String, hour: Int, minute: Int): Long {
        val clock = DateTimeProviderFake().apply { setFixedDateTime(now) }
        return AlarmTimeCalculatorImpl(clock) { TimeZone.of(zone) }
            .calculateNextAlarmTime(Alarm(hour = hour, minute = minute, repeatDays = "TTTTTTT"))!!
    }

    @Test fun dailyAlarmKeepsLocalTimeAcrossSpringDst() {
        val zone = TimeZone.of("Europe/London")
        val now = LocalDateTime(2030, 3, 30, 8, 0)
        val expected = LocalDateTime(2030, 3, 31, 7, 0).toInstant(zone).toEpochMilliseconds()
        assertEquals(expected, next(now, zone.id, 7, 0))
    }

    @Test fun dailyAlarmKeepsLocalTimeAcrossAutumnDst() {
        val zone = TimeZone.of("Europe/London")
        val now = LocalDateTime(2030, 10, 26, 8, 0)
        val expected = LocalDateTime(2030, 10, 27, 7, 0).toInstant(zone).toEpochMilliseconds()
        assertEquals(expected, next(now, zone.id, 7, 0))
    }

    @Test fun midnightCrossesYearBoundary() {
        val expected = LocalDateTime(2031, 1, 1, 0, 0).toInstant(TimeZone.UTC).toEpochMilliseconds()
        assertEquals(expected, next(LocalDateTime(2030, 12, 31, 23, 59), "UTC", 0, 0))
    }

    @Test fun everyWeekdayMaskProducesExactlyItsSelectedDays() {
        val clock = DateTimeProviderFake().apply { setFixedDateTime(2030, 1, 6, 6, 0) }
        val calculator = AlarmTimeCalculatorImpl(clock) { TimeZone.UTC }
        for (mask in 1..127) {
            val days = (0..6).map { if (mask and (1 shl it) != 0) 'T' else 'F' }.joinToString("")
            val times = calculator.calculateAlarmTimes(Alarm(hour = 7, minute = 0, repeatDays = days))
            val expected = (0..6).filter { mask and (1 shl it) != 0 }.map {
                LocalDateTime(2030, 1, 6 + it, 7, 0).toInstant(TimeZone.UTC).toEpochMilliseconds()
            }
            assertEquals(expected, times, "weekday mask $days")
        }
    }
}
