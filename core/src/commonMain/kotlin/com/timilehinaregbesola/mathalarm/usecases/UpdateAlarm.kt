package com.timilehinaregbesola.mathalarm.usecases

import com.timilehinaregbesola.mathalarm.data.AlarmRepository
import com.timilehinaregbesola.mathalarm.domain.model.Alarm
import com.timilehinaregbesola.mathalarm.interactors.AlarmInteractor

class UpdateAlarm(private val alarmRepository: AlarmRepository, private val interactor: AlarmInteractor? = null) {
    suspend operator fun invoke(alarm: Alarm) {
        alarmRepository.updateAlarm(alarm)
        try {
            if (alarm.isOn) interactor?.update(alarm)
        } catch (e: kotlinx.coroutines.CancellationException) {
            throw e
        } catch (e: Exception) {
            alarmRepository.updateAlarm(alarm.copy(scheduleError = e.message ?: "Unable to update alarm"))
            throw e
        }
    }
}
