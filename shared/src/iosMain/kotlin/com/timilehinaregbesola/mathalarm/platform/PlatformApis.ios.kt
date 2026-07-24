package com.timilehinaregbesola.mathalarm.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.cinterop.ExperimentalForeignApi
import platform.AudioToolbox.AudioServicesPlaySystemSound
import platform.AudioToolbox.kSystemSoundID_Vibrate
import platform.Foundation.NSBundle
import platform.Foundation.NSURL
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIAlertAction
import platform.UIKit.UIAlertActionStyleCancel
import platform.UIKit.UIAlertActionStyleDefault
import platform.UIKit.UIAlertController
import platform.UIKit.UIAlertControllerStyleAlert
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationOpenSettingsURLString
import platform.UIKit.UIImpactFeedbackGenerator
import platform.UIKit.UIImpactFeedbackStyle
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionBadge
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNAuthorizationStatusAuthorized
import platform.UserNotifications.UNUserNotificationCenter

private var cachedNotificationsEnabled = false

private fun refreshNotificationStatus() {
    UNUserNotificationCenter.currentNotificationCenter().getNotificationSettingsWithCompletionHandler { settings ->
        cachedNotificationsEnabled = settings?.authorizationStatus == UNAuthorizationStatusAuthorized
    }
}

@OptIn(ExperimentalForeignApi::class)
actual class PlatformVibrator actual constructor() {
    private val feedbackGenerator = UIImpactFeedbackGenerator(style = UIImpactFeedbackStyle.UIImpactFeedbackStyleHeavy)
    private var isVibrating = false
    
    actual fun startWaveform(pattern: LongArray, repeat: Int) {
        isVibrating = true
        // Use system vibration
        AudioServicesPlaySystemSound(kSystemSoundID_Vibrate)
        // Also trigger haptic feedback for modern devices
        feedbackGenerator.prepare()
        feedbackGenerator.impactOccurred()
    }
    
    actual fun cancel() {
        isVibrating = false
        // iOS doesn't have a way to cancel vibration mid-vibrate
        // but we can stop the loop by setting flag
    }
}

actual fun getRingtoneTitle(alarmTone: String): String {
    return when {
        alarmTone.isEmpty() -> "Default"
        alarmTone == "alarm_classic" -> "Classic"
        alarmTone == "alarm_digital" -> "Digital"
        alarmTone == "alarm_gentle" -> "Gentle"
        alarmTone == "alarm_nature" -> "Nature"
        alarmTone == "alarm_urgent" -> "Urgent"
        alarmTone.contains("/") -> alarmTone.substringAfterLast("/").substringBeforeLast(".")
        else -> "Custom Sound"
    }
}

actual fun getDefaultAlarmTone(): String {
    return "alarm_classic"
}

actual fun shouldStartMathScreenAlarmAudio(fromSheet: Boolean): Boolean = true

actual fun isIosPlatform(): Boolean = true

@OptIn(ExperimentalForeignApi::class)
actual fun openNotificationSettings() {
    val url = NSURL.URLWithString(UIApplicationOpenSettingsURLString)
    url?.let {
        UIApplication.sharedApplication.openURL(it, options = emptyMap<Any?, Any?>()) { _ -> }
    }
}

actual fun requestExactAlarmPermission() {
    // iOS doesn't require explicit exact alarm permission
    // Local notifications are handled via UNUserNotificationCenter
    // Request notification permissions instead
    UNUserNotificationCenter.currentNotificationCenter().requestAuthorizationWithOptions(
        options = UNAuthorizationOptionAlert or UNAuthorizationOptionSound or UNAuthorizationOptionBadge
    ) { granted, _ ->
        cachedNotificationsEnabled = granted
    }
}

actual fun toPlatformMediaSource(uriString: String): String = uriString

actual fun areNotificationsEnabled(): Boolean {
    refreshNotificationStatus()
    return cachedNotificationsEnabled
}

@OptIn(ExperimentalForeignApi::class)
actual fun shareText(title: String, text: String) {
    val activityItems = listOf(text)
    val activityController = UIActivityViewController(
        activityItems = activityItems,
        applicationActivities = null
    )
    
    UIApplication.sharedApplication.keyWindow?.rootViewController?.presentViewController(
        activityController,
        animated = true,
        completion = null
    )
}

@OptIn(ExperimentalForeignApi::class)
actual fun sendEmail(chooserTitle: String, email: String, subject: String, body: String) {
    val encodedSubject = subject.replace(" ", "%20")
    val encodedBody = body.replace(" ", "%20").replace("\n", "%0A")
    val mailtoUrl = "mailto:$email?subject=$encodedSubject&body=$encodedBody"
    
    NSURL.URLWithString(mailtoUrl)?.let { url ->
        UIApplication.sharedApplication.openURL(url, options = emptyMap<Any?, Any?>()) { _ -> }
    }
}

actual fun getApplicationId(): String {
    return NSBundle.mainBundle.bundleIdentifier ?: "com.timilehinaregbesola.mathalarm"
}

private val bundledAlarmTones = listOf(
    "alarm_classic" to "Classic",
    "alarm_digital" to "Digital",
    "alarm_gentle" to "Gentle",
    "alarm_nature" to "Nature",
    "alarm_urgent" to "Urgent"
)

actual class RingtonePickerLauncher(
    private val onResult: (String?) -> Unit
) {
    actual fun launch(currentTone: String?) {
        val alert = UIAlertController.alertControllerWithTitle(
            title = "Alarm Sound",
            message = null,
            preferredStyle = UIAlertControllerStyleAlert
        )

        bundledAlarmTones.forEach { (tone, title) ->
            val actionTitle = if (tone == currentTone) "$title ✓" else title
            alert.addAction(
                UIAlertAction.actionWithTitle(
                    title = actionTitle,
                    style = UIAlertActionStyleDefault
                ) { _ ->
                    onResult(tone)
                }
            )
        }

        alert.addAction(
            UIAlertAction.actionWithTitle(
                title = "Cancel",
                style = UIAlertActionStyleCancel,
                handler = null
            )
        )

        UIApplication.sharedApplication.keyWindow?.rootViewController?.presentViewController(
            alert,
            animated = true,
            completion = null
        )
    }
}

@Composable
actual fun rememberRingtonePickerLauncher(onResult: (String?) -> Unit): RingtonePickerLauncher {
    return remember(onResult) { RingtonePickerLauncher(onResult) }
}

@Composable
actual fun rememberNotificationPermissionHandler(onResult: (Boolean) -> Unit): () -> Unit {
    return remember {
        {
            UNUserNotificationCenter.currentNotificationCenter().requestAuthorizationWithOptions(
                options = UNAuthorizationOptionAlert or UNAuthorizationOptionSound or UNAuthorizationOptionBadge
            ) { granted, _ ->
                cachedNotificationsEnabled = granted
                onResult(granted)
            }
        }
    }
}

actual fun checkRingtonePermissions(
    tones: List<String>,
    unplayableDialogTitle: String,
    unplayableDialogMessage: (String) -> String
) {
    // iOS doesn't need explicit permissions for bundled sounds
    // Media library access would need separate handling if using user's music
}

actual fun previewAlarmTone(alarmTone: String) {
    com.timilehinaregbesola.mathalarm.interactors.IosAlarmAudioManager.startAlarm(
        soundName = alarmTone,
        vibrate = false,
        volume = 0.75f,
    )
}

actual fun stopAlarmTonePreview() {
    com.timilehinaregbesola.mathalarm.interactors.IosAlarmAudioManager.stopAlarm()
}

actual fun stopPlatformAlarmAudio() {
    // Stop the iOS alarm audio manager
    com.timilehinaregbesola.mathalarm.interactors.IosAlarmAudioManager.stopAlarm()
}
