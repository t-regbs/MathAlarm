package com.timilehinaregbesola.mathalarm.usecases.alarm

import com.timilehinaregbesola.mathalarm.data.AlarmRepository

class FindAlarm(private val alarmRepository: AlarmRepository) {
    suspend operator fun invoke(id: Long) = alarmRepository.findAlarm(id)
}
