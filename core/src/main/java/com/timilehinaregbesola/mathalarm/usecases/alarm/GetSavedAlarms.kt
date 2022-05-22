package com.timilehinaregbesola.mathalarm.usecases.alarm

import com.timilehinaregbesola.mathalarm.data.AlarmRepository

class GetSavedAlarms(private val alarmRepository: AlarmRepository) {
    operator fun invoke() = alarmRepository.getSavedAlarms()
}
