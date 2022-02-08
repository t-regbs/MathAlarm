package com.timilehinaregbesola.mathalarm.usecases

import com.timilehinaregbesola.mathalarm.domain.model.Alarm
import com.timilehinaregbesola.mathalarm.interactors.AlarmInteractor

class CancelAlarm(private val alarmInteractor: AlarmInteractor) {
    suspend operator fun invoke(alarm: Alarm) = alarmInteractor.cancel(alarm)
}
