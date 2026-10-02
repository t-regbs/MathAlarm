package com.timilehinaregbesola.mathalarm.platform

import com.timilehinaregbesola.mathalarm.alarm.AlarmSchedulerBridge
import com.timilehinaregbesola.mathalarm.sound.AlarmSoundCatalog
import kotlinx.cinterop.ExperimentalForeignApi
import platform.AudioToolbox.AudioServicesPlaySystemSound
import platform.AudioToolbox.kSystemSoundID_Vibrate
import platform.Foundation.NSBundle
import platform.UIKit.UIImpactFeedbackGenerator
import platform.UIKit.UIImpactFeedbackStyle

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

actual fun getDefaultAlarmTone(): String {
    return AlarmSoundCatalog.DEFAULT_SOUND
}

actual fun supportsSkipNext(): Boolean = false
actual fun supportsAlarmVibration(): Boolean = false
actual fun alarmSoundPickerKind(): AlarmSoundPickerKind = AlarmSoundPickerKind.BUNDLED_LIBRARY

actual fun shouldStartMathScreenAlarmAudio(fromSheet: Boolean): Boolean = true

actual fun isIosPlatform(): Boolean = true

actual fun toPlatformMediaSource(uriString: String): String = uriString

actual fun areNotificationsEnabled(): Boolean {
    return AlarmSchedulerBridge.authorizationStatus() == "authorized"
}

actual fun getApplicationId(): String {
    return NSBundle.mainBundle.bundleIdentifier ?: "com.timilehinaregbesola.mathalarm"
}

actual fun getAppShareUrl(): String = "https://github.com/t-regbs/MathAlarm"

actual fun previewAlarmTone(alarmTone: String, onFinished: () -> Unit) {
    com.timilehinaregbesola.mathalarm.interactors.IosAlarmAudioManager.startPreview(alarmTone, onFinished)
}

actual fun stopAlarmTonePreview() {
    com.timilehinaregbesola.mathalarm.interactors.IosAlarmAudioManager.stopPreview()
}

actual fun previewOwnedAlarmTone(ownerId: String, alarmTone: String,
    onFinished: (com.timilehinaregbesola.mathalarm.sound.TonePreviewResult) -> Unit) {
    com.timilehinaregbesola.mathalarm.interactors.IosAlarmAudioManager.startOwnedPreview(ownerId, alarmTone, onFinished)
}

actual fun stopOwnedAlarmTonePreview(ownerId: String) {
    com.timilehinaregbesola.mathalarm.interactors.IosAlarmAudioManager.stopOwnedPreview(ownerId)
}

actual fun stopPlatformAlarmAudio() {
    // Stop the iOS alarm audio manager
    com.timilehinaregbesola.mathalarm.interactors.IosAlarmAudioManager.stopAlarm()
}
