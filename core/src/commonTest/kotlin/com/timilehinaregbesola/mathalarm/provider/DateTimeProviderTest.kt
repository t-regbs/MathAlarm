package com.timilehinaregbesola.mathalarm.provider

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.time.Clock
import kotlin.time.Instant

class DateTimeProviderTest {
    private var instant = Instant.parse("2030-12-31T23:59:59Z")
    private val clock = object : Clock { override fun now() = instant }

    @Test fun dateRollsOverAtMidnight() {
        val provider = DateTimeProviderImpl(clock) { TimeZone.UTC }
        assertEquals(LocalDateTime(2030, 12, 31, 23, 59, 59), provider.getCurrentDateTime())
        instant = Instant.parse("2031-01-01T00:00:00Z")
        assertEquals(LocalDateTime(2031, 1, 1, 0, 0), provider.getCurrentDateTime())
    }

    @Test fun readsTheCurrentZoneOnEveryCall() {
        var zone: TimeZone = TimeZone.UTC
        val provider = DateTimeProviderImpl(clock) { zone }
        assertEquals(LocalDateTime(2030, 12, 31, 23, 59, 59), provider.getCurrentDateTime())
        zone = TimeZone.of("Asia/Tokyo")
        assertEquals(LocalDateTime(2031, 1, 1, 8, 59, 59), provider.getCurrentDateTime())
    }
}
