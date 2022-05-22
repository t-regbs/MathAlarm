package com.timilehinaregbesola.mathalarm.usecases.alarm

import com.timilehinaregbesola.mathalarm.data.AlarmRepository
import com.timilehinaregbesola.mathalarm.domain.model.Alarm
import com.timilehinaregbesola.mathalarm.interactors.AlarmInteractor

class ClearAlarms(
    private val alarmRepository: AlarmRepository,
    private val alarmInteractor: AlarmInteractor
) {
    suspend operator fun invoke(alarms: List<Alarm>) {
        for (alarm in alarms) {
            alarmInteractor.cancel(alarm)
        }
        alarmRepository.clear()
    }
}
