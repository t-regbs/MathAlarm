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

    fun setFixedDateTime(dateTime: LocalDateTime) {
        fixedDateTime = dateTime
    }

    fun clearFixedDateTime() {
        fixedDateTime = null
    }
}
