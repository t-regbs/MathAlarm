package com.timilehinaregbesola.mathalarm.alarm

import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

data class AlarmScheduleRequest(
    val alarmId: Long, val hour: Int, val minute: Int, val title: String,
    val soundName: String, val repeatDays: String, val snoozeMinutes: Int,
    val vibrate: Boolean, val difficulty: Int, val repeats: Boolean,
    val timeInMillis: Long, val occurrenceKey: String
)

data class AlarmScheduleResult(val success: Boolean, val usedAlarmKit: Boolean, val errorMessage: String? = null)

/** Completion is called only after AlarmKit has accepted or rejected the schedule. */
interface AlarmScheduleCompletion {
    fun complete(success: Boolean, error: String?)
}

interface NativeAlarmScheduler {
    fun scheduleAlarm(request: AlarmScheduleRequest, completion: AlarmScheduleCompletion)
    fun cancelAlarm(alarmId: Long)
    fun cancelOccurrence(alarmId: Long, occurrenceKey: String)
    fun cancelAllAlarms()
    fun isAlarmKitAvailable(): Boolean
    fun hasPendingOccurrence(alarmId: Long): Boolean
    fun snoozeAlarm(alarmId: Long, minutes: Int)
}

object AlarmSchedulerBridge {
    private var nativeScheduler: NativeAlarmScheduler? = null
    fun registerScheduler(scheduler: NativeAlarmScheduler) { nativeScheduler = scheduler }
    fun isAlarmKitAvailable(): Boolean = nativeScheduler?.isAlarmKitAvailable() == true
    suspend fun scheduleWithAlarmKit(request: AlarmScheduleRequest): AlarmScheduleResult {
        val scheduler = nativeScheduler ?: return AlarmScheduleResult(false, false)
        if (!scheduler.isAlarmKitAvailable()) return AlarmScheduleResult(false, false)
        return suspendCoroutine { continuation ->
            scheduler.scheduleAlarm(request, object : AlarmScheduleCompletion {
                override fun complete(success: Boolean, error: String?) {
                    continuation.resume(AlarmScheduleResult(success, true, error))
                }
            })
        }
    }
    fun cancelAlarm(alarmId: Long) { nativeScheduler?.cancelAlarm(alarmId) }
    fun cancelOccurrence(alarmId: Long, key: String) { nativeScheduler?.cancelOccurrence(alarmId, key) }
    fun cancelAllAlarms() { nativeScheduler?.cancelAllAlarms() }
    fun hasPendingOccurrence(alarmId: Long): Boolean = nativeScheduler?.hasPendingOccurrence(alarmId) == true
    fun snoozeAlarm(alarmId: Long, minutes: Int) { nativeScheduler?.snoozeAlarm(alarmId, minutes) }
    val shared: AlarmSchedulerBridge get() = this
}
