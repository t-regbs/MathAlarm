package com.timilehinaregbesola.mathalarm.usecases.alarm

import com.timilehinaregbesola.mathalarm.data.AlarmRepository
import com.timilehinaregbesola.mathalarm.interactors.NotificationInteractor

/**
 * Use case to show an alarm.
 */
class ShowAlarm(
    private val alarmRepository: AlarmRepository,
    private val notificationInteractor: NotificationInteractor,
    private val scheduleNextAlarm: ScheduleNextAlarm
) {

    /**
     * Shows the alarm.
     *
     * @param alarmId the alarm id to be shown
     */
    suspend operator fun invoke(alarmId: Long) {
        val alarm = alarmRepository.findAlarm(alarmId) ?: return

        if (alarm.repeat && alarm.isOn) {
            scheduleNextAlarm(alarm)
        } else {
        }

        if (alarm.isOn.not()) {
//            Timber.d("Task '${alarm.title}' is already completed. Will not notify")
            return
        } else {
//            logger.debug("Notifying task '${alarm.title}'")
            notificationInteractor.show(alarm)
        }
    }
}
