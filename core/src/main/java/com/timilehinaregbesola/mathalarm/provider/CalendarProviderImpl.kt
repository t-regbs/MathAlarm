package com.timilehinaregbesola.mathalarm.provider

import java.util.Calendar

/**
 * Provide the date and time to be used on the alarm use cases, respecting the Inversion of Control.
 */
class CalendarProviderImpl : CalendarProvider {

    /**
     * Gets the current [Calendar].
     *
     * @return the current [Calendar]
     */
    override fun getCurrentCalendar(): Calendar = Calendar.getInstance()
}
