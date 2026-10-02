package com.timilehinaregbesola.mathalarm.platform

import android.content.Context
import android.media.RingtoneManager
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import org.koin.core.context.GlobalContext

private fun getKoinContext(): Context = GlobalContext.get().get()

actual class PlatformVibrator actual constructor() {
    private val context: Context = getKoinContext()
    private val vibrator = ContextCompat.getSystemService(context, Vibrator::class.java)

    actual fun startWaveform(pattern: LongArray, repeat: Int) {
        if (vibrator == null) return
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createWaveform(pattern, repeat))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(pattern, repeat)
        }
    }

    actual fun cancel() {
        vibrator?.cancel()
    }
}

actual fun getDefaultAlarmTone(): String = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM).toString()

actual fun supportsSkipNext(): Boolean = true
actual fun supportsAlarmVibration(): Boolean = true
actual fun alarmSoundPickerKind(): AlarmSoundPickerKind = AlarmSoundPickerKind.SYSTEM

actual fun shouldStartMathScreenAlarmAudio(fromSheet: Boolean): Boolean = fromSheet

actual fun isIosPlatform(): Boolean = false

actual fun toPlatformMediaSource(uriString: String): String = uriString

actual fun areNotificationsEnabled(): Boolean {
    val context: Context = getKoinContext()
    return NotificationManagerCompat.from(context).areNotificationsEnabled()
}

actual fun getApplicationId(): String = getKoinContext().packageName

actual fun getAppShareUrl(): String =
    "https://play.google.com/store/apps/details?id=${getApplicationId()}"

actual fun previewAlarmTone(alarmTone: String, onFinished: () -> Unit) = onFinished()

actual fun stopAlarmTonePreview() = Unit

actual fun stopPlatformAlarmAudio() {
    // On Android, alarm audio is handled by the AlarmReceiver/service
    // This is a no-op as the audio stops when the ViewModel's audioPlayer.stop() is called
}
