package com.timilehinaregbesola.mathalarm.interactors

import com.timilehinaregbesola.mathalarm.data.AlarmRepository

class DeleteAlarmWithId(private val alarmRepository: AlarmRepository) {
    suspend operator fun invoke(alarmId: Long) = alarmRepository.deleteAlarmWithId(alarmId)
}
