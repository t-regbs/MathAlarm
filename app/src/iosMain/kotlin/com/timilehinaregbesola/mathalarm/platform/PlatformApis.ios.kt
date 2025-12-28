package com.timilehinaregbesola.mathalarm.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import kotlinx.cinterop.ExperimentalForeignApi
import platform.AudioToolbox.AudioServicesPlaySystemSound
import platform.AudioToolbox.kSystemSoundID_Vibrate
import platform.Foundation.NSBundle
import platform.Foundation.NSURL
import platform.UIKit.UIActivityViewController
import platform.UIKit.UIApplication
import platform.UIKit.UIApplicationOpenSettingsURLString
import platform.UIKit.UIImpactFeedbackGenerator
import platform.UIKit.UIImpactFeedbackStyle
import platform.UserNotifications.UNAuthorizationOptionAlert
import platform.UserNotifications.UNAuthorizationOptionBadge
import platform.UserNotifications.UNAuthorizationOptionSound
import platform.UserNotifications.UNAuthorizationStatusAuthorized
import platform.UserNotifications.UNUserNotificationCenter

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
        alarmTone.contains("/") -> alarmTone.substringAfterLast("/").substringBeforeLast(".")
        else -> "Custom Sound"
    }
}

actual fun getDefaultAlarmTone(): String {
    // Return a bundled default alarm sound or empty for system default
    return ""
}

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
        // Handled by callback
    }
}

actual fun toPlatformMediaSource(uriString: String): String = uriString

actual fun areNotificationsEnabled(): Boolean {
    var enabled = false
    UNUserNotificationCenter.currentNotificationCenter().getNotificationSettingsWithCompletionHandler { settings ->
        enabled = settings?.authorizationStatus == UNAuthorizationStatusAuthorized
    }
    return enabled
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

actual class RingtonePickerLauncher {
    actual fun launch(currentTone: String?) {
        // iOS doesn't have a system ringtone picker like Android
        // In a production app, you would implement a custom sound picker UI
        // For now, this is a no-op - users can set sounds through app settings
    }
}

@Composable
actual fun rememberRingtonePickerLauncher(onResult: (String?) -> Unit): RingtonePickerLauncher {
    return remember { RingtonePickerLauncher() }
}

@Composable
actual fun rememberNotificationPermissionHandler(onResult: (Boolean) -> Unit): () -> Unit {
    return remember {
        {
            UNUserNotificationCenter.currentNotificationCenter().requestAuthorizationWithOptions(
                options = UNAuthorizationOptionAlert or UNAuthorizationOptionSound or UNAuthorizationOptionBadge
            ) { granted, _ ->
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

actual fun stopPlatformAlarmAudio() {
    // Stop the iOS alarm audio manager
    com.timilehinaregbesola.mathalarm.interactors.IosAlarmAudioManager.stopAlarm()
}
