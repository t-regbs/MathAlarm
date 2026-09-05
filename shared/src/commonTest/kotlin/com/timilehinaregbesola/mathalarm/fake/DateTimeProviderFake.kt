package com.timilehinaregbesola.mathalarm.fake

import com.timilehinaregbesola.mathalarm.provider.DateTimeProvider
import kotlinx.datetime.LocalDateTime

class DateTimeProviderFake : DateTimeProvider {
    private val baseline = LocalDateTime(2030, 1, 6, 6, 0)
    private var fixedDateTime = baseline

    override fun getCurrentDateTime(): LocalDateTime = fixedDateTime

    fun setFixedDateTime(dateTime: LocalDateTime) {
        fixedDateTime = dateTime
    }

    fun clearFixedDateTime() {
        fixedDateTime = baseline
    }
}
