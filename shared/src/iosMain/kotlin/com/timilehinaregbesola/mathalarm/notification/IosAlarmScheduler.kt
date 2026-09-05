package com.timilehinaregbesola.mathalarm.notification

import co.touchlab.kermit.Logger
import com.timilehinaregbesola.mathalarm.alarm.AlarmScheduleRequest
import com.timilehinaregbesola.mathalarm.alarm.AlarmSchedulerBridge
import com.timilehinaregbesola.mathalarm.domain.model.Alarm
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import platform.Foundation.NSCalendar
import platform.Foundation.NSCalendarUnitDay
import platform.Foundation.NSCalendarUnitHour
import platform.Foundation.NSCalendarUnitMinute
import platform.Foundation.NSCalendarUnitMonth
import platform.Foundation.NSCalendarUnitYear
import platform.Foundation.NSDate
import platform.Foundation.NSDateComponents
import platform.Foundation.dateWithTimeIntervalSince1970
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionBadge
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNCalendarNotificationTrigger
import platform.UserNotifications.UNMutableNotificationContent
import platform.UserNotifications.UNNotificationAction
import platform.UserNotifications.UNNotificationActionOptionDestructive
import platform.UserNotifications.UNNotificationActionOptionNone
import platform.UserNotifications.UNNotificationCategory
import platform.UserNotifications.UNNotificationCategoryOptionNone
import platform.UserNotifications.UNNotificationRequest
import platform.UserNotifications.UNNotificationSound
import platform.UserNotifications.UNUserNotificationCenter
import kotlin.coroutines.resume
import kotlin.coroutines.suspendCoroutine
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * iOS Alarm Scheduler - Hybrid Implementation
 * 
 * Uses AlarmKit on iOS 26+ for native alarm experience,
 * falls back to UNUserNotificationCenter on iOS 15-25.
 * 
 * Following Alkaa's patterns:
 * - Registers notification categories when the notification center is first used
 * - Uses NSDate for cleaner time conversion
 * - Separates notification display concerns to IosAlarmNotification
 */
@OptIn(ExperimentalForeignApi::class, ExperimentalTime::class)
class IosAlarmScheduler(
    private val logger: Logger
) {
    private val notificationCenter by lazy {
        UNUserNotificationCenter.currentNotificationCenter().also(::registerNotificationCategories)
    }
    
    /**
     * Register notification categories with Snooze and Dismiss action buttons.
     * This enables interactive buttons on the alarm notification.
     */
    private fun registerNotificationCategories(notificationCenter: UNUserNotificationCenter) {
        logger.d { "Registering notification categories with actions" }
        
        val snoozeAction = UNNotificationAction.actionWithIdentifier(
            identifier = IosNotificationConstants.ACTION_IDENTIFIER_SNOOZE,
            title = "Snooze",
            options = UNNotificationActionOptionNone
        )
        
        val dismissAction = UNNotificationAction.actionWithIdentifier(
            identifier = IosNotificationConstants.ACTION_IDENTIFIER_DISMISS,
            title = "Dismiss",
            options = UNNotificationActionOptionDestructive
        )
        
        val alarmCategory = UNNotificationCategory.categoryWithIdentifier(
            identifier = IosNotificationConstants.CATEGORY_IDENTIFIER_ALARM,
            actions = listOf(snoozeAction, dismissAction),
            intentIdentifiers = emptyList<String>(),
            options = UNNotificationCategoryOptionNone
        )
        
        notificationCenter.setNotificationCategories(setOf(alarmCategory))
        logger.d { "Notification categories registered successfully" }
    }
    
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
    
    /** Schedule exactly this occurrence. Snoozes have their own stable identity. */
    suspend fun scheduleOccurrence(alarm: Alarm, timeInMillis: Long, repeating: Boolean = false, snooze: Boolean = false) {
        val local = Instant.fromEpochMilliseconds(timeInMillis).toLocalDateTime(TimeZone.currentSystemDefault())
        val day = local.dayOfWeek.ordinal.let { (it + 1) % 7 } // shared Sunday-first convention
        val key = if (snooze) "snooze" else "day_$day"
        val days = "FFFFFFF".toCharArray().apply { this[day] = 'T' }.concatToString()
        if (AlarmSchedulerBridge.isAlarmKitAvailable()) {
            val result = AlarmSchedulerBridge.scheduleWithAlarmKit(AlarmScheduleRequest(
                alarmId = alarm.alarmId, hour = alarm.hour, minute = alarm.minute,
                title = alarm.title, soundName = alarm.alarmTone, repeatDays = days,
                snoozeMinutes = alarm.snooze, vibrate = alarm.vibrate, difficulty = alarm.difficulty,
                repeats = repeating, timeInMillis = timeInMillis, occurrenceKey = key))
            check(result.success) { result.errorMessage ?: "Unable to schedule alarm with AlarmKit" }
            return
        }
        val allowed = suspendCoroutine<Boolean> { continuation ->
            notificationCenter.getNotificationSettingsWithCompletionHandler { settings ->
                continuation.resume(settings?.soundSetting == platform.UserNotifications.UNNotificationSettingEnabled)
            }
        }
        check(allowed) { "Enable notification sounds for Math Alarm in Settings" }
        val components = if (repeating) NSDateComponents().apply {
            hour = alarm.hour.toLong()
            minute = alarm.minute.toLong()
            second = 0
            weekday = (day + 1).toLong()
        } else {
            val date = NSDate.dateWithTimeIntervalSince1970(timeInMillis / 1000.0)
            NSCalendar.currentCalendar.components(NSCalendarUnitYear or NSCalendarUnitMonth or NSCalendarUnitDay or
                NSCalendarUnitHour or NSCalendarUnitMinute or platform.Foundation.NSCalendarUnitSecond, fromDate = date)
        }
        val request = UNNotificationRequest.requestWithIdentifier(
            "alarm_${alarm.alarmId}_$key", createNotificationContent(alarm),
            UNCalendarNotificationTrigger.triggerWithDateMatchingComponents(components, repeats = repeating))
        val error = suspendCoroutine<String?> { continuation ->
            notificationCenter.addNotificationRequest(request) { error ->
                continuation.resume(error?.localizedDescription)
            }
        }
        check(error == null) { error ?: "Unable to schedule notification" }
    }

    fun cancelSnooze(alarm: Alarm) {
        AlarmSchedulerBridge.cancelOccurrence(alarm.alarmId, "snooze")
        notificationCenter.removePendingNotificationRequestsWithIdentifiers(listOf("alarm_${alarm.alarmId}_snooze"))
    }

    /**
     * Create notification content for an alarm.
     * Uses constants from IosNotificationConstants for consistency.
     * 
     * Note: Critical alerts require special Apple entitlement (not available to most apps).
     * Instead, we use:
     * - TimeSensitive interruption level (breaks through Focus modes)
     * - Default sound for notification
     * - AlarmAudioController plays full alarm sound when notification arrives/is tapped
     */
    private fun createNotificationContent(alarm: Alarm): UNMutableNotificationContent {
        return UNMutableNotificationContent().apply {
            val alarmTitle = alarm.title.trim()
            if (alarmTitle.isNotEmpty()) {
                setTitle(alarmTitle)
            }
            setBody("Time to wake up! Solve the math problem to dismiss.")
            
            // iOS system alert sounds need a local filename with an extension.
            // Use the bundled CAF versions for system delivery; the original tone
            // id remains in userInfo for in-app looping playback.
            setSound(UNNotificationSound.soundNamed(notificationSoundName(alarm.alarmTone)))
            
            // Set relevance score to maximum for alarm notifications
            setRelevanceScore(1.0)
            
            // Use TimeSensitive interruption level (iOS 15+)
            // This breaks through Focus modes without requiring critical alerts entitlement
            setInterruptionLevel(platform.UserNotifications.UNNotificationInterruptionLevel.UNNotificationInterruptionLevelTimeSensitive)
            
            // Add alarm data to userInfo using constants for key names
            // This data is used by NotificationActionDelegate and for navigating to MathScreen
            setUserInfo(mapOf(
                IosNotificationConstants.USER_INFO_ALARM_ID to alarm.alarmId,
                IosNotificationConstants.USER_INFO_HOUR to alarm.hour,
                IosNotificationConstants.USER_INFO_MINUTE to alarm.minute,
                IosNotificationConstants.USER_INFO_REPEAT to alarm.repeat,
                IosNotificationConstants.USER_INFO_REPEAT_DAYS to alarm.repeatDays,
                IosNotificationConstants.USER_INFO_DIFFICULTY to alarm.difficulty,
                IosNotificationConstants.USER_INFO_SNOOZE to alarm.snooze,
                IosNotificationConstants.USER_INFO_VIBRATE to alarm.vibrate,
                IosNotificationConstants.USER_INFO_ALARM_TONE to alarm.alarmTone,
                IosNotificationConstants.USER_INFO_TITLE to alarm.title,
                IosNotificationConstants.USER_INFO_IS_ON to alarm.isOn
            ))
            
            // Set category for action buttons; registration occurs on first notification-center use.
            setCategoryIdentifier(IosNotificationConstants.CATEGORY_IDENTIFIER_ALARM)
        }
    }

    private fun notificationSoundName(alarmTone: String): String {
        val resourceName = alarmTone
            .ifEmpty { "alarm_classic" }
            .substringBeforeLast(".", alarmTone.ifEmpty { "alarm_classic" })

        return "$resourceName.caf"
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
        val identifiers = notificationIdentifiersForAlarm(alarm.alarmId)
        
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
        AlarmSchedulerBridge.cancelAllAlarms()
        
        // Cancel all notification-based alarms
        notificationCenter.removeAllPendingNotificationRequests()
        notificationCenter.removeAllDeliveredNotifications()
    }

    suspend fun hasPendingOccurrence(alarm: Alarm): Boolean {
        if (AlarmSchedulerBridge.isAlarmKitAvailable()) {
            return AlarmSchedulerBridge.hasPendingOccurrence(alarm.alarmId)
        }

        val identifiers = notificationIdentifiersForAlarm(alarm.alarmId).toSet()
        return suspendCoroutine { continuation ->
            notificationCenter.getPendingNotificationRequestsWithCompletionHandler { requests ->
                val hasPending = requests?.any { request ->
                    (request as? UNNotificationRequest)?.identifier in identifiers
                } == true
                continuation.resume(hasPending)
            }
        }
    }

    private fun notificationIdentifiersForAlarm(alarmId: Long): List<String> {
        val identifiers = mutableListOf("alarm_$alarmId", "alarm_${alarmId}_snooze")
        for (i in 0..6) {
            identifiers.add("alarm_${alarmId}_day_$i")
        }
        return identifiers
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
