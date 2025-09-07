package com.timilehinaregbesola.mathalarm.utils

import android.Manifest
import android.Manifest.permission.READ_EXTERNAL_STORAGE
import android.app.Activity
import android.app.AlertDialog
import android.content.Context
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import androidx.core.net.toUri
import timber.log.Timber

/**
 * Checks if all ringtones can be played, and requests permissions if it is not the case
 */
fun checkPermissions(
    activity: Activity,
    tones: List<String>,
    unplayableDialogTitle: String,
    unplayableDialogMessage: (String) -> String
) {
    if (Build.VERSION.SDK_INT >= 23 && activity.checkSelfPermission(READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
        val unplayable = tones
            .filter { alarmtone ->
                runCatching {
                    val player = MediaPlayer()
                    player.setDataSource(activity, alarmtone.toUri())
                    player.apply {
                        setOnErrorListener { mp, _, _ ->
                            Timber.e("Error occured while playing audio.")
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
                AlertDialog.Builder(activity).setTitle(unplayableDialogTitle)
                    .setMessage(unplayableDialogMessage(unplayable.joinToString(", ")))
                    .setPositiveButton(android.R.string.ok) { _, _ ->
                        activity.requestPermissions(arrayOf(READ_EXTERNAL_STORAGE), 3)
                    }
                    .setNegativeButton(android.R.string.cancel, null)
                    .show()
            } catch (e: Exception) {
                Timber.e("Was not able to show dialog to request permission, continue without the dialog")
                activity.requestPermissions(arrayOf(READ_EXTERNAL_STORAGE), 3)
            }
        }
    }
}

fun handleNotificationPermission(context: Activity, callback: (granted: Boolean) -> Unit) {
    if (Build.VERSION.SDK_INT >= 33) {
        context.handlePermission(Manifest.permission.POST_NOTIFICATIONS) { granted ->
            callback(granted)
        }
    } else {
        callback(true)
    }
}
