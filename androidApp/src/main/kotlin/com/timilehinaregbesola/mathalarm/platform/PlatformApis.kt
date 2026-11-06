package com.timilehinaregbesola.mathalarm.platform

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.remember
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import co.touchlab.kermit.Logger
import com.russhwolf.settings.Settings as AppSettings
import com.timilehinaregbesola.mathalarm.analytics.AnalyticsEvents
import com.timilehinaregbesola.mathalarm.analytics.AnalyticsTracker
import com.timilehinaregbesola.mathalarm.analytics.PermissionAnalyticsPending
import com.timilehinaregbesola.mathalarm.analytics.trackSafely
import com.timilehinaregbesola.mathalarm.utils.PickRingtone
import org.koin.core.context.GlobalContext

private fun getKoinContext(): Context = GlobalContext.get().get()

private fun trackPermissionHandoff(type: String, key: String) {
    runCatching {
        GlobalContext.get().get<AppSettings>().putBoolean(key, true)
        GlobalContext.get().get<AnalyticsTracker>()
            .trackSafely(AnalyticsEvents.permissionPrompted(type, "settings"))
    }
}

/**
 * Platform-specific ringtone picker result handler.
 * Returns the selected ringtone URI as a String, or null if cancelled.
 */
fun interface RingtonePickerLauncher {
    fun launch(currentTone: String?)
}

fun getRingtoneTitle(alarmTone: String): String {
    val context: Context = getKoinContext()
    return try {
        val ringtone = RingtoneManager.getRingtone(context, alarmTone.toUri())
        try {
            ringtone?.getTitle(context).orEmpty()
        } finally {
            ringtone?.stop()
        }
    } catch (_: Exception) {
        ""
    }
}

fun openNotificationSettings() {
    val context: Context = getKoinContext()
    val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS).apply {
        putExtra(Settings.EXTRA_APP_PACKAGE, context.packageName)
        addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    }
    context.startActivity(intent)
    trackPermissionHandoff("notifications", PermissionAnalyticsPending.NOTIFICATIONS)
}

fun requestExactAlarmPermission() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val context: Context = getKoinContext()
        val intent = Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        }
        context.startActivity(intent)
        trackPermissionHandoff("exact_alarm", PermissionAnalyticsPending.EXACT_ALARM)
    }
}

fun shareText(title: String, text: String) {
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

fun sendEmail(chooserTitle: String, email: String, subject: String = "", body: String = "") {
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

@Composable
fun rememberRingtonePickerLauncher(onResult: (String?) -> Unit): RingtonePickerLauncher? {
    val currentOnResult = rememberUpdatedState(onResult)
    val launcher = rememberLauncherForActivityResult(contract = PickRingtone()) { uri ->
        currentOnResult.value(uri?.toString())
    }
    return remember(launcher) {
        RingtonePickerLauncher { tone -> launcher.launch(tone) }
    }
}

@Composable
fun rememberNotificationPermissionHandler(onResult: (Boolean) -> Unit): () -> Unit {
    val activity = LocalActivity.current

    val permissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        runCatching {
            GlobalContext.get().get<AnalyticsTracker>().trackSafely(
                AnalyticsEvents.permissionResult("notifications", if (isGranted) "granted" else "denied")
            )
        }
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
                        runCatching {
                            GlobalContext.get().get<AnalyticsTracker>().trackSafely(
                                AnalyticsEvents.permissionPrompted("notifications", "runtime")
                            )
                        }
                        permissionLauncher.launch(permission)
                    }
                }
            } else {
                onResult(true)
            }
        }
    }
}

fun checkRingtonePermissions(
    activity: Activity,
    tones: List<String>,
    unplayableDialogTitle: String,
    unplayableDialogMessage: (String) -> String
) {
    if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.S_V2 &&
        activity.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED
    ) {
        val unplayable = tones
            .filter { tone ->
                val player = MediaPlayer()
                try {
                    player.setDataSource(activity, tone.toUri())
                    false
                } catch (_: Exception) {
                    true
                } finally {
                    player.release()
                }
            }
            .map { tone ->
                runCatching {
                    val ringtone = RingtoneManager.getRingtone(activity, tone.toUri())
                    try {
                        ringtone?.getTitle(activity)
                    } finally {
                        ringtone?.stop()
                    }
                }.getOrNull()?.takeIf { it.isNotBlank() } ?: tone
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
