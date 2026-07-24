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
 * - Registers notification categories with action buttons on init
 * - Uses NSDate for cleaner time conversion
 * - Separates notification display concerns to IosAlarmNotification
 */
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
     * Schedule a one-time alarm that fires at the next occurrence of the specified time.
     * Uses NSDate for cleaner time conversion following Alkaa's pattern.
     */
    @OptIn(ExperimentalForeignApi::class, ExperimentalTime::class)
    private fun scheduleOneTimeAlarm(alarm: Alarm): Boolean {
        val content = createNotificationContent(alarm)
        
        // Calculate the next occurrence time in milliseconds
        val timeInMillis = calculateNextAlarmTimeMillis(alarm.hour, alarm.minute)
        
        // Convert to NSDate and extract components (cleaner than manual NSDateComponents)
        val dateComponents = dateComponentsFromMillis(timeInMillis)
        
        val trigger = UNCalendarNotificationTrigger.triggerWithDateMatchingComponents(
            dateComponents = dateComponents,
            repeats = false
        )
        
        val request = UNNotificationRequest.requestWithIdentifier(
            identifier = "alarm_${alarm.alarmId}",
            content = content,
            trigger = trigger
        )
        
        logger.d { "Scheduling one-time alarm: id=${alarm.alarmId} at $timeInMillis" }
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
     * Convert milliseconds timestamp to NSDateComponents.
     * Following Alkaa's cleaner approach using NSDate.
     */
    @OptIn(ExperimentalForeignApi::class)
    private fun dateComponentsFromMillis(timeInMillis: Long): NSDateComponents {
        val nsDate = NSDate.dateWithTimeIntervalSince1970(timeInMillis / 1000.0)
        return NSCalendar.currentCalendar.components(
            NSCalendarUnitYear or NSCalendarUnitMonth or NSCalendarUnitDay
                or NSCalendarUnitHour or NSCalendarUnitMinute,
            fromDate = nsDate
        )
    }
    
    /**
     * Calculate the next occurrence of the given hour:minute in milliseconds.
     */
    @OptIn(ExperimentalTime::class)
    private fun calculateNextAlarmTimeMillis(hour: Int, minute: Int): Long {
        val now = Instant.fromEpochMilliseconds(kotlin.time.Clock.System.now().toEpochMilliseconds())
        val tz = TimeZone.currentSystemDefault()
        val localNow = now.toLocalDateTime(tz)
        val today = localNow.date
        
        // Create alarm time for today
        val alarmTimeToday = LocalDateTime(
            date = today,
            time = LocalTime(hour, minute, 0)
        )
        val alarmInstantToday = alarmTimeToday.toInstant(tz)
        
        // If alarm time has passed today, schedule for tomorrow
        return if (alarmInstantToday > now) {
            alarmInstantToday.toEpochMilliseconds()
        } else {
            val tomorrow = today.plus(DatePeriod(days = 1))
            val alarmTimeTomorrow = LocalDateTime(
                date = tomorrow,
                time = LocalTime(hour, minute, 0)
            )
            alarmTimeTomorrow.toInstant(tz).toEpochMilliseconds()
        }
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
            
            // Set category for action buttons (registered in init)
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
        if (AlarmSchedulerBridge.isAlarmKitAvailable()) {
            // Note: Swift side handles this
            logger.d { "Cancelling AlarmKit alarms..." }
        }
        
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
        val identifiers = mutableListOf("alarm_$alarmId")
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
