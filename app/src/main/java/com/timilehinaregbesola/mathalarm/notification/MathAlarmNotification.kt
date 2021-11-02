package com.timilehinaregbesola.mathalarm.notification

import android.app.PendingIntent
import android.app.TaskStackBuilder
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.media.AudioAttributes
import android.media.MediaPlayer
import androidx.core.app.NotificationCompat
import androidx.core.net.toUri
import com.timilehinaregbesola.mathalarm.AlarmReceiver
import com.timilehinaregbesola.mathalarm.R
import com.timilehinaregbesola.mathalarm.domain.model.Alarm
import com.timilehinaregbesola.mathalarm.presentation.MainActivity
import com.timilehinaregbesola.mathalarm.utils.getNotificationManager
import timber.log.Timber

/**
 * Handles the notification related to the Task reminders.
 */
internal class MathAlarmNotification(
    private val context: Context,
    private val channel: MathAlarmNotificationChannel,
    private val player: MediaPlayer
) {
    /**
     * Shows the [MathAlarmNotification] based on the given Alarm.
     *
     * @param alarm the alarm event to be shown in the notification
     */
    fun show(alarm: Alarm) {
        Timber.d("Showing notification for '${alarm.title}'")
        val builder = buildNotification(alarm)
//        builder.addAction(getCompleteAction(alarm))
        player.apply {
            reset()
            setDataSource(context, alarm.alarmTone.toUri())
            setAudioAttributes(
                AudioAttributes.Builder()
                    .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                    .setUsage(AudioAttributes.USAGE_ALARM)
                    .build()
            )
            prepare()
            isLooping = true
            start()
        }
        context.getNotificationManager()?.notify(alarm.alarmId.toInt(), builder.build())
    }

    /**
     * Shows the repeating [MathAlarmNotification] based on the given Alarm.
     *
     * @param alarm the task to be shown in the notification
     */
    fun showRepeating(alarm: Alarm) {
        Timber.d("Showing repeating notification for '${alarm.title}'")
        val builder = buildNotification(alarm)
        context.getNotificationManager()?.notify(alarm.alarmId.toInt(), builder.build())
    }

    /**
     * Dismiss the [MathAlarmNotification] based on the given id.
     *
     * @param notificationId the notification id to be dismissed
     */
    fun dismiss(notificationId: Long) {
        Timber.d("Dismissing notification id '$notificationId'")
        if (player.isPlaying)
            player.stop()
        context.getNotificationManager()?.cancel(notificationId.toInt())
    }

    private fun buildNotification(alarm: Alarm) =
        NotificationCompat.Builder(context, channel.getChannelId()).apply {
            val alarmImage = BitmapFactory.decodeResource(context.resources, R.drawable.icon)
            val vibratePattern = longArrayOf(0, 100, 200, 300)
            val bigPicStyle = NotificationCompat.BigPictureStyle()
                .bigPicture(alarmImage)
                .bigLargeIcon(null)
//            setContentIntent(buildPendingIntent(task))
            setSmallIcon(R.drawable.icon)
            setContentTitle(context.getString(R.string.notification_title))
            setContentText(alarm.title)
            setFullScreenIntent(buildPendingIntent(alarm), true)
            setStyle(bigPicStyle)
            setSound(null)
            setLargeIcon(alarmImage)
            setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            setCategory(NotificationCompat.CATEGORY_ALARM)
            setVibrate(vibratePattern)
            setPriority(NotificationCompat.PRIORITY_HIGH)
            setAutoCancel(true)
            addAction(getSnoozeAction(alarm))
        }

    private fun buildPendingIntent(alarm: Alarm): PendingIntent {
        val notificationIntent = Intent(
            Intent.ACTION_VIEW,
            "https://timilehinaregbesola.com/alarmId=${alarm.alarmId}".toUri(),
            context,
            MainActivity::class.java
        )
        return TaskStackBuilder.create(context).run {
            addNextIntentWithParentStack(notificationIntent)
            getPendingIntent(REQUEST_CODE_OPEN_TASK, PendingIntent.FLAG_UPDATE_CURRENT)
        }
    }

    private fun getCompleteAction(alarm: Alarm): NotificationCompat.Action {
        val actionTitle = context.getString(R.string.notification_action_completed)
        val intent = getIntent(alarm, AlarmReceiver.COMPLETE_ACTION, REQUEST_CODE_ACTION_COMPLETE)
        return NotificationCompat.Action(ACTION_NO_ICON, actionTitle, intent)
    }

    private fun getSnoozeAction(alarm: Alarm): NotificationCompat.Action {
        val actionTitle = context.getString(R.string.notification_action_snooze)
        val intent = getIntent(alarm, AlarmReceiver.SNOOZE_ACTION, REQUEST_CODE_ACTION_SNOOZE)
        return NotificationCompat.Action(ACTION_NO_ICON, actionTitle, intent)
    }

    private fun getIntent(
        alarm: Alarm,
        intentAction: String,
        requestCode: Int
    ): PendingIntent {
        val receiverIntent = Intent(context, AlarmReceiver::class.java).apply {
            action = intentAction
            putExtra(AlarmReceiver.EXTRA_TASK, alarm.alarmId)
        }

        return PendingIntent
            .getBroadcast(
                context,
                requestCode,
                receiverIntent,
                PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
    }

    companion object {

        private const val REQUEST_CODE_OPEN_TASK = 1_121_111

        private const val REQUEST_CODE_ACTION_COMPLETE = 1_234

        private const val REQUEST_CODE_ACTION_SNOOZE = 4_321

        private const val ACTION_NO_ICON = 0
    }
}
