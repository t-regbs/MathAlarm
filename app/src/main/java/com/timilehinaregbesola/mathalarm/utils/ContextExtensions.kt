@file:JvmName("extension-context")

package com.timilehinaregbesola.mathalarm.utils

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.annotation.StringRes
import androidx.core.app.AlarmManagerCompat
import timber.log.Timber
import java.util.*

/**
 * Sets a alarm using [AlarmManagerCompat] to be triggered based on the given parameter.
 *
 * @param triggerAtMillis time in milliseconds that the alarm should go off, using the
 * appropriate clock (depending on the alarm type).
 * @param operation action to perform when the alarm goes off
 * @param type type to define how the alarm will behave
 */
fun Context.setExactAlarm(
    triggerAtMillis: Long,
    operation: PendingIntent?,
    type: Int = AlarmManager.RTC_WAKEUP,
) {
    val currentTime = Calendar.getInstance().timeInMillis
    if (triggerAtMillis <= currentTime) {
        Timber.w("It is not possible to set alarm in the past")
        return
    }

    if (operation == null) {
        Timber.e("PendingIntent is null")
        return
    }

    val manager = getAlarmManager()
    manager?.let {
        AlarmManagerCompat.setExactAndAllowWhileIdle(it, type, triggerAtMillis, operation)
    }
}

/**
 * Cancels a alarm set on [AlarmManager], based on the given [PendingIntent].
 *
 * @param operation action to be canceled
 */
fun Context.cancelAlarm(operation: PendingIntent?) {
    if (operation == null) {
        Timber.e("PendingIntent is null")
        return
    }

    val manager = getAlarmManager()
    manager?.let { manager.cancel(operation) }
}

/**
 * Shows a [Toast] with the given message.
 *
 * @param messageId the message String resource id
 * @param duration the Toast duration, if not provided will be set to [Toast.LENGTH_SHORT]
 */
fun Context.showToast(@StringRes messageId: Int, duration: Int = Toast.LENGTH_SHORT) {
    Toast.makeText(this, messageId, duration).show()
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
