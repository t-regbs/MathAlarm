package com.timilehinaregbesola.mathalarm.provider

import kotlinx.datetime.LocalDateTime

/**
 * Provide the date and time to be used on the alarm use cases, respecting the Inversion of Control.
 */
interface DateTimeProvider {

    /**
     * Gets the current [LocalDateTime] in the system default time zone.
     *
     * @return the current [LocalDateTime]
     */
    fun getCurrentDateTime(): LocalDateTime
}
