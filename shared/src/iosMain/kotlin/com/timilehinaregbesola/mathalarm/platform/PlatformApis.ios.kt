package com.timilehinaregbesola.mathalarm.platform

import com.timilehinaregbesola.mathalarm.alarm.AlarmSchedulerBridge
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import com.timilehinaregbesola.mathalarm.sound.AlarmSoundCatalog
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
import platform.UIKit.UIViewController
import platform.UIKit.popoverPresentationController

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
    AlarmSoundCatalog.find(alarmTone)?.let { return it.displayName }
    return when {
        alarmTone.isEmpty() -> "Default"
        alarmTone.contains("/") -> alarmTone.substringAfterLast("/").substringBeforeLast(".")
        else -> "Custom Sound"
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

@OptIn(ExperimentalForeignApi::class)
actual fun openNotificationSettings() {
    val url = NSURL.URLWithString(UIApplicationOpenSettingsURLString)
    url?.let {
        UIApplication.sharedApplication.openURL(it, options = emptyMap<Any?, Any?>()) { _ -> }
    }
}

actual fun requestExactAlarmPermission() {
    AlarmSchedulerBridge.requestAuthorization { granted ->
        if (!granted) openNotificationSettings()
    }
}

actual fun toPlatformMediaSource(uriString: String): String = uriString

actual fun areNotificationsEnabled(): Boolean {
    return AlarmSchedulerBridge.authorizationStatus() == "authorized"
}

@OptIn(ExperimentalForeignApi::class)
actual fun shareText(title: String, text: String) {
    val activityItems = listOf(text)
    val activityController = UIActivityViewController(
        activityItems = activityItems,
        applicationActivities = null
    )
    
    var presenter: UIViewController = UIApplication.sharedApplication.keyWindow?.rootViewController ?: return
    while (presenter.presentedViewController != null) {
        presenter = presenter.presentedViewController ?: break
    }
    activityController.popoverPresentationController?.apply {
        sourceView = presenter.view
        sourceRect = presenter.view.bounds
    }
    presenter.presentViewController(
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

actual fun getAppShareUrl(): String = "https://github.com/t-regbs/MathAlarm"

// The shared bundled sound editor handles selection on iOS.
@Composable
actual fun rememberRingtonePickerLauncher(onResult: (String?) -> Unit): RingtonePickerLauncher? = null

@Composable
actual fun rememberNotificationPermissionHandler(onResult: (Boolean) -> Unit): () -> Unit {
    return remember {
        {
            AlarmSchedulerBridge.requestAuthorization(onResult)
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

actual fun previewAlarmTone(alarmTone: String, onFinished: () -> Unit) {
    com.timilehinaregbesola.mathalarm.interactors.IosAlarmAudioManager.startPreview(alarmTone, onFinished)
}

actual fun stopAlarmTonePreview() {
    com.timilehinaregbesola.mathalarm.interactors.IosAlarmAudioManager.stopPreview()
}

actual fun stopPlatformAlarmAudio() {
    // Stop the iOS alarm audio manager
    com.timilehinaregbesola.mathalarm.interactors.IosAlarmAudioManager.stopAlarm()
}
