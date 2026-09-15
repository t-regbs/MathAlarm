package com.timilehinaregbesola.mathalarm.usecases

import com.timilehinaregbesola.mathalarm.data.AlarmRepository
import com.timilehinaregbesola.mathalarm.provider.AlarmTimeCalculator
import kotlinx.datetime.TimeZone
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
        if (!alarm.isOn || !alarm.repeat || alarm.skippedDate != null) return null

        val next = alarmTimeCalculator.calculateNextAlarmTime(alarm) ?: return null
        val skippedDate = Instant.fromEpochMilliseconds(next)
            .toLocalDateTime(TimeZone.currentSystemDefault())
            .date
            .toString()
        val updated = alarm.copy(skippedDate = skippedDate)
        rescheduleFutureAlarms.restoreAlarm(updated)
        return skippedDate
    }

    suspend fun undo(alarmId: Long): Boolean {
        val alarm = alarmRepository.findAlarm(alarmId) ?: return false
        if (!alarm.isOn || !alarm.repeat || alarm.skippedDate == null) return false

        val updated = alarm.copy(skippedDate = null)
        rescheduleFutureAlarms.restoreAlarm(updated)
        return true
    }
}
