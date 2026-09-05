@file:JvmName("extension-context")

package com.timilehinaregbesola.mathalarm.utils

import android.app.Activity
import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.AlarmManagerCompat
import androidx.core.content.ContextCompat
import co.touchlab.kermit.Logger
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
@OptIn(ExperimentalTime::class, androidx.compose.foundation.ExperimentalFoundationApi::class,
    androidx.compose.material3.ExperimentalMaterial3Api::class, androidx.compose.ui.ExperimentalComposeUiApi::class,
    kotlinx.coroutines.InternalCoroutinesApi::class, androidx.compose.animation.ExperimentalAnimationApi::class)
fun Context.setExactAlarm(
    triggerAtMillis: Long,
    operation: PendingIntent?,
    type: Int = AlarmManager.RTC_WAKEUP,
) {
    requireNotNull(operation) { "Alarm PendingIntent is missing" }
    val manager = checkNotNull(getAlarmManager()) { "AlarmManager is unavailable" }
    check(Build.VERSION.SDK_INT < Build.VERSION_CODES.S || manager.canScheduleExactAlarms()) {
        "Allow alarms and reminders in system settings to schedule this alarm"
    }
    if (type == AlarmManager.RTC_WAKEUP) {
        val showIntent = PendingIntent.getActivity(this, 0,
            android.content.Intent(this, com.timilehinaregbesola.mathalarm.presentation.MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        // Past times are delivered promptly by AlarmManager; never silently change the date.
        manager.setAlarmClock(AlarmManager.AlarmClockInfo(triggerAtMillis, showIntent), operation)
    } else {
        AlarmManagerCompat.setExactAndAllowWhileIdle(manager, type, triggerAtMillis, operation)
    }
}

/**
 * Cancels a alarm set on [AlarmManager], based on the given [PendingIntent].
 *
 * @param operation action to be canceled
 */
fun Context.cancelAlarm(operation: PendingIntent?) {
    Logger.d(messageString = "cancelAlarm called with operation=${operation?.hashCode()}", tag = "Context cancelAlarm")

    if (operation == null) {
        Logger.e(messageString = "PendingIntent is null, cannot cancel alarm", tag = "Context cancelAlarm")
        return
    }

    val manager = getAlarmManager()
    if (manager == null) {
        Logger.e(messageString = "AlarmManager is null, cannot cancel alarm", tag = "Context cancelAlarm")
        return
    }

    try {
        Logger.d(messageString = "Canceling alarm with AlarmManager.cancel", tag = "Context cancelAlarm")
        manager.cancel(operation)
        Logger.d(messageString = "Alarm canceled successfully", tag = "Context cancelAlarm")
    } catch (e: Exception) {
        Logger.e(e, tag = "Context cancelAlarm") {"Failed to cancel alarm"}
    }
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
