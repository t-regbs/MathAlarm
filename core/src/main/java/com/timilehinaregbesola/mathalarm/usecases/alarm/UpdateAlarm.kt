package com.timilehinaregbesola.mathalarm.usecases.alarm

import com.timilehinaregbesola.mathalarm.data.AlarmRepository
import com.timilehinaregbesola.mathalarm.domain.model.Alarm

class UpdateAlarm(
    private val alarmRepository: AlarmRepository
) {
    suspend operator fun invoke(alarm: Alarm) = alarmRepository.updateAlarm(alarm)
}
