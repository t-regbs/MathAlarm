package com.timilehinaregbesola.mathalarm.usecases

import com.timilehinaregbesola.mathalarm.data.AlarmRepository
import com.timilehinaregbesola.mathalarm.interactors.AlarmInteractor
import com.timilehinaregbesola.mathalarm.interactors.NotificationInteractor
import com.timilehinaregbesola.mathalarm.provider.CalendarProvider
import java.util.*

/**
 * Use case to snooze an alarm.
 */
class SnoozeAlarm(
    private val calendarProvider: CalendarProvider,
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

        val snoozedTime = getSnoozedAlarm(calendarProvider.getCurrentCalendar(), minutes)
        alarm.apply {
            hour = snoozedTime[Calendar.HOUR_OF_DAY]
            minute = snoozedTime[Calendar.MINUTE]
        }
        alarmInteractor.schedule(alarm, alarm.repeat)
        notificationInteractor.dismiss(alarmId)
//        logger.debug("Task snoozed in $minutes minutes")
    }

    private fun getSnoozedAlarm(calendar: Calendar, minutes: Int): Calendar =
        calendar.apply { add(Calendar.MINUTE, minutes) }

    companion object {
        private const val DEFAULT_SNOOZE = 5
    }
}
