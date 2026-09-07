package com.timilehinaregbesola.mathalarm.usecases

import com.timilehinaregbesola.mathalarm.data.AlarmRepository
import com.timilehinaregbesola.mathalarm.domain.model.Alarm
import com.timilehinaregbesola.mathalarm.interactors.AlarmInteractor
import kotlinx.coroutines.CancellationException

class UpdateAlarm(
    private val alarmRepository: AlarmRepository,
    private val interactor: AlarmInteractor? = null
) {
    suspend operator fun invoke(alarm: Alarm) {
        val updated = if (alarm.snooze == 0) alarm.copy(snoozedUntil = null) else alarm
        try {
            if (alarm.snooze == 0) interactor?.cancelSnooze(alarm)
            alarmRepository.updateAlarm(updated)
            if (updated.isOn) interactor?.update(updated)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            alarmRepository.updateAlarm(updated.copy(scheduleError = e.message ?: "Unable to update alarm"))
            throw e
        }
    }
}
