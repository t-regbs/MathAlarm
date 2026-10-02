package com.timilehinaregbesola.mathalarm.usecases

import com.timilehinaregbesola.mathalarm.data.AlarmRepository

@OptIn(kotlin.experimental.ExperimentalObjCRefinement::class)
@kotlin.native.HiddenFromObjC
class FindAlarm(private val alarmRepository: AlarmRepository) {
    suspend operator fun invoke(id: Long) = alarmRepository.findAlarm(id)
}
