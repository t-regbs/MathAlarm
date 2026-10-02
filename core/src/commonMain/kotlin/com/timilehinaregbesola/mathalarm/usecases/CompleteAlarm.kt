package com.timilehinaregbesola.mathalarm.usecases

import com.timilehinaregbesola.mathalarm.data.AlarmRepository
import com.timilehinaregbesola.mathalarm.domain.model.Alarm
import com.timilehinaregbesola.mathalarm.interactors.AlarmInteractor
import com.timilehinaregbesola.mathalarm.interactors.NotificationInteractor
import com.timilehinaregbesola.mathalarm.provider.DateTimeProvider
import com.timilehinaregbesola.mathalarm.provider.DateTimeProviderImpl
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant

@OptIn(kotlin.experimental.ExperimentalObjCRefinement::class)
@kotlin.native.HiddenFromObjC
class CompleteAlarm(
    private val alarmRepository: AlarmRepository,
    private val alarmInteractor: AlarmInteractor,
    private val notificationInteractor: NotificationInteractor,
    private val dateTimeProvider: DateTimeProvider = DateTimeProviderImpl(),
    private val onCleanupFailure: (Long, Exception) -> Unit = { _, _ -> },
    private val onCleanupSuccess: (Long) -> Unit = {},
    private val onCompleted: () -> Unit = {},
) {
    suspend operator fun invoke(alarmId: Long, expectedActiveAt: Long? = null): Boolean {
        val alarm = alarmRepository.findAlarm(alarmId)
        if (alarm == null) {
            if (expectedActiveAt == null) notificationInteractor.dismiss(alarmId)
            return false
        }
        if (expectedActiveAt != null && alarm.activeAt != expectedActiveAt) return false
        val now = dateTimeProvider.getCurrentDateTime()
            .toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds()
        // Known later repeating deliveries remain valid while presentation is serialized,
        // even if their wall-clock time passed while this occurrence was unresolved.
        val pendingAfter = if (alarm.repeat) alarm.activeAt ?: now else now
        val pending = alarm.pendingTimes.filter { it > pendingAfter }
        val hasRemaining = if (alarm.scheduleInitialized) {
            pending.isNotEmpty()
        } else {
            alarmInteractor.hasPendingOccurrence(alarm)
        }
        val remainsEnabled = alarm.isOn && (alarm.repeat || hasRemaining)
        val updated = alarm.copy(
            isOn = remainsEnabled,
            pendingTimes = pending,
            activeAt = null,
            snoozeCount = 0,
            snoozedUntil = null,
            scheduleError = if (alarm.repeat || hasRemaining) alarm.scheduleError else null
        )
        alarmInteractor.cancelSnooze(alarm)
        // Native cancellation may fail, but must not cancel unresolved recovery before
        // the accepted resolution is durable. A failed write retains the active occurrence.
        if (!updated.isOn) alarmInteractor.cancelRegularOccurrences(alarm)
        val accepted = updated.copy(scheduleError = AlarmCommandJournal.cleanup(updated.scheduleError))
        alarmRepository.updateAlarm(accepted)
        AlarmCommandJournal.cleanupAccepted(accepted, alarmRepository, alarmInteractor,
            notificationInteractor, onCleanupFailure, onCleanupSuccess)
        if (alarm.activeAt != null) onCompleted()
        return true
    }

    // Never overwrite current settings with the snapshot embedded in a notification.
    suspend operator fun invoke(alarm: Alarm) = invoke(alarm.alarmId)
}
