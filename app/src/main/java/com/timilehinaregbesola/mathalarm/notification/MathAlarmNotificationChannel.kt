package com.timilehinaregbesola.mathalarm.notification

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.timilehinaregbesola.mathalarm.R
import com.timilehinaregbesola.mathalarm.utils.getNotificationManager

/**
 * [NotificationChannel] to send Alarm notifications in Android O and above.
 */
class MathAlarmNotificationChannel(context: Context) {

    init {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val name = context.getString(R.string.channel_alarm_name)
            val description = context.getString(R.string.channel_alarm_description)
            val importance = NotificationManager.IMPORTANCE_HIGH

            NotificationChannel(CHANNEL_ID, name, importance).apply {
                this.description = description
//                setSound(null, null)
                context.getNotificationManager()?.createNotificationChannel(this)
            }
        }
    }

    /**
     * Gets the [MathAlarmNotificationChannel] id.
     *
     * @return the [MathAlarmNotificationChannel] id
     */
    fun getChannelId() = CHANNEL_ID

    companion object {

        const val CHANNEL_ID = "alarm_channel"
    }
}
