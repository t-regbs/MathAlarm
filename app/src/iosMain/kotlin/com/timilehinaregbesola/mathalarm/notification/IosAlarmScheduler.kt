package com.timilehinaregbesola.mathalarm.notification

import co.touchlab.kermit.Logger
import com.timilehinaregbesola.mathalarm.alarm.AlarmScheduleRequest
import com.timilehinaregbesola.mathalarm.alarm.AlarmSchedulerBridge
import com.timilehinaregbesola.mathalarm.domain.model.Alarm
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.datetime.Clock
import kotlinx.datetime.DateTimeUnit
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toLocalDateTime
import platform.Foundation.NSCalendar
import platform.Foundation.NSCalendarUnitDay
import platform.Foundation.NSCalendarUnitHour
import platform.Foundation.NSCalendarUnitMinute
import platform.Foundation.NSCalendarUnitMonth
import platform.Foundation.NSCalendarUnitSecond
import platform.Foundation.NSCalendarUnitWeekday
import platform.Foundation.NSCalendarUnitYear
import platform.Foundation.NSDateComponents
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionBadge
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNCalendarNotificationTrigger
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNNotificationSound
import platform.UserNotifications.UNUserNotificationCenter
import kotlin.time.ExperimentalTime

/**
 * iOS Alarm Scheduler - Hybrid Implementation
 * 
 * Uses AlarmKit on iOS 26+ for native alarm experience,
 * falls back to UNUserNotificationCenter on iOS 15-25.
 */
class IosAlarmScheduler(
    private val logger: Logger
) {
    private val notificationCenter = UNUserNotificationCenter.currentNotificationCenter()
    
    /**
     * Check if AlarmKit is available (iOS 26+)
     */
    fun isAlarmKitAvailable(): Boolean {
        return AlarmSchedulerBridge.isAlarmKitAvailable()
    }
    
    /**
     * Request notification permissions from the user
     */
    fun requestPermissions(onResult: (Boolean) -> Unit) {
        notificationCenter.requestAuthorizationWithOptions(
            options = UNAuthorizationOptionAlert or UNAuthorizationOptionSound or UNAuthorizationOptionBadge
        ) { granted, error ->
            if (error != null) {
                logger.e { "Permission request error: ${error.localizedDescription}" }
            }
            onResult(granted)
        }
    }
    
    /**
     * Schedule an alarm - uses AlarmKit on iOS 26+, notifications on older iOS
     * 
     * @param alarm The alarm to schedule
     * @param reschedule Whether this is a reschedule (after alarm fired)
     * @return true if scheduling was initiated successfully
     */
    @OptIn(ExperimentalTime::class)
    fun scheduleAlarm(alarm: Alarm, reschedule: Boolean = false): Boolean {
        logger.d { "Scheduling alarm: id=${alarm.alarmId}, time=${alarm.hour}:${alarm.minute}, repeat=${alarm.repeat}" }
        
        // First, cancel any existing alarms
        cancelAlarm(alarm)
        
        // Try AlarmKit first (iOS 26+)
        if (tryScheduleWithAlarmKit(alarm)) {
            logger.d { "Alarm scheduled with AlarmKit: ${alarm.alarmId}" }
            return true
        }
        
        // Fallback to notifications (iOS 15-25)
        logger.d { "Using notification fallback for alarm: ${alarm.alarmId}" }
        return scheduleWithNotifications(alarm)
    }
    
    /**
     * Try to schedule alarm with AlarmKit (iOS 26+)
     * @return true if successfully scheduled, false to use fallback
     */
    private fun tryScheduleWithAlarmKit(alarm: Alarm): Boolean {
        logger.d { "tryScheduleWithAlarmKit: Checking AlarmKit availability..." }
        
        if (!AlarmSchedulerBridge.isAlarmKitAvailable()) {
            logger.d { "AlarmKit not available, will use notifications" }
            return false
        }
        
        logger.d { "AlarmKit is available, creating request..." }
        
        val request = AlarmScheduleRequest(
            alarmId = alarm.alarmId,
            hour = alarm.hour,
            minute = alarm.minute,
            title = alarm.title,
            soundName = alarm.alarmTone,
            repeatDays = alarm.repeatDays,
            snoozeMinutes = alarm.snooze,
            vibrate = alarm.vibrate,
            difficulty = alarm.difficulty,
            repeats = alarm.repeat  // Pass the repeat flag to Swift
        )
        
        logger.d { "AlarmScheduleRequest created: id=${request.alarmId}, time=${request.hour}:${request.minute}, repeats=${request.repeats}, repeatDays=${request.repeatDays}" }
        
        val result = AlarmSchedulerBridge.scheduleWithAlarmKit(request)
        
        logger.d { "AlarmKit schedule result: success=${result.success}, usedAlarmKit=${result.usedAlarmKit}, error=${result.errorMessage}" }
        
        if (result.usedAlarmKit && result.success) {
            logger.i { "✅ Alarm ${alarm.alarmId} scheduled successfully with AlarmKit" }
            return true
        }
        
        if (result.errorMessage != null) {
            logger.w { "AlarmKit error: ${result.errorMessage}" }
        }
        
        logger.d { "AlarmKit scheduling failed, will fall back to notifications" }
        return false
    }
    
    /**
     * Schedule alarm using notifications (fallback for iOS < 26)
     */
    private fun scheduleWithNotifications(alarm: Alarm): Boolean {
        return if (alarm.repeat && alarm.repeatDays.contains('T')) {
            scheduleRepeatingAlarm(alarm)
        } else {
            scheduleOneTimeAlarm(alarm)
        }
    }
    
    /**
     * Schedule a one-time alarm that fires at the next occurrence of the specified time
     */
    @OptIn(ExperimentalForeignApi::class, ExperimentalTime::class)
    private fun scheduleOneTimeAlarm(alarm: Alarm): Boolean {
        val content = createNotificationContent(alarm)
        
        // Create date components for the alarm time
        val dateComponents = NSDateComponents().apply {
            hour = alarm.hour.toLong()
            minute = alarm.minute.toLong()
            second = 0
        }
        
        val trigger = UNCalendarNotificationTrigger.triggerWithDateMatchingComponents(
            dateComponents = dateComponents,
            repeats = false
        )
        
        val request = UNNotificationRequest.requestWithIdentifier(
            identifier = "alarm_${alarm.alarmId}",
            content = content,
            trigger = trigger
        )
        
        notificationCenter.addNotificationRequest(request) { error ->
            if (error != null) {
                logger.e { "Failed to schedule one-time alarm: ${error.localizedDescription}" }
            } else {
                logger.d { "One-time alarm scheduled: id=${alarm.alarmId}" }
            }
        }
        
        return true
    }
    
    /**
     * Schedule a repeating alarm for specific days of the week
     */
    @OptIn(ExperimentalForeignApi::class)
    private fun scheduleRepeatingAlarm(alarm: Alarm): Boolean {
        val content = createNotificationContent(alarm)
        val repeatDays = alarm.repeatDays
        
        // iOS weekday: 1 = Sunday, 2 = Monday, ..., 7 = Saturday
        // repeatDays string: index 0 = Monday, 1 = Tuesday, ..., 6 = Sunday
        val dayMapping = mapOf(
            0 to 2L, // Monday -> iOS weekday 2
            1 to 3L, // Tuesday -> iOS weekday 3
            2 to 4L, // Wednesday -> iOS weekday 4
            3 to 5L, // Thursday -> iOS weekday 5
            4 to 6L, // Friday -> iOS weekday 6
            5 to 7L, // Saturday -> iOS weekday 7
            6 to 1L  // Sunday -> iOS weekday 1
        )
        
        repeatDays.forEachIndexed { index, char ->
            if (char == 'T') {
                val iosWeekday = dayMapping[index] ?: return@forEachIndexed
                
                val dateComponents = NSDateComponents().apply {
                    hour = alarm.hour.toLong()
                    minute = alarm.minute.toLong()
                    second = 0
                    weekday = iosWeekday
                }
                
                val trigger = UNCalendarNotificationTrigger.triggerWithDateMatchingComponents(
                    dateComponents = dateComponents,
                    repeats = true
                )
                
                val request = UNNotificationRequest.requestWithIdentifier(
                    identifier = "alarm_${alarm.alarmId}_day_$index",
                    content = content,
                    trigger = trigger
                )
                
                notificationCenter.addNotificationRequest(request) { error ->
                    if (error != null) {
                        logger.e { "Failed to schedule repeating alarm for day $index: ${error.localizedDescription}" }
                    } else {
                        logger.d { "Repeating alarm scheduled: id=${alarm.alarmId}, day=$index" }
                    }
                }
            }
        }
        
        return true
    }
    
    /**
     * Create notification content for an alarm
     */
    private fun createNotificationContent(alarm: Alarm): UNMutableNotificationContent {
        return UNMutableNotificationContent().apply {
            setTitle("Math Alarm")
            setBody(alarm.title.ifEmpty { "Time to wake up! Solve the math problem to dismiss." })
            
            // Use critical sound with max volume for alarm
            // This bypasses Do Not Disturb and silent mode
            // Note: Critical alerts require special entitlement from Apple for App Store
            // For development, we use the default critical sound
            val alarmSound = if (alarm.alarmTone.isNotEmpty()) {
                // Try to use custom sound file if specified
                UNNotificationSound.criticalSoundNamed(alarm.alarmTone, withAudioVolume = 1.0f)
            } else {
                // Use default critical sound at max volume
                UNNotificationSound.defaultCriticalSoundWithAudioVolume(1.0f)
            }
            setSound(alarmSound)
            
            // Set relevance score to maximum for alarm notifications
            setRelevanceScore(1.0)
            
            // Mark as interruptive (iOS 15+)
            setInterruptionLevel(platform.UserNotifications.UNNotificationInterruptionLevel.UNNotificationInterruptionLevelCritical)
            
            // Add ALL alarm data to userInfo for handling when notification is tapped
            // This data is used to navigate to the MathScreen with the alarm info
            setUserInfo(mapOf(
                "alarmId" to alarm.alarmId,
                "hour" to alarm.hour,
                "minute" to alarm.minute,
                "repeat" to alarm.repeat,
                "repeatDays" to alarm.repeatDays,
                "difficulty" to alarm.difficulty,
                "snooze" to alarm.snooze,
                "vibrate" to alarm.vibrate,
                "alarmTone" to alarm.alarmTone,
                "title" to alarm.title,
                "isOn" to alarm.isOn
            ))
            
            // Set category for action buttons
            setCategoryIdentifier("ALARM_CATEGORY")
        }
    }
    
    /**
     * Cancel all notifications/alarms for an alarm
     * Cancels both AlarmKit alarms and notification-based alarms
     */
    fun cancelAlarm(alarm: Alarm) {
        logger.d { "Cancelling alarm: id=${alarm.alarmId}" }
        
        // Cancel AlarmKit alarm if available
        AlarmSchedulerBridge.cancelAlarm(alarm.alarmId)
        
        // Also cancel notification-based alarm (in case of migration or fallback)
        val identifiers = mutableListOf("alarm_${alarm.alarmId}")
        
        // Also cancel any day-specific notifications for repeating alarms
        for (i in 0..6) {
            identifiers.add("alarm_${alarm.alarmId}_day_$i")
        }
        
        notificationCenter.removePendingNotificationRequestsWithIdentifiers(identifiers)
        notificationCenter.removeDeliveredNotificationsWithIdentifiers(identifiers)
        
        logger.d { "Alarm cancelled: id=${alarm.alarmId}" }
    }
    
    /**
     * Cancel all pending alarms
     * Cancels both AlarmKit and notification-based alarms
     */
    fun cancelAllAlarms() {
        logger.d { "Cancelling all alarms" }
        
        // Cancel all AlarmKit alarms
        if (AlarmSchedulerBridge.isAlarmKitAvailable()) {
            // Note: Swift side handles this
            logger.d { "Cancelling AlarmKit alarms..." }
        }
        
        // Cancel all notification-based alarms
        notificationCenter.removeAllPendingNotificationRequests()
        notificationCenter.removeAllDeliveredNotifications()
    }
    
    /**
     * Snooze an alarm
     */
    fun snoozeAlarm(alarmId: Long, minutes: Int) {
        logger.d { "Snoozing alarm $alarmId for $minutes minutes" }
        AlarmSchedulerBridge.snoozeAlarm(alarmId, minutes)
    }
    
    /**
     * Get the count of pending notifications
     */
    fun getPendingNotificationCount(callback: (Int) -> Unit) {
        notificationCenter.getPendingNotificationRequestsWithCompletionHandler { requests ->
            callback(requests?.size ?: 0)
        }
    }
}
