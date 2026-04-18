package com.timilehinaregbesola.mathalarm.usecases

import com.timilehinaregbesola.mathalarm.data.AlarmRepository
import com.timilehinaregbesola.mathalarm.domain.model.Alarm
import com.timilehinaregbesola.mathalarm.interactors.AlarmInteractor
import com.timilehinaregbesola.mathalarm.interactors.NotificationInteractor
import com.timilehinaregbesola.mathalarm.provider.DateTimeProvider
import com.timilehinaregbesola.mathalarm.provider.DateTimeProviderImpl
import kotlinx.datetime.DayOfWeek

/**
 * Use case to set an alarm as completed in the database.
 */
class CompleteAlarm(
    private val alarmRepository: AlarmRepository,
    private val alarmInteractor: AlarmInteractor,
    private val notificationInteractor: NotificationInteractor,
    private val dateTimeProvider: DateTimeProvider = DateTimeProviderImpl(),
) {

    /**
     * Completes the given alarm.
     *
     * @param alarmId the alarm id
     *
     */
    suspend operator fun invoke(alarmId: Long) {
        val alarm = alarmRepository.findAlarm(alarmId) ?: return
        invoke(alarm)
    }

    /**
     * Completes the given alarm.
     *
     * @param alarm the alarm to be updated
     *
     */
    suspend operator fun invoke(alarm: Alarm) {
        when {
            alarm.repeat -> {
                notificationInteractor.dismiss(alarm.alarmId)
            }
            alarmInteractor.hasPendingOccurrence(alarm) || hasRemainingSelectedDaysLaterThisWeek(alarm) -> {
                notificationInteractor.dismiss(alarm.alarmId)
            }
            else -> {
                val updatedAlarm = updateAlarmAsCompleted(alarm)
                alarmRepository.updateAlarm(updatedAlarm)
                alarmInteractor.cancel(alarm)
                notificationInteractor.dismiss(alarm.alarmId)
            }
        }
    }

    private fun updateAlarmAsCompleted(alarm: Alarm) =
        alarm.copy(isOn = false)

    private fun hasRemainingSelectedDaysLaterThisWeek(alarm: Alarm): Boolean {
        val currentDayIndex = dateTimeProvider.getCurrentDateTime().date.dayOfWeek.toIndex()
        return ((currentDayIndex + 1)..6).any { alarm.repeatDays.getOrNull(it) == 'T' }
    }

    private fun DayOfWeek.toIndex(): Int = when (this) {
        DayOfWeek.SUNDAY -> 0
        DayOfWeek.MONDAY -> 1
        DayOfWeek.TUESDAY -> 2
        DayOfWeek.WEDNESDAY -> 3
        DayOfWeek.THURSDAY -> 4
        DayOfWeek.FRIDAY -> 5
        DayOfWeek.SATURDAY -> 6
    }
}
