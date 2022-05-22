package com.timilehinaregbesola.mathalarm.usecases.alarm

import com.timilehinaregbesola.mathalarm.data.AlarmRepository
import com.timilehinaregbesola.mathalarm.domain.model.Alarm
import com.timilehinaregbesola.mathalarm.interactors.AlarmInteractor
import kotlinx.coroutines.flow.collect

/**
 * Use case to reschedule tasks scheduled in the future or missing repeating.
 */
class RescheduleFutureAlarms(
    private val alarmRepository: AlarmRepository,
    private val alarmInteractor: AlarmInteractor
) {

    /**
     * Reschedule scheduled and misses repeating tasks.
     */
    suspend operator fun invoke() {
        alarmRepository.getSavedAlarms().collect { list ->
            val futureAlarms = list.filter { it.isOn }
            futureAlarms.forEach { rescheduleFutureAlarm(it) }
        }
    }

    private fun rescheduleFutureAlarm(alarm: Alarm) {
        alarmInteractor.schedule(alarm, alarm.repeat)
//        logger.debug("Task '${alarm.title} rescheduled to '${alarm.dueDate}")
    }
}
