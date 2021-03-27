package com.timilehinaregbesola.mathalarm.interactors

import com.timilehinaregbesola.mathalarm.data.AlarmRepository
import com.timilehinaregbesola.mathalarm.domain.Alarm

class UpdateAlarm(private val alarmRepository: AlarmRepository) {
    suspend operator fun invoke(alarm: Alarm) = alarmRepository.updateAlarm(alarm)
}
