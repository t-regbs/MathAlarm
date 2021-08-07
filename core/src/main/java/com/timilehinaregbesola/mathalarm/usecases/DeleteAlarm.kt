package com.timilehinaregbesola.mathalarm.usecases

import com.timilehinaregbesola.mathalarm.data.AlarmRepository
import com.timilehinaregbesola.mathalarm.domain.model.Alarm

class DeleteAlarm(private val alarmRepository: AlarmRepository) {
    suspend operator fun invoke(alarm: Alarm) = alarmRepository.deleteAlarm(alarm)
}
