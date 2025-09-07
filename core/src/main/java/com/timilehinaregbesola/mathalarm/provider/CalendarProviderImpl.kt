package com.timilehinaregbesola.mathalarm.provider

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.ExperimentalTime


/**
 * Provide the date and time to be used on the alarm use cases, respecting the Inversion of Control.
 */
class DateTimeProviderImpl : DateTimeProvider {

    /**
     * Gets the current [LocalDateTime] in the system default time zone.
     *
     * @return the current [LocalDateTime]
     */
    @OptIn(ExperimentalTime::class)
    override fun getCurrentDateTime(): LocalDateTime =
        Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault())
}
