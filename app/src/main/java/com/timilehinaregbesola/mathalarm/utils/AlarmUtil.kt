package com.timilehinaregbesola.mathalarm.utils

import com.timilehinaregbesola.mathalarm.domain.model.Alarm
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

const val SUN = 0
const val MON = 1
const val TUE = 2
const val WED = 3
const val THU = 4
const val FRI = 5
const val SAT = 6
const val EASY = 0
const val MEDIUM = 1
const val HARD = 2

val days = listOf("S", "M", "T", "W", "T", "F", "S")
val fullDays = listOf(
    "Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday"
)

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
    val minutePadded = minute.toString().padStart(2, '0')
    val amPm = if (isAm) "AM" else "PM"
    return "%02d:%s %s".format(hour12, minutePadded, amPm)
}

/**
 * Extension function for [DayOfWeek] to get its corresponding index (0-6).
 */
fun DayOfWeek.toIndex(): Int = when (this) {
    DayOfWeek.SUNDAY -> SUN
    DayOfWeek.MONDAY -> MON
    DayOfWeek.TUESDAY -> TUE
    DayOfWeek.WEDNESDAY -> WED
    DayOfWeek.THURSDAY -> THU
    DayOfWeek.FRIDAY -> FRI
    DayOfWeek.SATURDAY -> SAT
}

/**
 * Like getTodayDateTimeInSystemZone(), but enforces second=0.
 */
@OptIn(ExperimentalTime::class)
fun Alarm.initLocalDateTimeInSystemZone(): LocalDateTime {
    val nowInstant = Clock.System.now()
    val tz = TimeZone.currentSystemDefault()
    val today = nowInstant.toLocalDateTime(tz).date
    return LocalDateTime(
        date = today,
        time = LocalTime(hour, minute, 0)
    )
}

/**
 * Calculate the next time an alarm will go off.
 *
 * @param alarm The alarm to calculate the next time for
 * @param timeZone The timezone to use for the calculation
 * @return The next time the alarm will go off as an Instant, or null if the alarm has no repeat days set
 */
@OptIn(ExperimentalTime::class)
fun calculateNextAlarmTime(alarm: Alarm, timeZone: TimeZone = TimeZone.currentSystemDefault()): Instant? {
    val nowInstant = Clock.System.now()
    val localNow = nowInstant.toLocalDateTime(timeZone)
    val todayDate = localNow.date

    // Check if the alarm has any repeat days set
    val hasRepeatDays = alarm.repeatDays.contains('T')

    if (!hasRepeatDays) {
        // If alarm doesn't have any repeat days set, just check today
        val alarmDateTime = LocalDateTime(
            date = todayDate,
            time = LocalTime(alarm.hour, alarm.minute, 0)
        )
        var candidateInstant = alarmDateTime.toInstant(timeZone)

        // If the alarm time is in the past, add one week (7 days)
        if (candidateInstant < nowInstant) {
            val oneWeek = DatePeriod(days = 7)
            candidateInstant = candidateInstant.plus(oneWeek, timeZone)
        }

        return candidateInstant
    } else {
        // For alarms with repeat days, find the next occurrence based on repeat days

        // Get the current day of week (0..6)
        val currentDayIndex = todayDate.dayOfWeek.toIndex()

        // Check if the alarm is set for the current day
        val isSetForToday = alarm.repeatDays.getOrNull(currentDayIndex) == 'T'

        // Create a LocalDateTime for the alarm time today
        val alarmTimeToday = LocalDateTime(
            date = todayDate,
            time = LocalTime(alarm.hour, alarm.minute, 0)
        )
        val alarmInstantToday = alarmTimeToday.toInstant(timeZone)

        // If the alarm is set for today and the time hasn't passed yet, use today's time
        if (isSetForToday && alarmInstantToday > nowInstant) {
            return alarmInstantToday
        } else {
            // Otherwise, find the next occurrence based on repeat days
            // Find the earliest day such that:
            //   - repeatDays[dayIndex] == 'T'
            //   - The alarm time for that day is in the future

            // First, try to find the next occurrence within the next 7 days
            for (offset in 1..7) {
                val nextDate = todayDate.plus(DatePeriod(days = offset))
                val nextDayIndex = nextDate.dayOfWeek.toIndex()

                if (alarm.repeatDays.getOrNull(nextDayIndex) == 'T') {
                    val candidateDateTime = LocalDateTime(
                        date = nextDate,
                        time = LocalTime(alarm.hour, alarm.minute, 0)
                    )
                    return candidateDateTime.toInstant(timeZone)
                }
            }

            // If no future occurrence was found, and the alarm is set for today but the time has passed,
            // use today's time + 1 week
            if (isSetForToday) {
                val oneWeek = DatePeriod(days = 7)
                return alarmInstantToday.plus(oneWeek, timeZone)
            }
        }
    }

    // If no future day matched (repeatDays all 'F'), return null
    return null
}

/**
 * Compute "time left until next alarm" exactly as your old Calendar‐based function did,
 * but using kotlinx-datetime under the hood. Returns a string like:
 *
 *   • "5 hours 3 minutes"
 *   • "1 day 2 hours 15 minutes"
 *   • "45 minutes"
 *   • "0 minutes"  (if repeatDays is all 'F')
 *
 * We interpret repeatDays[0]=='T' as Sunday, [1]=='T' as Monday, …, [6]=='T' as Saturday.
 */
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
