package com.timilehinaregbesola.mathalarm.alarm

import co.touchlab.kermit.Logger

/**
 * Data class representing alarm scheduling parameters
 * This is exported to Swift via Swift Export
 */
data class AlarmScheduleRequest(
    val alarmId: Long,
    val hour: Int,
    val minute: Int,
    val title: String,
    val soundName: String,
    val repeatDays: String,
    val snoozeMinutes: Int,
    val vibrate: Boolean,
    val difficulty: Int,
    val repeats: Boolean  // true = repeating alarm, false = one-time alarm
)

/**
 * Result of an alarm scheduling operation
 */
data class AlarmScheduleResult(
    val success: Boolean,
    val usedAlarmKit: Boolean,
    val errorMessage: String? = null
)

/**
 * Interface that Swift implements to provide native AlarmKit functionality.
 * 
 * This interface is exported to Swift via Swift Export, allowing Swift code
 * to implement it and provide AlarmKit capabilities when available (iOS 26+).
 * 
 * On iOS < 26, the Swift implementation returns false for isAlarmKitAvailable(),
 * and Kotlin falls back to UNUserNotificationCenter-based alarms.
 */
interface NativeAlarmScheduler {
    /**
     * Schedule an alarm using AlarmKit (iOS 26+)
     * @return true if successfully scheduled with AlarmKit, false to use fallback
     */
    fun scheduleAlarm(request: AlarmScheduleRequest): Boolean
    
    /**
     * Cancel an alarm scheduled with AlarmKit
     */
    fun cancelAlarm(alarmId: Long)
    
    /**
     * Cancel all alarms scheduled with AlarmKit
     */
    fun cancelAllAlarms()
    
    /**
     * Check if AlarmKit is available on this device (iOS 26+)
     */
    fun isAlarmKitAvailable(): Boolean

    /**
     * Returns true if a future occurrence for this alarm is still pending in AlarmKit.
     */
    fun hasPendingOccurrence(alarmId: Long): Boolean
    
    /**
     * Snooze an active alarm
     * @param alarmId The alarm to snooze
     * @param minutes How long to snooze
     */
    fun snoozeAlarm(alarmId: Long, minutes: Int)
}

/**
 * Bridge object that holds the Swift-provided NativeAlarmScheduler implementation.
 * 
 * Swift code registers its implementation at app startup:
 * ```swift
 * AlarmSchedulerBridge.shared.registerScheduler(myAlarmKitWrapper)
 * ```
 * 
 * Kotlin code then uses it:
 * ```kotlin
 * if (AlarmSchedulerBridge.isAlarmKitAvailable()) {
 *     AlarmSchedulerBridge.scheduleWithAlarmKit(request)
 * }
 * ```
 */
object AlarmSchedulerBridge {
    private val logger = Logger.withTag("AlarmSchedulerBridge")
    
    /**
     * The Swift-provided scheduler implementation
     * Set by Swift at app initialization
     */
    private var nativeScheduler: NativeAlarmScheduler? = null
    
    /**
     * Register the Swift AlarmKit implementation
     * Called from Swift at app startup
     */
    fun registerScheduler(scheduler: NativeAlarmScheduler) {
        logger.d { "Registering native alarm scheduler" }
        nativeScheduler = scheduler
    }
    
    /**
     * Check if AlarmKit is available
     */
    fun isAlarmKitAvailable(): Boolean {
        return nativeScheduler?.isAlarmKitAvailable() == true
    }
    
    /**
     * Schedule an alarm using AlarmKit if available
     * @return AlarmScheduleResult with success status and whether AlarmKit was used
     */
    fun scheduleWithAlarmKit(request: AlarmScheduleRequest): AlarmScheduleResult {
        val scheduler = nativeScheduler
        
        if (scheduler == null) {
            logger.w { "No native scheduler registered" }
            return AlarmScheduleResult(
                success = false,
                usedAlarmKit = false,
                errorMessage = "Native scheduler not registered"
            )
        }
        
        if (!scheduler.isAlarmKitAvailable()) {
            logger.d { "AlarmKit not available, will use fallback" }
            return AlarmScheduleResult(
                success = false,
                usedAlarmKit = false
            )
        }
        
        return try {
            val success = scheduler.scheduleAlarm(request)
            logger.d { "AlarmKit schedule result: $success for alarm ${request.alarmId}" }
            AlarmScheduleResult(
                success = success,
                usedAlarmKit = true
            )
        } catch (e: Exception) {
            logger.e(e) { "Error scheduling with AlarmKit" }
            AlarmScheduleResult(
                success = false,
                usedAlarmKit = false,
                errorMessage = e.message
            )
        }
    }
    
    /**
     * Cancel an alarm
     */
    fun cancelAlarm(alarmId: Long) {
        nativeScheduler?.cancelAlarm(alarmId)
    }

    fun hasPendingOccurrence(alarmId: Long): Boolean {
        return nativeScheduler?.hasPendingOccurrence(alarmId) == true
    }
    
    /**
     * Snooze an alarm
     */
    fun snoozeAlarm(alarmId: Long, minutes: Int) {
        nativeScheduler?.snoozeAlarm(alarmId, minutes)
    }
    
    /**
     * For Swift access - the shared instance
     */
    val shared: AlarmSchedulerBridge get() = this
}
