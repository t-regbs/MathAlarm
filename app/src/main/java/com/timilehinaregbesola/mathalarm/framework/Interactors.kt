package com.timilehinaregbesola.mathalarm.framework

import com.timilehinaregbesola.mathalarm.interactors.*

data class Interactors(
    val addAlarm: AddAlarm,
    val clearAlarms: ClearAlarms,
    val deleteAlarm: DeleteAlarm,
    val deleteAlarmWithId: DeleteAlarmWithId,
    val findAlarm: FindAlarm,
    val getAlarms: GetAlarms,
    val getLatestAlarm: GetLatestAlarm,
    val updateAlarm: UpdateAlarm
)
