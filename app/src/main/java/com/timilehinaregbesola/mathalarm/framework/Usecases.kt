package com.timilehinaregbesola.mathalarm.framework

import com.timilehinaregbesola.mathalarm.usecases.alarm.*
import com.timilehinaregbesola.mathalarm.usecases.preferences.LoadAppTheme
import com.timilehinaregbesola.mathalarm.usecases.preferences.UpdateAppTheme

data class Usecases(
    val addAlarm: AddAlarm,
    val clearAlarms: ClearAlarms,
    val deleteAlarm: DeleteAlarm,
    val findAlarm: FindAlarm,
    val getSavedAlarms: GetSavedAlarms,
    val updateAlarm: UpdateAlarm,
    val scheduleAlarm: ScheduleAlarm,
    val completeAlarm: CompleteAlarm,
    val rescheduleFutureAlarms: RescheduleFutureAlarms,
    val scheduleNextAlarm: ScheduleNextAlarm,
    val showAlarm: ShowAlarm,
    val snoozeAlarm: SnoozeAlarm,
    val cancelAlarm: CancelAlarm,
    val loadAppTheme: LoadAppTheme,
    val updateAppTheme: UpdateAppTheme
)
