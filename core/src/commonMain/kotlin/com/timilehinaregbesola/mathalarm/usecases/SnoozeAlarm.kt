package com.timilehinaregbesola.mathalarm.usecases

import com.timilehinaregbesola.mathalarm.data.AlarmRepository
import com.timilehinaregbesola.mathalarm.interactors.AlarmInteractor
import com.timilehinaregbesola.mathalarm.interactors.NotificationInteractor
import com.timilehinaregbesola.mathalarm.provider.DateTimeProvider
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlin.time.Duration.Companion.minutes

class SnoozeAlarm(
    private val dateTimeProvider: DateTimeProvider,
    private val notificationInteractor: NotificationInteractor,
    private val alarmInteractor: AlarmInteractor,
    private val alarmRepository: AlarmRepository,
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
        val time = (dateTimeProvider.getCurrentDateTime().toInstant(TimeZone.currentSystemDefault()) + delay.minutes)
            .toEpochMilliseconds()
        // Preserve the recurring schedule and keep ringing unless the snooze was accepted.
        val snoozed = alarm.copy(snoozedUntil = time, activeAt = null, snoozeCount = alarm.snoozeCount + 1)
        alarmInteractor.scheduleSnooze(snoozed, time)
        alarmRepository.updateAlarm(snoozed)
        notificationInteractor.dismiss(alarmId)
        onSnoozed()
        return true
    }
}
