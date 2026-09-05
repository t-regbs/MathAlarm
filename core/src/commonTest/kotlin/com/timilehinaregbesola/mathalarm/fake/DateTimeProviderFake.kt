package com.timilehinaregbesola.mathalarm.fake

import com.timilehinaregbesola.mathalarm.provider.DateTimeProvider
import kotlinx.datetime.LocalDateTime

class DateTimeProviderFake : DateTimeProvider {
    private val baseline = LocalDateTime(2030, 1, 6, 6, 0)
    private var fixedDateTime = baseline

    override fun getCurrentDateTime(): LocalDateTime = fixedDateTime

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
     * Reset to the deterministic Sunday baseline.
     */
    fun clearFixedDateTime() {
        fixedDateTime = baseline
    }
}
