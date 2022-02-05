package com.timilehinaregbesola.mathalarm.usecases

import com.timilehinaregbesola.mathalarm.data.AlarmRepository
import com.timilehinaregbesola.mathalarm.domain.model.Alarm
import com.timilehinaregbesola.mathalarm.interactors.AlarmInteractor

class UpdateAlarm(
    private val alarmRepository: AlarmRepository,
    private val alarmInteractor: AlarmInteractor
) {
    suspend operator fun invoke(alarm: Alarm) {
        if (!alarm.isOn) alarmInteractor.cancel(alarm)
        alarmRepository.updateAlarm(alarm)
    }
}
