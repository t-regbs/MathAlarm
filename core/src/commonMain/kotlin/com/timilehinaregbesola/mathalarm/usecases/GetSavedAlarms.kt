package com.timilehinaregbesola.mathalarm.usecases

import com.timilehinaregbesola.mathalarm.data.AlarmRepository

@OptIn(kotlin.experimental.ExperimentalObjCRefinement::class)
@kotlin.native.HiddenFromObjC
class GetSavedAlarms(private val alarmRepository: AlarmRepository) {
    operator fun invoke() = alarmRepository.getSavedAlarms()
}
