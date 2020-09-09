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
    // Create the content intent for the notification, which launches
    // this activity
    // create intent
    val fullScreenIntent = Intent(applicationContext, AlarmMathActivity::class.java)
    fullScreenIntent.putExtra(ALARM_EXTRA, extras)
    // create PendingIntent
    val fullScreenPendingIntent = PendingIntent.getActivity(
        applicationContext,
        NOTIFICATION_ID,
        fullScreenIntent,
        PendingIntent.FLAG_UPDATE_CURRENT
    )
    val vibratePattern = longArrayOf(0, 100, 200, 300)
    //add style
    val alarmImage = BitmapFactory.decodeResource(
        applicationContext.resources,
        R.mipmap.ic_launcher
    )
    val bigPicStyle = NotificationCompat.BigPictureStyle()
        .bigPicture(alarmImage)
        .bigLargeIcon(null)
    // get an instance of NotificationCompat.Builder
    // Build the notification
    val builder = NotificationCompat.Builder(
        applicationContext,
        applicationContext.getString(R.string.alarm_notification_channel_id)
    )


        // set title, text and icon to builder
        .setSmallIcon(R.mipmap.ic_launcher)
        .setContentTitle(
            applicationContext
                .getString(R.string.notification_title)
        )
        .setContentText(messageBody)

        //set content intent
//        .setContentIntent(contentPendingIntent)
        .setFullScreenIntent(fullScreenPendingIntent, true)
        .setAutoCancel(true)

        // add style to builder
        .setStyle(bigPicStyle)
        .setLargeIcon(alarmImage)
        //Set visibility
        .setVisibility(VISIBILITY_PUBLIC)
        .setSound(tone, AudioManager.STREAM_ALARM)
        .setCategory(NotificationCompat.CATEGORY_ALARM)
        .setVibrate(vibratePattern)

        //set priority
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
