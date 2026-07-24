package com.timilehinaregbesola.mathalarm.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf
import com.timilehinaregbesola.mathalarm.presentation.appsettings.AlarmPreferences

// Vibration
expect class PlatformVibrator() {
    fun startWaveform(pattern: LongArray, repeat: Int)
    fun cancel()
}

// Ringtone title resolver
expect fun getRingtoneTitle(alarmTone: String): String

// Default alarm tone uri as String
expect fun getDefaultAlarmTone(): String

// Whether MathScreen should start its own alarm audio player.
// Android keeps notification alarm audio in AlarmService; iOS starts audio from
// the screen after notification tap because background notification audio is limited.
expect fun shouldStartMathScreenAlarmAudio(fromSheet: Boolean): Boolean

// True when running on iOS.
expect fun isIosPlatform(): Boolean

// Notification permission helpers
expect fun openNotificationSettings()

// Exact alarm permission screen
expect fun requestExactAlarmPermission()

// Platform URI conversion helper if needed by players; may return same string on some platforms
expect fun toPlatformMediaSource(uriString: String): String

// Check if notifications are enabled for the app
expect fun areNotificationsEnabled(): Boolean

// Share text with other apps
expect fun shareText(title: String, text: String)

// Send email
expect fun sendEmail(chooserTitle: String, email: String, subject: String = "", body: String = "")

// Get application ID for sharing
expect fun getApplicationId(): String

// Applies platform night mode state for Android XML resources and launch surfaces.
expect fun applyPlatformNightMode(theme: AlarmPreferences.Theme)

// CompositionLocal for optional access to PlatformVibrator (can be replaced in previews/tests)
val LocalPlatformVibrator = staticCompositionLocalOf<PlatformVibrator?> { null }

/**
 * Platform-specific ringtone picker result handler.
 * Returns the selected ringtone URI as a String, or null if cancelled.
 */
expect class RingtonePickerLauncher {
    fun launch(currentTone: String?)
}

/**
 * Remember a ringtone picker launcher that calls [onResult] with the selected tone URI.
 */
@Composable
expect fun rememberRingtonePickerLauncher(onResult: (String?) -> Unit): RingtonePickerLauncher

/**
 * Platform-specific permission handling for notification permissions.
 * Calls [onResult] with true if permission granted, false otherwise.
 */
@Composable
expect fun rememberNotificationPermissionHandler(onResult: (Boolean) -> Unit): () -> Unit

/**
 * Check and request external storage permission for playing ringtones if needed.
 * This is mainly needed on Android for custom ringtones.
 * @param unplayableDialogMessage A function that takes the list of unplayable tones as a string and returns the message
 */
expect fun checkRingtonePermissions(
    tones: List<String>,
    unplayableDialogTitle: String,
    unplayableDialogMessage: (String) -> String
)

expect fun previewAlarmTone(alarmTone: String)

expect fun stopAlarmTonePreview()

/**
 * Stop the platform alarm audio.
 * On iOS, this stops the IosAlarmAudioManager.
 * On Android, this is a no-op (audio is handled by the service).
 */
expect fun stopPlatformAlarmAudio()
