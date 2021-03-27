package com.timilehinaregbesola.mathalarm.interactors

import com.timilehinaregbesola.mathalarm.data.AlarmRepository

class FindAlarm(private val alarmRepository: AlarmRepository) {
    suspend operator fun invoke(id: Long) = alarmRepository.findAlarm(id)
}
