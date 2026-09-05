package com.timilehinaregbesola.mathalarm.fake

import com.timilehinaregbesola.mathalarm.provider.DateTimeProvider
import kotlinx.datetime.LocalDateTime
import kotlin.time.ExperimentalTime

class DateTimeProviderFake : DateTimeProvider {
    private var fixedDateTime: LocalDateTime = LocalDateTime(2030, 1, 6, 6, 0)

    @OptIn(ExperimentalTime::class)
    override fun getCurrentDateTime(): LocalDateTime {
        return fixedDateTime
    }

    fun setFixedDateTime(dateTime: LocalDateTime) {
        fixedDateTime = dateTime
    }

    fun clearFixedDateTime() {
        fixedDateTime = LocalDateTime(2030, 1, 6, 6, 0)
    }
}
