package com.timilehinaregbesola.mathalarm.provider

import com.timilehinaregbesola.mathalarm.domain.model.Alarm
import kotlinx.datetime.*
import kotlin.time.Instant

/** Calendar exceptions follow the alarm's local wall-clock time when travelling. */
fun Alarm.skippedTime(zone: TimeZone): Long? = skippedDate?.let { value ->
    runCatching {
        LocalDateTime(LocalDate.parse(value), LocalTime(hour, minute))
            .toInstant(zone).toEpochMilliseconds()
    }.getOrNull()
}

fun Alarm.activeSkippedDate(calculator: AlarmTimeCalculator, zone: TimeZone): String? =
    skippedDate.takeIf { skippedTime(zone)?.let(calculator::isInFuture) == true }

/** Rebase persisted one-time dates before filtering; never generate a fresh weekly cycle. */
fun Alarm.remainingOccurrences(calculator: AlarmTimeCalculator, zone: TimeZone): List<Long> {
    if (repeat || !scheduleInitialized) return calculator.calculateAlarmTimes(this).sorted()
    val previousZone = scheduleTimeZone?.let(TimeZone::of) ?: zone
    return pendingTimes.map { time ->
        Instant.fromEpochMilliseconds(time).toLocalDateTime(previousZone)
            .toInstant(zone).toEpochMilliseconds()
    }.filter(calculator::isInFuture).sorted()
}
