package com.timilehinaregbesola.mathalarm.framework

import com.timilehinaregbesola.mathalarm.usecases.*

data class Usecases(
    val addAlarm: AddAlarm,
    val clearAlarms: ClearAlarms,
    val deleteAlarm: DeleteAlarm,
    val deleteAlarmWithId: DeleteAlarmWithId,
    val findAlarm: FindAlarm,
    val getAlarms: GetAlarms,
    val getSavedAlarms: GetSavedAlarms,
    val updateAlarm: UpdateAlarm,
    val scheduleAlarm: ScheduleAlarm,
    val completeAlarm: CompleteAlarm,
    val rescheduleFutureAlarms: RescheduleFutureAlarms,
    val scheduleNextAlarm: ScheduleNextAlarm,
    val showAlarm: ShowAlarm,
    val snoozeAlarm: SnoozeAlarm
)
