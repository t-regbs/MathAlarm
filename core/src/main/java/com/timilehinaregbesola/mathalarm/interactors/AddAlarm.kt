package com.timilehinaregbesola.mathalarm.interactors

import com.timilehinaregbesola.mathalarm.data.AlarmRepository
import com.timilehinaregbesola.mathalarm.domain.Alarm

class AddAlarm(private val alarmRepository: AlarmRepository) {
    suspend operator fun invoke(alarm: Alarm) = alarmRepository.addAlarm(alarm)
}
