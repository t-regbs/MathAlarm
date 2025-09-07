@file:JvmName("extension-context")

package com.timilehinaregbesola.mathalarm.utils

import android.app.Activity
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.core.app.ActivityCompat
import androidx.core.app.AlarmManagerCompat
import androidx.core.content.ContextCompat
import timber.log.Timber
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

/**
 * Sets a alarm using [AlarmManagerCompat] to be triggered based on the given parameter.
 *
 * @param triggerAtMillis time in milliseconds that the alarm should go off, using the
 * appropriate clock (depending on the alarm type).
 * @param operation action to perform when the alarm goes off
 * @param type type to define how the alarm will behave
 */
@OptIn(ExperimentalTime::class)
fun Context.setExactAlarm(
    triggerAtMillis: Long,
    operation: PendingIntent?,
    type: Int = AlarmManager.RTC_WAKEUP,
) {
    var adjustedTriggerTime = triggerAtMillis
    val currentTime = Clock.System.now().toEpochMilliseconds()

    if (adjustedTriggerTime <= currentTime) {
        // If the alarm time is in the past, add one week (7 days) to the trigger time
        Timber.w("Alarm time is in the past, scheduling for next week")
        val oneWeekInMillis = 7 * 24 * 60 * 60 * 1000L
        adjustedTriggerTime += oneWeekInMillis
    }

    if (operation == null) {
        Timber.e("PendingIntent is null, cannot schedule alarm")
        return
    }

    val manager = getAlarmManager()
    if (manager == null) {
        Timber.e("AlarmManager is null, cannot schedule alarm")
        return
    }

    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S && !manager.canScheduleExactAlarms()) {
        Timber.e("Cannot schedule exact alarms - permission not granted on Android S+")
        return
    }

    try {
        AlarmManagerCompat.setExactAndAllowWhileIdle(
            manager,
            type,
            adjustedTriggerTime,
            operation
        )
        Timber.d("Alarm scheduled successfully")
    } catch (e: Exception) {
        Timber.e(e, "Failed to schedule alarm")
    }
}

/**
 * Cancels a alarm set on [AlarmManager], based on the given [PendingIntent].
 *
 * @param operation action to be canceled
 */
fun Context.cancelAlarm(operation: PendingIntent?) {
    Timber.d("cancelAlarm called with operation=${operation?.hashCode()}")

    if (operation == null) {
        Timber.e("PendingIntent is null, cannot cancel alarm")
        return
    }

    val manager = getAlarmManager()
    if (manager == null) {
        Timber.e("AlarmManager is null, cannot cancel alarm")
        return
    }

    try {
        Timber.d("Canceling alarm with AlarmManager.cancel")
        manager.cancel(operation)
        Timber.d("Alarm canceled successfully")
    } catch (e: Exception) {
        Timber.e(e, "Failed to cancel alarm")
    }
}

fun Context.email(
    chooserTitle: String,
    email: String = "",
    subject: String = "",
    text: String = "",
) {
    val intent = Intent(Intent.ACTION_SENDTO)
    intent.data = Uri.parse("mailto:")

    if (email.isNotEmpty()) {
        intent.putExtra(Intent.EXTRA_EMAIL, arrayOf(email))
    }

    if (subject.isNotEmpty()) {
        intent.putExtra(Intent.EXTRA_SUBJECT, subject)
    }

    if (text.isNotEmpty()) {
        intent.putExtra(Intent.EXTRA_TEXT, text)
    }

    startActivity(Intent.createChooser(intent, chooserTitle))
}

fun Context.hasPermission(permission: String) =
    ContextCompat.checkSelfPermission(this, permission) == PackageManager.PERMISSION_GRANTED

fun Activity.handlePermission(permission: String, callback: (granted: Boolean) -> Unit) {
    if (hasPermission(permission)) {
        callback(true)
    } else {
        ActivityCompat.requestPermissions(this, arrayOf(permission), 3)
    }
}

fun Context.openNotificationSettings() {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
        val intent = Intent(Settings.ACTION_APP_NOTIFICATION_SETTINGS)
        intent.putExtra(Settings.EXTRA_APP_PACKAGE, packageName)
        startActivity(intent)
    } else {
        // For Android versions below Oreo, you can't directly open the app's notification settings.
        // You can open the general notification settings instead.
        val intent = Intent(Settings.ACTION_SETTINGS)
        startActivity(intent)
    }
}
