package com.timilehinaregbesola.mathalarm.utils

import com.timilehinaregbesola.mathalarm.domain.model.Alarm
import com.timilehinaregbesola.mathalarm.provider.remainingOccurrences
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

val days = listOf("S", "M", "T", "W", "T", "F", "S")
val fullDays = listOf(
    "Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"
)

fun formatShortDate(value: String, languageTag: String = "en"): String =
    com.timilehinaregbesola.mathalarm.platform.formatAlarmDate(value, languageTag)

/**
 * Returns a 12-hour "hh:mm AM/PM" string for this Alarm's hour/minute.
 */
fun Alarm.getFormatTime(): String {
    val isAm = hour < 12
    val hour12 = when {
        hour == 0 -> 12
        hour == 12 -> 12
        hour > 12 -> hour - 12
        else -> hour
    }
    val hourPadded = hour12.toString().padStart(2, '0')
    val minutePadded = minute.toString().padStart(2, '0')
    val amPm = if (isAm) "AM" else "PM"
    return "$hourPadded:$minutePadded $amPm"
}

/** Human-readable interval used by the alarm-set confirmation. */
@OptIn(ExperimentalTime::class)
fun Alarm.getTimeLeft(): String {
    val nowInstant = Clock.System.now()
    val chosenInstant = calculateNextAlarmTime(this)

    // If no future day matched (repeatDays all 'F'), show "0 minutes"
    if (chosenInstant == null) {
        return "0 minutes"
    }

    // Compute the duration between "now" and "chosenInstant" in seconds:
    val totalSeconds = (chosenInstant - nowInstant).inWholeSeconds
    val daysPart = (totalSeconds / (60 * 60 * 24)).toInt()
    val hoursPart = ((totalSeconds % (60 * 60 * 24)) / (60 * 60)).toInt()
    val minutesPart = ((totalSeconds % (60 * 60)) / 60).toInt()

    val dString = if (daysPart == 1) "day" else "days"
    val hString = if (hoursPart == 1) "hour" else "hours"
    val mString = if (minutesPart == 1) "minute" else "minutes"

    return when {
        daysPart > 0 -> "$daysPart $dString $hoursPart $hString $minutesPart $mString"
        hoursPart > 0 -> "$hoursPart $hString $minutesPart $mString"
        else -> "$minutesPart $mString"
    }
}

/** Show extra schedule context only when the clock and selected day are insufficient. */
fun Alarm.shouldShowNextOccurrence(
    timeZone: TimeZone = TimeZone.currentSystemDefault(),
    clock: Clock = Clock.System,
): Boolean {
    if (repeat || skippedDate != null || snoozedUntil?.let { it > clock.now().toEpochMilliseconds() } == true) {
        return true
    }
    return remainingOccurrences(presentationOccurrenceCalculator(timeZone, clock), timeZone).size > 1
}

private fun presentationOccurrenceCalculator(timeZone: TimeZone, clock: Clock) =
    com.timilehinaregbesola.mathalarm.provider.AlarmTimeCalculatorImpl(
        object : com.timilehinaregbesola.mathalarm.provider.DateTimeProvider {
            override fun getCurrentDateTime() = clock.now().toLocalDateTime(timeZone)
        }
    ) { timeZone }
