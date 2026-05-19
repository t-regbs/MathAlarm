package com.timilehinaregbesola.mathalarm.platform

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import android.provider.Settings
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import co.touchlab.kermit.Logger
import com.timilehinaregbesola.mathalarm.BuildConfig
import com.timilehinaregbesola.mathalarm.utils.PickRingtone
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

actual fun getRingtoneTitle(alarmTone: String): String {
    val context: Context = getKoinContext()
    return try {
        RingtoneManager.getRingtone(context, alarmTone.toUri()).getTitle(context)
    } catch (_: Exception) {
        ""
    }
}

actual fun getDefaultAlarmTone(): String = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM).toString()

actual fun shouldStartMathScreenAlarmAudio(fromSheet: Boolean): Boolean = fromSheet

actual fun isIosPlatform(): Boolean = false

actual fun openNotificationSettings() {
    val context: Context = getKoinContext()
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
            putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    } else {
        val intent = Intent(Settings.ACTION_SETTINGS).apply { addFlags(Intent.FLAG_ACTIVITY_NEW_TASK) }
        context.startActivity(intent)
    }
}

actual fun requestExactAlarmPermission() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val context: Context = getKoinContext()
        val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
    }
}

actual fun toPlatformMediaSource(uriString: String): String = uriString

actual fun areNotificationsEnabled(): Boolean {
    val context: Context = getKoinContext()
    return NotificationManagerCompat.from(context).areNotificationsEnabled()
}

actual fun shareText(title: String, text: String) {
    val context: Context = getKoinContext()
    val sendIntent = Intent().apply {
        action = Intent.ACTION_SEND
        putExtra(Intent.EXTRA_TEXT, text)
        type = "text/plain"
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(Intent.createChooser(sendIntent, title).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    })
}

actual fun sendEmail(chooserTitle: String, email: String, subject: String, body: String) {
    val context: Context = getKoinContext()
    val intent = Intent(Intent.ACTION_SENDTO).apply {
        data = "mailto:".toUri()
        if (email.isNotEmpty()) {
            putExtra(Intent.EXTRA_EMAIL, arrayOf(email))
        }
        if (subject.isNotEmpty()) {
            putExtra(Intent.EXTRA_SUBJECT, subject)
        }
        if (body.isNotEmpty()) {
            putExtra(Intent.EXTRA_TEXT, body)
        }
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(Intent.createChooser(intent, chooserTitle).apply {
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    })
}

actual fun getApplicationId(): String = BuildConfig.APPLICATION_ID

actual class RingtonePickerLauncher(
    private val launchPicker: (String?) -> Unit,
) {
    actual fun launch(currentTone: String?) {
        launchPicker(currentTone)
    }
}

@Composable
actual fun rememberRingtonePickerLauncher(onResult: (String?) -> Unit): RingtonePickerLauncher {
    // We need to create the launcher dynamically based on the current tone
    // Using a state-based approach to handle the current tone
    val currentToneState = remember { mutableStateOf("") }

    val launcher = rememberLauncherForActivityResult(
        contract = PickRingtone(currentToneState.value)
    ) { uri ->
        onResult(uri?.toString())
    }

    return remember(launcher) {
        RingtonePickerLauncher { tone ->
            currentToneState.value = tone ?: ""
            launcher.launch(null)
        }
    }
}

@Composable
actual fun rememberNotificationPermissionHandler(onResult: (Boolean) -> Unit): () -> Unit {
    val activity = LocalActivity.current

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        onResult(isGranted)
    }

    return remember(activity) {
        {
            if (Build.VERSION.SDK_INT >= 33) {
                val permission = Manifest.permission.POST_NOTIFICATIONS
                when {
                    activity?.let {
                        ContextCompat.checkSelfPermission(it, permission)
                    } == PackageManager.PERMISSION_GRANTED -> {
                        onResult(true)
                    }
                    else -> {
                        permissionLauncher.launch(permission)
                    }
                }
            } else {
                onResult(true)
            }
        }
    }
}

actual fun checkRingtonePermissions(
    tones: List<String>,
    unplayableDialogTitle: String,
    unplayableDialogMessage: (String) -> String
) {
    val context: Context = getKoinContext()
    val activity = (context as? Activity) ?: return

    if (Build.VERSION.SDK_INT >= 23 &&
        activity.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
    ) {
        val unplayable = tones
            .filter { alarmtone ->
                runCatching {
                    val player = MediaPlayer()
                    player.setDataSource(activity, alarmtone.toUri())
                    player.apply {
                        setOnErrorListener { mp, _, _ ->
                            Logger.e("Error occurred while playing audio.")
                            mp.stop()
                            mp.release()
                            true
                        }
                    }
                }.isFailure
            }
            .mapNotNull { tone -> RingtoneManager.getRingtone(activity, Uri.parse(tone)) }
            .map { ringtone ->
                runCatching {
                    ringtone.getTitle(activity) ?: "null"
                }.getOrDefault("null")
            }

        if (unplayable.isNotEmpty()) {
            try {
                AlertDialog.Builder(activity)
                    .setTitle(unplayableDialogTitle)
                    .setMessage(unplayableDialogMessage(unplayable.joinToString(", ")))
                    .setPositiveButton(android.R.string.ok) { _, _ ->
                        ActivityCompat.requestPermissions(
                            activity,
                            arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                            3
                        )
                    }
                    .setNegativeButton(android.R.string.cancel, null)
                    .show()
            } catch (e: Exception) {
                Logger.e("Was not able to show dialog to request permission")
                ActivityCompat.requestPermissions(
                    activity,
                    arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE),
                    3
                )
            }
        }
    }
}

actual fun previewAlarmTone(alarmTone: String) = Unit

actual fun stopAlarmTonePreview() = Unit

actual fun stopPlatformAlarmAudio() {
    // On Android, alarm audio is handled by the AlarmReceiver/service
    // This is a no-op as the audio stops when the ViewModel's audioPlayer.stop() is called
}
