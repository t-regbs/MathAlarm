package com.timilehinaregbesola.mathalarm.fake

import com.timilehinaregbesola.mathalarm.provider.DateTimeProvider
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class DateTimeProviderFake : DateTimeProvider {
    private var fixedDateTime: LocalDateTime? = null

    @OptIn(ExperimentalTime::class)
    override fun getCurrentDateTime(): LocalDateTime {
        return fixedDateTime ?: Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
    }

    /**
     * Set a fixed date/time for deterministic testing.
     */
    fun setFixedDateTime(dateTime: LocalDateTime) {
        fixedDateTime = dateTime
    }

    /**
     * Set a fixed date/time using individual components.
     */
    fun setFixedDateTime(
        year: Int,
        month: Int,
        dayOfMonth: Int,
        hour: Int,
        minute: Int,
        second: Int = 0
    ) {
        fixedDateTime = LocalDateTime(year, month, dayOfMonth, hour, minute, second)
    }

    /**
     * Clear the fixed time and return to using the real clock.
     */
    fun clearFixedDateTime() {
        fixedDateTime = null
    }
}
