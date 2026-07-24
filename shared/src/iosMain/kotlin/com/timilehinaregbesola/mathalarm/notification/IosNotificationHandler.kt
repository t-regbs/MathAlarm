package com.timilehinaregbesola.mathalarm.notification

import com.timilehinaregbesola.mathalarm.framework.database.AlarmEntity
import com.timilehinaregbesola.mathalarm.interactors.IosAlarmAudioManager
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import platform.UserNotifications.UNNotification
import platform.UserNotifications.UNNotificationResponse
import platform.UserNotifications.UNUserNotificationCenter
import platform.UserNotifications.UNUserNotificationCenterDelegateProtocol
import platform.darwin.NSObject

/**
 * Singleton to hold the deeplink information when a notification is tapped
 * Accessible from both Kotlin and Swift
 */
object NotificationDeeplinkHolder {
    private val _deeplinkInfo = MutableStateFlow<String?>(null)
    val deeplinkInfo: StateFlow<String?> = _deeplinkInfo.asStateFlow()
    
    // Function with named parameter for Swift compatibility
    fun setDeeplink(alarmJson: String?) {
        println("NotificationDeeplinkHolder: Setting deeplink = $alarmJson")
        _deeplinkInfo.value = alarmJson
    }
    
    // Alternative function name for easier Swift access
    fun setAlarmDeeplink(json: String) {
        println("NotificationDeeplinkHolder: setAlarmDeeplink called with json = $json")
        _deeplinkInfo.value = json
    }
    
    fun clearDeeplink() {
        println("NotificationDeeplinkHolder: Clearing deeplink")
        _deeplinkInfo.value = null
    }
    
    // For Swift access - the shared instance
    val shared: NotificationDeeplinkHolder get() = this
}

/**
 * Singleton to manage alarm audio from Swift
 * Wraps IosAlarmAudioManager for Swift interop
 */
object AlarmAudioController {
    val shared: AlarmAudioController get() = this
    
    /**
     * Start playing the alarm
     */
    fun startAlarm(soundName: String, vibrate: Boolean) {
        println("AlarmAudioController: Starting alarm - sound=$soundName, vibrate=$vibrate")
        IosAlarmAudioManager.startAlarm(soundName, vibrate, 1.0f)
    }
    
    /**
     * Stop the alarm
     */
    fun stopAlarm() {
        println("AlarmAudioController: Stopping alarm")
        IosAlarmAudioManager.stopAlarm()
    }
    
    /**
     * Check if alarm is currently playing
     */
    fun isPlaying(): Boolean {
        return IosAlarmAudioManager.isPlaying()
    }
    
    /**
     * Snooze the alarm
     */
    fun snoozeAlarm(minutes: Int) {
        println("AlarmAudioController: Snoozing alarm for $minutes minutes")
        IosAlarmAudioManager.snooze(minutes)
    }
}

// Action identifiers for notification actions
private const val ACTION_SNOOZE = "SNOOZE_ACTION"
private const val ACTION_DISMISS = "DISMISS_ACTION"
private const val ACTION_SOLVE = "SOLVE_ACTION"

/**
 * iOS Notification Delegate to handle notification interactions
 */
class IosNotificationDelegate : NSObject(), UNUserNotificationCenterDelegateProtocol {
    
    /**
     * Called when user taps on a notification or selects an action
     */
    override fun userNotificationCenter(
        center: UNUserNotificationCenter,
        didReceiveNotificationResponse: UNNotificationResponse,
        withCompletionHandler: () -> Unit
    ) {
        val userInfo = didReceiveNotificationResponse.notification.request.content.userInfo
        val actionIdentifier = didReceiveNotificationResponse.actionIdentifier
        
        // Extract alarm data from userInfo
        val alarmId = (userInfo["alarmId"] as? Number)?.toLong() ?: 0L
        val hour = (userInfo["hour"] as? Number)?.toInt() ?: 0
        val minute = (userInfo["minute"] as? Number)?.toInt() ?: 0
        val difficulty = (userInfo["difficulty"] as? Number)?.toInt() ?: 0
        val snooze = (userInfo["snooze"] as? Number)?.toInt() ?: 5
        val vibrate = (userInfo["vibrate"] as? Boolean) ?: false
        val title = (userInfo["title"] as? String) ?: ""
        val alarmTone = (userInfo["alarmTone"] as? String) ?: ""
        
        when (actionIdentifier) {
            ACTION_SNOOZE -> {
                // Handle snooze - reschedule alarm for snooze minutes later
                // This would need to be handled by scheduling a new notification
            }
            ACTION_DISMISS -> {
                // Just dismiss the notification - no action needed
                NotificationDeeplinkHolder.clearDeeplink()
            }
            ACTION_SOLVE, 
            "com.apple.UNNotificationDefaultActionIdentifier" -> {
                // Default tap or "Solve Math" action - navigate to math screen
                val alarmEntity = AlarmEntity(
                    alarmId = alarmId,
                    hour = hour,
                    minute = minute,
                    repeat = false,
                    repeatDays = "FFFFFFF",
                    isOn = true,
                    difficulty = difficulty,
                    alarmTone = alarmTone,
                    vibrate = vibrate,
                    snooze = snooze,
                    title = title,
                    isSaved = true
                )
                
                // Convert to JSON for the NavGraph
                val alarmJson = Json.encodeToString(alarmEntity)
                NotificationDeeplinkHolder.setDeeplink(alarmJson)
            }
        }
        
        withCompletionHandler()
    }
    
    /**
     * Called when notification arrives while app is in foreground
     * Allows showing the notification banner even when app is open
     */
    override fun userNotificationCenter(
        center: UNUserNotificationCenter,
        willPresentNotification: UNNotification,
        withCompletionHandler: (platform.UserNotifications.UNNotificationPresentationOptions) -> Unit
    ) {
        // Show banner, play sound, and show badge even when app is in foreground
        withCompletionHandler(
            platform.UserNotifications.UNNotificationPresentationOptionBanner or
            platform.UserNotifications.UNNotificationPresentationOptionSound or
            platform.UserNotifications.UNNotificationPresentationOptionBadge
        )
    }
}

/**
 * Set up the notification delegate
 * Call this early in app initialization
 */
fun setupNotificationDelegate(): IosNotificationDelegate {
    val delegate = IosNotificationDelegate()
    UNUserNotificationCenter.currentNotificationCenter().delegate = delegate
    return delegate
}
