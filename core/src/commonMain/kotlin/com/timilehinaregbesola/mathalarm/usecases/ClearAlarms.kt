package com.timilehinaregbesola.mathalarm.usecases

import com.timilehinaregbesola.mathalarm.data.AlarmRepository
import com.timilehinaregbesola.mathalarm.domain.model.Alarm

@OptIn(kotlin.experimental.ExperimentalObjCRefinement::class)
@kotlin.native.HiddenFromObjC
class ClearAlarms(
    private val alarmRepository: AlarmRepository,
    private val deleteAlarm: DeleteAlarm
) {
    suspend operator fun invoke(alarms: List<Alarm>) {
        for (alarm in alarms) deleteAlarm(alarm.alarmId)
        alarmRepository.clear()
    }
}
