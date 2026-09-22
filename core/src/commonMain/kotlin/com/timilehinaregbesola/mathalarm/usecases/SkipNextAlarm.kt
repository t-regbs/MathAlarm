package com.timilehinaregbesola.mathalarm.usecases

import com.timilehinaregbesola.mathalarm.data.AlarmRepository
import com.timilehinaregbesola.mathalarm.provider.AlarmTimeCalculator
import com.timilehinaregbesola.mathalarm.provider.activeSkippedDate
import com.timilehinaregbesola.mathalarm.provider.remainingOccurrences
import com.timilehinaregbesola.mathalarm.provider.skippedTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

class SkipNextAlarm(
    private val alarmRepository: AlarmRepository,
    private val alarmTimeCalculator: AlarmTimeCalculator,
    private val rescheduleFutureAlarms: RescheduleFutureAlarms,
    private val timeZone: () -> TimeZone = { TimeZone.currentSystemDefault() },
) {
    /** Skips the next normal occurrence and returns its local date for presentation. */
    suspend operator fun invoke(alarmId: Long): String? {
        rescheduleFutureAlarms.clearExpiredSkips()
        val alarm = alarmRepository.findAlarm(alarmId) ?: return null
        if (!alarm.canSkipNext) return null

        val zone = timeZone()
        val occurrences = alarm.remainingOccurrences(alarmTimeCalculator, zone)
        val next = occurrences.firstOrNull() ?: return null
        val skippedDate = Instant.fromEpochMilliseconds(next)
            .toLocalDateTime(zone)
            .date
            .toString()
        val updated = if (alarm.repeat) {
            alarm.copy(skippedDate = skippedDate)
        } else {
            val remaining = occurrences.drop(1)
            alarm.copy(
                isOn = remaining.isNotEmpty(),
                pendingTimes = remaining,
                scheduleInitialized = true,
                scheduleTimeZone = zone.id,
                skippedDate = skippedDate,
            )
        }
        rescheduleFutureAlarms.restoreAlarm(updated, preserveSnooze = true)
        return skippedDate
    }

    suspend fun undo(alarmId: Long, expectedSkippedDate: String? = null): Boolean {
        val alarm = alarmRepository.findAlarm(alarmId) ?: return false
        // A queued snackbar must never undo a more recent skip.
        if (expectedSkippedDate != null && alarm.skippedDate != expectedSkippedDate) return false
        val zone = timeZone()
        val skippedTime = alarm.skippedTime(zone)
        if (alarm.activeSkippedDate(alarmTimeCalculator, zone) == null || skippedTime == null) {
            if (alarm.skippedDate != null) alarmRepository.updateAlarm(alarm.copy(skippedDate = null))
            return false
        }
        val updated = if (alarm.repeat) {
            if (!alarm.isOn) return false
            alarm.copy(skippedDate = null)
        } else {
            alarm.copy(
                isOn = true,
                pendingTimes = (alarm.remainingOccurrences(alarmTimeCalculator, zone) + skippedTime).distinct().sorted(),
                scheduleInitialized = true,
                scheduleTimeZone = zone.id,
                skippedDate = null,
            )
        }
        rescheduleFutureAlarms.restoreAlarm(updated, preserveSnooze = true)
        return true
    }
}
