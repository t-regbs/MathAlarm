package com.timilehinaregbesola.mathalarm.usecases

import com.timilehinaregbesola.mathalarm.data.AlarmRepository
import com.timilehinaregbesola.mathalarm.domain.model.Alarm
import com.timilehinaregbesola.mathalarm.provider.AlarmTimeCalculator
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

class SkipNextAlarm(
    private val alarmRepository: AlarmRepository,
    private val alarmTimeCalculator: AlarmTimeCalculator,
    private val rescheduleFutureAlarms: RescheduleFutureAlarms,
) {
    /** Skips the next normal occurrence and returns its local date for presentation. */
    suspend operator fun invoke(alarmId: Long): String? {
        val alarm = alarmRepository.findAlarm(alarmId) ?: return null
        if (!alarm.isOn || alarm.skippedDate != null) return null

        val zone = TimeZone.currentSystemDefault()
        val oneTimeOccurrences = if (alarm.repeat) {
            emptyList()
        } else {
            remainingOneTimeOccurrences(alarm)
        }
        val next = if (alarm.repeat) {
            alarmTimeCalculator.calculateNextAlarmTime(alarm)
        } else {
            oneTimeOccurrences.firstOrNull()
        } ?: return null
        val skippedDate = Instant.fromEpochMilliseconds(next)
            .toLocalDateTime(zone)
            .date
            .toString()
        val updated = if (alarm.repeat) {
            alarm.copy(skippedDate = skippedDate)
        } else {
            val remaining = oneTimeOccurrences.filterNot { it == next }
            alarm.copy(
                isOn = remaining.isNotEmpty(),
                pendingTimes = remaining,
                scheduleInitialized = true,
                scheduleTimeZone = alarm.scheduleTimeZone ?: zone.id,
                skippedDate = skippedDate,
            )
        }
        rescheduleFutureAlarms.restoreAlarm(updated)
        return skippedDate
    }

    suspend fun undo(alarmId: Long): Boolean {
        val alarm = alarmRepository.findAlarm(alarmId) ?: return false
        val skippedDate = alarm.skippedDate ?: return false

        val updated = if (alarm.repeat) {
            if (!alarm.isOn) return false
            alarm.copy(skippedDate = null)
        } else {
            val zone = alarm.scheduleTimeZone
                ?.let { runCatching { TimeZone.of(it) }.getOrNull() }
                ?: TimeZone.currentSystemDefault()
            val date = runCatching { LocalDate.parse(skippedDate) }.getOrNull() ?: return false
            val skippedTime = LocalDateTime(date, LocalTime(alarm.hour, alarm.minute))
                .toInstant(zone)
                .toEpochMilliseconds()
            if (!alarmTimeCalculator.isInFuture(skippedTime)) {
                alarmRepository.updateAlarm(alarm.copy(skippedDate = null))
                return false
            }
            alarm.copy(
                isOn = true,
                pendingTimes = (alarm.pendingTimes + skippedTime).distinct().sorted(),
                scheduleInitialized = true,
                scheduleTimeZone = zone.id,
                skippedDate = null,
            )
        }
        rescheduleFutureAlarms.restoreAlarm(updated)
        return true
    }

    private fun remainingOneTimeOccurrences(alarm: Alarm): List<Long> {
        val occurrences = if (alarm.scheduleInitialized) {
            alarm.pendingTimes
        } else {
            alarmTimeCalculator.calculateAlarmTimes(alarm)
        }
        return occurrences.filter(alarmTimeCalculator::isInFuture).sorted()
    }
}
