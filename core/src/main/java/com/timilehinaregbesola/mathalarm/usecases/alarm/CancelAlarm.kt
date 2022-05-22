package com.timilehinaregbesola.mathalarm.usecases.alarm

import com.timilehinaregbesola.mathalarm.domain.model.Alarm
import com.timilehinaregbesola.mathalarm.interactors.AlarmInteractor

class CancelAlarm(private val alarmInteractor: AlarmInteractor) {
    operator fun invoke(alarm: Alarm) = alarmInteractor.cancel(alarm)
}
