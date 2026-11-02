package com.timilehinaregbesola.mathalarm.utils

import com.timilehinaregbesola.mathalarm.domain.model.Alarm
import com.timilehinaregbesola.mathalarm.provider.remainingOccurrences
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
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
fun Alarm.initLocalDateTimeInSystemZone(
    clock: Clock = Clock.System,
    timeZone: TimeZone = TimeZone.currentSystemDefault()
): LocalDateTime {
    val nowInstant = clock.now()
    val tz = timeZone
    val today = nowInstant.toLocalDateTime(tz).date
    return LocalDateTime(
        date = today,
        time = LocalTime(hour, minute, 0)
    )
}

/** Next scheduled occurrence, using the same calendar and persistence rules as recovery. */
@OptIn(ExperimentalTime::class)
fun calculateNextAlarmTime(alarm: Alarm, timeZone: TimeZone = TimeZone.currentSystemDefault(), clock: Clock = Clock.System): Instant? {
    val calculator = occurrenceCalculator(timeZone, clock)
    return (alarm.remainingOccurrences(calculator, timeZone) + listOfNotNull(alarm.snoozedUntil))
        .filter(calculator::isInFuture).minOrNull()?.let(Instant::fromEpochMilliseconds)
}

private fun occurrenceCalculator(timeZone: TimeZone, clock: Clock) = com.timilehinaregbesola.mathalarm.provider.AlarmTimeCalculatorImpl(
        object : com.timilehinaregbesola.mathalarm.provider.DateTimeProvider {
            override fun getCurrentDateTime() = clock.now().toLocalDateTime(timeZone)
        }
    ) { timeZone }
