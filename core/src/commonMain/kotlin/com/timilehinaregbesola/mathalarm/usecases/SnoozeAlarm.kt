package com.timilehinaregbesola.mathalarm.usecases

import com.timilehinaregbesola.mathalarm.data.AlarmRepository
import com.timilehinaregbesola.mathalarm.interactors.AlarmInteractor
import com.timilehinaregbesola.mathalarm.interactors.NotificationInteractor
import com.timilehinaregbesola.mathalarm.provider.DateTimeProvider
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlin.time.Duration.Companion.minutes

@OptIn(kotlin.experimental.ExperimentalObjCRefinement::class)
@kotlin.native.HiddenFromObjC
class SnoozeAlarm(
    private val dateTimeProvider: DateTimeProvider,
    private val notificationInteractor: NotificationInteractor,
    private val alarmInteractor: AlarmInteractor,
    private val alarmRepository: AlarmRepository,
    private val onCleanupFailure: (Long, Exception) -> Unit = { _, _ -> },
    private val onCleanupSuccess: (Long) -> Unit = {},
    private val onSnoozed: () -> Unit = {},
) {
    suspend operator fun invoke(
        alarmId: Long,
        minutes: Int? = null,
        expectedActiveAt: Long? = null,
    ): Boolean {
        val alarm = alarmRepository.findAlarm(alarmId) ?: return false
        if (!alarm.isOn || !alarm.canSnooze || alarm.activeAt == null) return false
        if (expectedActiveAt != null && alarm.activeAt != expectedActiveAt) return false
        val delay = minutes ?: alarm.snooze
        require(delay > 0)
        val pendingTime = AlarmCommandJournal.pendingSnooze(alarm)
        val previousError = if (pendingTime != null) AlarmCommandJournal.previousError(alarm) else alarm.scheduleError
        val time = pendingTime ?: (dateTimeProvider.getCurrentDateTime().toInstant(TimeZone.currentSystemDefault()) + delay.minutes)
            .toEpochMilliseconds()
        // Preserve the recurring schedule and keep ringing unless the snooze was accepted.
        val snoozed = alarm.copy(snoozedUntil = time, activeAt = null, snoozeCount = alarm.snoozeCount + 1)
        // Persist intent without consuming the occurrence or its allowance. If native
        // acceptance wins a race with process death/final-write failure, launch retries
        // the exact same time and identity rather than creating another snooze.
        val desired = alarm.copy(scheduleError = AlarmCommandJournal.snooze(time, previousError))
        alarmRepository.updateAlarm(desired)
        try {
            alarmInteractor.scheduleSnooze(snoozed, time)
        } catch (error: kotlinx.coroutines.CancellationException) {
            throw error // Leave the durable intent and unresolved recovery for reconciliation.
        } catch (error: Exception) {
            runCatching { alarmRepository.updateAlarm(alarm) }
            throw error
        }
        val accepted = snoozed.copy(scheduleError = AlarmCommandJournal.cleanup(previousError))
        alarmRepository.updateAlarm(accepted)
        AlarmCommandJournal.cleanupAccepted(accepted, alarmRepository, alarmInteractor,
            notificationInteractor, onCleanupFailure, onCleanupSuccess)
        onSnoozed()
        return true
    }
}
