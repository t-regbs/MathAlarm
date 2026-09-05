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
    private val alarmRepository: AlarmRepository
) {
    suspend operator fun invoke(alarmId: Long, minutes: Int? = null) {
        val alarm = alarmRepository.findAlarm(alarmId) ?: return
        if (!alarm.isOn) return
        val delay = minutes ?: alarm.snooze.takeIf { it > 0 } ?: return
        require(delay > 0)
        val time = (dateTimeProvider.getCurrentDateTime().toInstant(TimeZone.currentSystemDefault()) + delay.minutes)
            .toEpochMilliseconds()
        // Preserve the recurring schedule and keep ringing unless the snooze was accepted.
        alarmInteractor.scheduleSnooze(alarm, time)
        alarmRepository.updateAlarm(alarm.copy(snoozedUntil = time, activeAt = null))
        notificationInteractor.dismiss(alarmId)
    }
}
