package com.timilehinaregbesola.mathalarm.usecases

import com.timilehinaregbesola.mathalarm.data.AlarmRepository
import com.timilehinaregbesola.mathalarm.interactors.AlarmInteractor
import com.timilehinaregbesola.mathalarm.interactors.NotificationInteractor
import com.timilehinaregbesola.mathalarm.provider.DateTimeProvider
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.time.ExperimentalTime

/**
 * Use case to snooze an alarm.
 */
class SnoozeAlarm(
    private val dateTimeProvider: DateTimeProvider,
    private val notificationInteractor: NotificationInteractor,
    private val alarmInteractor: AlarmInteractor,
    private val alarmRepository: AlarmRepository
) {

    /**
     * Snoozes the alarm.
     *
     * @param alarmId the task id
     * @param minutes time to be snoozed in minutes
     *
     * @return observable to be subscribe
     */
    suspend operator fun invoke(alarmId: Long, minutes: Int = DEFAULT_SNOOZE) {
        require(minutes > 0) { "The delay minutes must be positive" }
        val alarm = alarmRepository.findAlarm(alarmId) ?: return

        val snoozedTime = getSnoozedDateTime(dateTimeProvider.getCurrentDateTime(), minutes)
        alarm.apply {
            hour = snoozedTime.hour
            minute = snoozedTime.minute
        }
        alarmInteractor.schedule(alarm, alarm.repeat)
        notificationInteractor.dismiss(alarmId)
    }

    @OptIn(ExperimentalTime::class)
    private fun getSnoozedDateTime(dateTime: LocalDateTime, minutes: Int): LocalDateTime {
        val tz = TimeZone.currentSystemDefault()
        val instant = dateTime.toInstant(tz)
        val newInstant = instant.plus(minutes.toLong(), DateTimeUnit.MINUTE)
        return newInstant.toLocalDateTime(tz)
    }

    companion object {
        private const val DEFAULT_SNOOZE = 5
    }
}
