package com.timilehinaregbesola.mathalarm.usecases

import com.timilehinaregbesola.mathalarm.data.AlarmRepository
import com.timilehinaregbesola.mathalarm.domain.model.Alarm
import com.timilehinaregbesola.mathalarm.interactors.AlarmInteractor
import com.timilehinaregbesola.mathalarm.interactors.NotificationInteractor
import com.timilehinaregbesola.mathalarm.provider.DateTimeProvider
import com.timilehinaregbesola.mathalarm.provider.DateTimeProviderImpl
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant

class CompleteAlarm(
    private val alarmRepository: AlarmRepository,
    private val alarmInteractor: AlarmInteractor,
    private val notificationInteractor: NotificationInteractor,
    private val dateTimeProvider: DateTimeProvider = DateTimeProviderImpl(),
) {
    suspend operator fun invoke(alarmId: Long) {
        val alarm = alarmRepository.findAlarm(alarmId)
        if (alarm == null) {
            notificationInteractor.dismiss(alarmId)
            return
        }
        val now = dateTimeProvider.getCurrentDateTime()
            .toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds()
        val pending = alarm.pendingTimes.filter { it > now }
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
            snoozedUntil = null,
            scheduleError = if (alarm.repeat || hasRemaining) alarm.scheduleError else null
        )
        alarmInteractor.cancelSnooze(alarm)
        if (!updated.isOn) alarmInteractor.cancel(alarm)
        alarmRepository.updateAlarm(updated)
        notificationInteractor.dismiss(alarmId)
    }

    // Never overwrite current settings with the snapshot embedded in a notification.
    suspend operator fun invoke(alarm: Alarm) = invoke(alarm.alarmId)
}
