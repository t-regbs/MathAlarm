package com.android.example.mathalarm.utils

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import androidx.core.app.NotificationCompat
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
fun NotificationManager.sendNotification(messageBody: String, applicationContext: Context) {
    // Create the content intent for the notification, which launches
    // this activity
    // create intent
    val contentIntent = Intent(applicationContext, AlarmMathActivity::class.java)
    // create PendingIntent
    val contentPendingIntent = PendingIntent.getActivity(
        applicationContext,
        NOTIFICATION_ID,
        contentIntent,
        PendingIntent.FLAG_UPDATE_CURRENT
    )

    //add style
    val alarmImage = BitmapFactory.decodeResource(
        applicationContext.resources,
        R.mipmap.ic_launcher
    )
    val bigPicStyle = NotificationCompat.BigPictureStyle()
        .bigPicture(alarmImage)
        .bigLargeIcon(null)

    //add snooze action
//    val snoozeIntent = Intent(applicationContext, SnoozeReceiver::class.java)
//    val snoozePendingIntent: PendingIntent = PendingIntent.getBroadcast(
//        applicationContext,
//        REQUEST_CODE,
//        snoozeIntent,
//        FLAGS)

    // get an instance of NotificationCompat.Builder
    // Build the notification
    val builder = NotificationCompat.Builder(
        applicationContext,
        applicationContext.getString(R.string.alarm_notification_channel_id)
    )


        // set title, text and icon to builder
        .setSmallIcon(R.mipmap.ic_launcher)
        .setContentTitle(applicationContext
            .getString(R.string.notification_title))
        .setContentText(messageBody)

        //set content intent
        .setContentIntent(contentPendingIntent)
        .setAutoCancel(true)

        // add style to builder
        .setStyle(bigPicStyle)
        .setLargeIcon(alarmImage)

        // add snooze action
//        .addAction(
//            R.drawable.egg_icon,
//            applicationContext.getString(R.string.snooze),
//            snoozePendingIntent
//        )

        //set priority
        .setPriority(NotificationCompat.PRIORITY_HIGH)
    notify(NOTIFICATION_ID, builder.build())
}

/**
 * Cancels all notifications.
 *
 */
fun NotificationManager.cancelNotifications() {
    cancelAll()
}
