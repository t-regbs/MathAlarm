package com.timilehinaregbesola.mathalarm.utils

import android.app.Activity
import android.content.Context
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import androidx.activity.result.contract.ActivityResultContract
import com.timilehinaregbesola.mathalarm.domain.model.Alarm

class PickRingtone(val alarm: Alarm?) : ActivityResultContract<Int?, Uri?>() {
    override fun createIntent(context: Context, ringtoneType: Int?) =
        Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
            putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, alarm!!.alarmTone)

            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
            putExtra(
                RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI,
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            )

            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true)
            putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
        }

    override fun parseResult(resultCode: Int, result: Intent?): Uri? {
        if (resultCode != Activity.RESULT_OK) {
            return null
        }
        return result?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
    }
}
