package com.timilehinaregbesola.mathalarm.usecases

import com.timilehinaregbesola.mathalarm.data.AlarmRepository
import com.timilehinaregbesola.mathalarm.domain.model.Alarm
import com.timilehinaregbesola.mathalarm.interactors.AlarmInteractor
import com.timilehinaregbesola.mathalarm.provider.AlarmTimeCalculator
import kotlinx.coroutines.flow.first

/**
 * Use case to reschedule tasks scheduled in the future or missing repeating.
 */
class RescheduleFutureAlarms(
    private val alarmRepository: AlarmRepository,
    private val alarmInteractor: AlarmInteractor,
    private val alarmTimeCalculator: AlarmTimeCalculator,
    private val scheduleNextAlarm: ScheduleNextAlarm
) {

    /**
     * Reschedule scheduled and missed repeating alarms.
     * 
     * This separates handling into two categories:
     * 1. Future alarms - alarms with next trigger time in the future
     * 2. Missed repeating alarms - repeating alarms whose time has passed
     */
    suspend operator fun invoke() {
        val activeAlarms = alarmRepository.getSavedAlarms().first().filter { it.isOn }
        
        val futureAlarms = activeAlarms.filter { isFutureAlarm(it) }
        val missedRepeating = activeAlarms.filter { isMissedRepeating(it) }

        futureAlarms.forEach { rescheduleFutureAlarm(it) }
        missedRepeating.forEach { rescheduleRepeatingAlarm(it) }
    }

    /**
     * Checks if the alarm's next trigger time is in the future.
     */
    private fun isFutureAlarm(alarm: Alarm): Boolean {
        val nextTime = alarmTimeCalculator.calculateNextAlarmTime(alarm) ?: return false
        return alarmTimeCalculator.isInFuture(nextTime)
    }

    /**
     * Checks if this is a repeating alarm whose time has already passed.
     */
    private fun isMissedRepeating(alarm: Alarm): Boolean {
        if (!alarm.repeat) return false
        val nextTime = alarmTimeCalculator.calculateNextAlarmTime(alarm) ?: return false
        return !alarmTimeCalculator.isInFuture(nextTime)
    }

    /**
     * Reschedule a future alarm by calculating its times and scheduling each.
     */
    private fun rescheduleFutureAlarm(alarm: Alarm) {
        val alarmTimes = alarmTimeCalculator.calculateAlarmTimes(alarm)
        alarmTimes.forEach { timeInMillis ->
            alarmInteractor.schedule(alarm, timeInMillis)
        }
    }

    /**
     * Reschedule a missed repeating alarm by advancing to its next occurrence.
     */
    private fun rescheduleRepeatingAlarm(alarm: Alarm) {
        scheduleNextAlarm(alarm)
    }
}
