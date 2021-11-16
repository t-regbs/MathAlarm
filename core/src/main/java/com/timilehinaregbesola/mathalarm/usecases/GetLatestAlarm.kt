package com.timilehinaregbesola.mathalarm.usecases

import com.timilehinaregbesola.mathalarm.data.AlarmRepository

class GetLatestAlarm(private val alarmRepository: AlarmRepository) {
    suspend operator fun invoke() = alarmRepository.getLatestAlarmFromDatabase()
}