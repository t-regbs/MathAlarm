package com.timilehinaregbesola.mathalarm.framework

import com.timilehinaregbesola.mathalarm.usecases.AddAlarm
import com.timilehinaregbesola.mathalarm.usecases.CancelAlarm
import com.timilehinaregbesola.mathalarm.usecases.ClearAlarms
import com.timilehinaregbesola.mathalarm.usecases.CompleteAlarm
import com.timilehinaregbesola.mathalarm.usecases.DeleteAlarm
import com.timilehinaregbesola.mathalarm.usecases.FindAlarm
import com.timilehinaregbesola.mathalarm.usecases.GetSavedAlarms
import com.timilehinaregbesola.mathalarm.usecases.RescheduleFutureAlarms
import com.timilehinaregbesola.mathalarm.usecases.ScheduleAlarm
import com.timilehinaregbesola.mathalarm.usecases.ScheduleNextAlarm
import com.timilehinaregbesola.mathalarm.usecases.ShowAlarm
import com.timilehinaregbesola.mathalarm.usecases.SnoozeAlarm
import com.timilehinaregbesola.mathalarm.usecases.UpdateAlarm

import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

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
    val cancelAlarm: CancelAlarm
) {
    private val commandMutex = Mutex()

    /** Serialize UI, receiver and recovery commands across database and OS scheduling. */
    suspend fun <T> command(block: suspend Usecases.() -> T): T = commandMutex.withLock { block() }
}
