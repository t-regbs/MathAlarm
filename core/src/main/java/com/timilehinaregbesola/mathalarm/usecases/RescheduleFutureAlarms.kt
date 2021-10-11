package com.timilehinaregbesola.mathalarm.usecases

import com.timilehinaregbesola.mathalarm.data.AlarmRepository
import com.timilehinaregbesola.mathalarm.domain.model.Alarm
import com.timilehinaregbesola.mathalarm.interactors.AlarmInteractor
import com.timilehinaregbesola.mathalarm.provider.CalendarProvider
import java.util.*

/**
 * Use case to reschedule tasks scheduled in the future or missing repeating.
 */
class RescheduleFutureAlarms(
    private val alarmRepository: AlarmRepository,
    private val alarmInteractor: AlarmInteractor,
    private val calendarProvider: CalendarProvider,
    private val scheduleNextAlarm: ScheduleNextAlarm
) {

    /**
     * Reschedule scheduled and misses repeating tasks.
     */
    suspend operator fun invoke() {
        val uncompletedAlarms = alarmRepository.getAlarms().filter { it.isOn }
        val futureAlarms = uncompletedAlarms.filter { isInFuture(it.initCalendar()) }
        val missedRepeating = uncompletedAlarms.filter { isMissedRepeating(it) }

        futureAlarms.forEach { rescheduleFutureAlarm(it) }
        missedRepeating.forEach { rescheduleRepeatingAlarm(it) }
    }

    private fun isInFuture(calendar: Calendar?): Boolean {
        val currentTime = calendarProvider.getCurrentCalendar()
        return calendar?.after(currentTime) ?: false
    }

    private fun isMissedRepeating(alarm: Alarm): Boolean {
        val currentTime = calendarProvider.getCurrentCalendar()
        return alarm.repeat && alarm.initCalendar().before(currentTime)
    }

    private fun rescheduleFutureAlarm(alarm: Alarm) {
        alarmInteractor.schedule(alarm, alarm.repeat)
//        logger.debug("Task '${alarm.title} rescheduled to '${alarm.dueDate}")
    }

    private suspend fun rescheduleRepeatingAlarm(alarm: Alarm) {
        scheduleNextAlarm(alarm)
//        logger.debug("Repeating task '${alarm.title} rescheduled to '${task.dueDate}")
    }

    private fun Alarm.initCalendar(): Calendar {
        val cal = Calendar.getInstance()
        cal[Calendar.HOUR_OF_DAY] = hour
        cal[Calendar.MINUTE] = minute
        cal[Calendar.SECOND] = 0
        return cal
    }
}
