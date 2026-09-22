package com.timilehinaregbesola.mathalarm.framework

import com.timilehinaregbesola.mathalarm.domain.model.Alarm
import kotlin.time.Clock
import kotlinx.coroutines.flow.asSharedFlow

/** Caller holds the command lock. System-delivered iOS alarms have no delivery receiver. */
internal suspend fun Usecases.consumeDueOccurrence(
    alarmId: Long,
    now: Long = Clock.System.now().toEpochMilliseconds(),
): Alarm? {
    val current = findAlarm(alarmId) ?: return null
    if (current.isOn && current.scheduleInitialized) {
        val snoozed = current.snoozedUntil?.takeIf { it <= now }
        val regular = current.pendingTimes.filter { it <= now }.maxOrNull()
        val due = listOfNotNull(snoozed, regular).maxOrNull()
        if (due != null && due > (current.activeAt ?: Long.MIN_VALUE)) {
            showAlarm(current.alarmId, due, snoozed = due == snoozed)
        }
    }
    return findAlarm(alarmId)
}

/** True also acknowledges a duplicate action while the accepted snooze is pending. */
internal suspend fun Usecases.snoozeFromNotification(
    alarmId: Long,
    now: Long = Clock.System.now().toEpochMilliseconds(),
): Boolean = command {
    val current = findAlarm(alarmId) ?: return@command false
    if (!current.isOn) return@command false
    if (current.activeAt == null && current.snoozedUntil?.let { it > now } == true) {
        return@command true
    }
    val alarm = consumeDueOccurrence(alarmId, now) ?: return@command false
    val activeAt = alarm.activeAt ?: return@command false
    snoozeAlarm(alarmId, expectedActiveAt = activeAt)
}

/** Closes a challenge already visible when a notification action succeeds. */
internal object NotificationSnoozeEvents {
    private val events = kotlinx.coroutines.flow.MutableSharedFlow<Long>()
    val snoozed = events.asSharedFlow()

    suspend fun notifySnoozed(alarmId: Long) = events.emit(alarmId)
}
