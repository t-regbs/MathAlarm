package com.timilehinaregbesola.mathalarm.platform

import com.timilehinaregbesola.mathalarm.presentation.appsettings.AlarmPreferences

// Vibration
expect class PlatformVibrator() {
    fun startWaveform(pattern: LongArray, repeat: Int)
    fun cancel()
}

// Default alarm tone uri as String
expect fun getDefaultAlarmTone(): String

expect fun supportsSkipNext(): Boolean

expect fun supportsAlarmVibration(): Boolean

enum class AlarmSoundPickerKind { SYSTEM, BUNDLED_LIBRARY }

expect fun alarmSoundPickerKind(): AlarmSoundPickerKind

// Whether MathScreen should start its own alarm audio player.
// Android keeps notification alarm audio in AlarmService; iOS starts audio from
// the screen after notification tap because background notification audio is limited.
expect fun shouldStartMathScreenAlarmAudio(fromSheet: Boolean): Boolean

// True when running on iOS.
expect fun isIosPlatform(): Boolean

// Platform URI conversion helper if needed by players; may return same string on some platforms
expect fun toPlatformMediaSource(uriString: String): String

// Check if notifications are enabled for the app
expect fun areNotificationsEnabled(): Boolean

// Get application ID for sharing
expect fun getApplicationId(): String

/** URL safe to share for the current platform's release. */
expect fun getAppShareUrl(): String

// Applies platform night mode state for Android XML resources and launch surfaces.
expect fun applyPlatformNightMode(theme: AlarmPreferences.Theme)

expect fun previewAlarmTone(alarmTone: String, onFinished: () -> Unit)

expect fun stopAlarmTonePreview()

/**
 * Stop the platform alarm audio.
 * On iOS, this stops the IosAlarmAudioManager.
 * On Android, this is a no-op (audio is handled by the service).
 */
expect fun stopPlatformAlarmAudio()
