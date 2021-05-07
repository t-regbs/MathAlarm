package com.timilehinaregbesola.mathalarm.utils

import android.Manifest
import android.app.Activity
import android.app.AlertDialog
import android.content.pm.PackageManager
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import androidx.core.net.toUri
import com.timilehinaregbesola.mathalarm.R

/**
 * Checks if all ringtones can be played, and requests permissions if it is not the case
 */
fun checkPermissions(activity: Activity, tones: List<String>) {
    if (Build.VERSION.SDK_INT >= 23 && activity.checkSelfPermission(Manifest.permission.READ_EXTERNAL_STORAGE) != PackageManager.PERMISSION_GRANTED) {
        val unplayable = tones
            .filter { alarmtone ->
                runCatching {
                    val player: MediaPlayer = MediaPlayer()
                    player.setDataSource(activity, alarmtone.toUri())
                    player.apply {
                        setOnErrorListener { mp, _, _ ->
//                            log.e("Error occurred while playing audio.")
                            mp.stop()
                            mp.release()
//                            player = null
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
                AlertDialog.Builder(activity).setTitle(activity.getString(R.string.alert))
                    .setMessage(activity.getString(R.string.permissions_external_storage_text, unplayable.joinToString(", ")))
                    .setPositiveButton(android.R.string.ok) { _, _ ->
                        activity.requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), 3)
                    }
                    .setNegativeButton(android.R.string.cancel, null)
                    .show()
            } catch (e: Exception) {
//                logger.e("Was not able to show dialog to request permission, continue without the dialog")
                activity.requestPermissions(arrayOf(Manifest.permission.READ_EXTERNAL_STORAGE), 3)
            }
        }
    }
}
