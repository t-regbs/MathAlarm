package com.timilehinaregbesola.mathalarm.alarm

import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine

data class AlarmScheduleRequest(
    val alarmId: Long,
    val hour: Int,
    val minute: Int,
    val title: String,
    val soundName: String,
    val repeatDays: String,
    val vibrate: Boolean,
    val difficulty: Int,
    val repeats: Boolean,
    val timeInMillis: Long,
    val occurrenceKey: String
)

data class AlarmScheduleResult(
    val success: Boolean,
    val usedAlarmKit: Boolean,
    val errorMessage: String? = null
)

/** Completion is called only after AlarmKit has accepted or rejected the schedule. */
interface AlarmScheduleCompletion {
    fun complete(success: Boolean, error: String?)
}

interface AlarmAuthorizationCompletion {
    fun complete(authorized: Boolean)
}

interface NativeAlarmScheduler {
    fun scheduleAlarm(request: AlarmScheduleRequest, completion: AlarmScheduleCompletion)
    /** Null on success; a message on failure. Cancellation is synchronous in AlarmKit. */
    fun cancelAlarm(alarmId: Long): String?
    fun cancelOccurrence(alarmId: Long, occurrenceKey: String): String?
    fun isAlarmKitAvailable(): Boolean
    fun hasPendingOccurrence(alarmId: Long, occurrenceKey: String): Boolean
    fun authorizationStatus(): String
    fun requestAuthorization(completion: AlarmAuthorizationCompletion)
    fun acknowledgePendingHandoff(payload: String)
    fun hasPendingHandoff(): Boolean
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
    fun cancelAlarm(alarmId: Long) {
        nativeScheduler?.cancelAlarm(alarmId)?.let { throw IllegalStateException(it) }
    }
    fun cancelOccurrence(alarmId: Long, key: String) {
        nativeScheduler?.cancelOccurrence(alarmId, key)?.let { throw IllegalStateException(it) }
    }
    fun hasPendingOccurrence(alarmId: Long, occurrenceKey: String): Boolean =
        nativeScheduler?.hasPendingOccurrence(alarmId, occurrenceKey) == true
    fun authorizationStatus(): String = nativeScheduler?.authorizationStatus() ?: "unavailable"
    fun requestAuthorization(onResult: (Boolean) -> Unit) {
        val scheduler = nativeScheduler
        if (scheduler == null) {
            onResult(false)
            return
        }
        scheduler.requestAuthorization(object : AlarmAuthorizationCompletion {
            override fun complete(authorized: Boolean) = onResult(authorized)
        })
    }
    fun acknowledgePendingHandoff(payload: String) { nativeScheduler?.acknowledgePendingHandoff(payload) }
    fun hasPendingHandoff(): Boolean = nativeScheduler?.hasPendingHandoff() == true
    val shared: AlarmSchedulerBridge get() = this
}
