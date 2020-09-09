package com.android.example.mathalarm.utils

import android.app.Notification
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.media.AudioManager
import android.net.Uri
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.VISIBILITY_PUBLIC
import com.android.example.mathalarm.R
import com.android.example.mathalarm.screens.alarmmath.AlarmMathActivity

// Notification ID.
private val NOTIFICATION_ID = 0
private val REQUEST_CODE = 0
private val FLAGS = 0

/**
 * Builds and delivers the notification.
 *
 * @param context, activity context.
 */
fun NotificationManager.sendNotification(
    applicationContext: Context,
    messageBody: String,
    extras: String,
    tone: Uri
) {

    val fullScreenIntent = Intent(applicationContext, AlarmMathActivity::class.java)
    fullScreenIntent.putExtra(ALARM_EXTRA, extras)
    val fullScreenPendingIntent = PendingIntent.getActivity(
        applicationContext,
        NOTIFICATION_ID,
        fullScreenIntent,
        PendingIntent.FLAG_UPDATE_CURRENT
    )
    val vibratePattern = longArrayOf(0, 100, 200, 300)
    val alarmImage = BitmapFactory.decodeResource(
        applicationContext.resources,
        R.mipmap.ic_launcher
    )
    val bigPicStyle = NotificationCompat.BigPictureStyle()
        .bigPicture(alarmImage)
        .bigLargeIcon(null)

    val builder = NotificationCompat.Builder(
        applicationContext,
        applicationContext.getString(R.string.alarm_notification_channel_id)
    )
        .setSmallIcon(R.mipmap.ic_launcher)
        .setContentTitle(applicationContext.getString(R.string.notification_title))
        .setContentText(messageBody)
        .setFullScreenIntent(fullScreenPendingIntent, true)
        .setAutoCancel(true)
        .setStyle(bigPicStyle)
        .setLargeIcon(alarmImage)
        .setVisibility(VISIBILITY_PUBLIC)
        .setSound(tone, AudioManager.STREAM_MUSIC)
        .setCategory(NotificationCompat.CATEGORY_ALARM)
        .setVibrate(vibratePattern)
        .setPriority(NotificationCompat.PRIORITY_HIGH)

    val notif = builder.build()
    notif.flags = notif.flags or Notification.FLAG_INSISTENT
    notify(NOTIFICATION_ID, notif)
}

/**
 * Cancels all notifications.
 *
 */
fun NotificationManager.cancelNotifications() {
    cancelAll()
}
