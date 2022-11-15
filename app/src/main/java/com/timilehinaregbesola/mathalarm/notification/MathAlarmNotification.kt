package com.timilehinaregbesola.mathalarm.notification

import android.app.PendingIntent
import android.app.TaskStackBuilder
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.media.RingtoneManager
import android.os.Build
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.core.app.NotificationCompat
import androidx.core.net.toUri
import com.google.accompanist.navigation.material.ExperimentalMaterialNavigationApi
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.timilehinaregbesola.mathalarm.AlarmReceiver
import com.timilehinaregbesola.mathalarm.R
import com.timilehinaregbesola.mathalarm.domain.model.Alarm
import com.timilehinaregbesola.mathalarm.framework.database.AlarmEntity
import com.timilehinaregbesola.mathalarm.framework.database.AlarmMapper
import com.timilehinaregbesola.mathalarm.interactors.AudioPlayer
import com.timilehinaregbesola.mathalarm.presentation.MainActivity
import com.timilehinaregbesola.mathalarm.utils.getNotificationManager
import kotlinx.coroutines.InternalCoroutinesApi
import timber.log.Timber
import java.io.InputStream
import java.net.URLEncoder

/**
 * Handles the notification related to the Task reminders.
 */
@ExperimentalMaterialNavigationApi
@ExperimentalAnimationApi
@InternalCoroutinesApi
@ExperimentalComposeUiApi
@ExperimentalMaterialApi
@ExperimentalFoundationApi
class MathAlarmNotification(
    private val context: Context,
    private val channel: MathAlarmNotificationChannel,
    private val player: AudioPlayer
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
        var uriExists: Boolean
        var toneUri = alarm.alarmTone.toUri()
        player.apply {
            init()
            reset()
            try {
                val inputStream: InputStream? = context.contentResolver.openInputStream(toneUri)
                inputStream?.close()
                uriExists = true
            } catch (e: Exception) {
                uriExists = false
                Timber.w("File corresponding to the uri does not exist $toneUri")
            }
            toneUri = if (uriExists) toneUri else RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            setDataSource(toneUri)
            startAlarmAudio()
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
        val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
        val jsonAdapter = moshi.adapter(AlarmEntity::class.java).lenient()
        val json = jsonAdapter.toJson(AlarmMapper().mapFromDomainModel(alarm))
        val alarmJson = URLEncoder.encode(json, "utf-8")
        val notificationIntent = Intent(
            Intent.ACTION_VIEW,
            "https://timilehinaregbesola.com/alarmId=$alarmJson".toUri(),
            context,
            MainActivity::class.java
        )
        return TaskStackBuilder.create(context).run {
            addNextIntentWithParentStack(notificationIntent)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                getPendingIntent(REQUEST_CODE_OPEN_TASK, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE)
            } else {
                getPendingIntent(REQUEST_CODE_OPEN_TASK, PendingIntent.FLAG_UPDATE_CURRENT)
            }
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
