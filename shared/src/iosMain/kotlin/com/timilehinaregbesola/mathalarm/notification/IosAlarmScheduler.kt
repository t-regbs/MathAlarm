package com.timilehinaregbesola.mathalarm.notification

import co.touchlab.kermit.Logger
import com.timilehinaregbesola.mathalarm.alarm.AlarmScheduleRequest
import com.timilehinaregbesola.mathalarm.alarm.AlarmSchedulerBridge
import com.timilehinaregbesola.mathalarm.domain.model.Alarm
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

/** AlarmKit is the delivery backend for the iOS/iPadOS 26+ app. */
class IosAlarmScheduler(private val logger: Logger) {
    /** AlarmKit owns weekly recurrence. Reinstall only a missing weekday after delivery. */
    suspend fun ensureRepeatingOccurrence(alarm: Alarm, timeInMillis: Long) {
        val key = regularOccurrenceKey(timeInMillis)
        if (AlarmSchedulerBridge.hasPendingOccurrence(alarm.alarmId, key)) {
            logger.d { "Retaining AlarmKit weekly registration: id=${alarm.alarmId}, key=$key" }
            return
        }
        scheduleOccurrence(alarm, timeInMillis, repeating = true)
    }

    private fun regularOccurrenceKey(timeInMillis: Long): String {
        val local = Instant.fromEpochMilliseconds(timeInMillis).toLocalDateTime(TimeZone.currentSystemDefault())
        val day = (local.dayOfWeek.ordinal + 1) % 7
        return "day_$day"
    }

    /** Schedule exactly this occurrence. Snoozes have their own stable identity. */
    suspend fun scheduleOccurrence(
        alarm: Alarm,
        timeInMillis: Long,
        repeating: Boolean = false,
        snooze: Boolean = false
    ) {
        val local = Instant.fromEpochMilliseconds(timeInMillis).toLocalDateTime(TimeZone.currentSystemDefault())
        val day = local.dayOfWeek.ordinal.let { (it + 1) % 7 } // shared Sunday-first convention
        val key = if (snooze) "snooze" else regularOccurrenceKey(timeInMillis)
        val days = "FFFFFFF".toCharArray().apply { this[day] = 'T' }.concatToString()
        val request = AlarmScheduleRequest(
            alarmId = alarm.alarmId,
            hour = alarm.hour,
            minute = alarm.minute,
            title = alarm.title,
            soundName = alarm.alarmTone,
            repeatDays = days,
            vibrate = alarm.vibrate,
            difficulty = alarm.difficulty,
            repeats = repeating,
            timeInMillis = timeInMillis,
            occurrenceKey = key
        )
        val result = AlarmSchedulerBridge.scheduleWithAlarmKit(request)
        check(result.success) { result.errorMessage ?: "Unable to schedule alarm with AlarmKit" }
    }

    fun cancelSnooze(alarm: Alarm) =
        AlarmSchedulerBridge.cancelOccurrence(alarm.alarmId, "snooze")

    /** Challenge completion and accepted snoozes cancel recovery independently. */
    fun cancelRecovery(alarmId: Long) =
        AlarmSchedulerBridge.cancelOccurrence(alarmId, "recovery")

    /** Keep the independent snooze and recovery registrations intact. */
    fun cancelRegularOccurrences(alarm: Alarm) {
        val failures = (0..6).mapNotNull { day ->
            runCatching { AlarmSchedulerBridge.cancelOccurrence(alarm.alarmId, "day_$day") }
                .exceptionOrNull()?.let { it.message ?: it.toString() }
        }
        check(failures.isEmpty()) { failures.joinToString("; ") }
    }

    fun cancelAlarm(alarm: Alarm) {
        logger.d { "Cancelling AlarmKit alarm: id=${alarm.alarmId}" }
        AlarmSchedulerBridge.cancelAlarm(alarm.alarmId)
    }

    suspend fun hasPendingOccurrence(alarm: Alarm): Boolean {
        val keys = alarm.pendingTimes.map { time ->
            val day = Instant.fromEpochMilliseconds(time)
                .toLocalDateTime(TimeZone.currentSystemDefault()).dayOfWeek.ordinal.let { (it + 1) % 7 }
            "day_$day"
        }.toMutableSet()
        if (alarm.snoozedUntil != null) keys.add("snooze")
        if (keys.isEmpty()) return false
        return keys.all { key -> AlarmSchedulerBridge.hasPendingOccurrence(alarm.alarmId, key) }
    }
}
