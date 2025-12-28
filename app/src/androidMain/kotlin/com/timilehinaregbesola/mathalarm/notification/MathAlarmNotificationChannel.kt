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
        // Alarm channel (high priority for heads-up and full-screen intent)
        val alarmName = context.getString(R.string.channel_alarm_name)
        val alarmDescription = context.getString(R.string.channel_alarm_description)
        NotificationChannel(ALARM_CHANNEL_ID, alarmName, NotificationManager.IMPORTANCE_HIGH).apply {
            description = alarmDescription
            context.getNotificationManager()?.createNotificationChannel(this)
        }

        // Update channel (default priority for app update notifications)
        val updateName = context.getString(R.string.channel_update_name)
        val updateDescription = context.getString(R.string.channel_update_description)
        NotificationChannel(UPDATE_CHANNEL_ID, updateName, NotificationManager.IMPORTANCE_DEFAULT).apply {
            description = updateDescription
            context.getNotificationManager()?.createNotificationChannel(this)
        }
    }

    /**
     * Gets the alarm notification channel id.
     *
     * @return the alarm channel id
     */
    fun getAlarmChannelId() = ALARM_CHANNEL_ID

    /**
     * Gets the update notification channel id.
     *
     * @return the update channel id
     */
    fun getUpdateChannelId() = UPDATE_CHANNEL_ID

    companion object {
        const val ALARM_CHANNEL_ID = "alarm_channel"
        const val UPDATE_CHANNEL_ID = "update_channel"
    }
}
