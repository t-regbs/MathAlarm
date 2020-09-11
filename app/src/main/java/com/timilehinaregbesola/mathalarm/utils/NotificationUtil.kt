package com.timilehinaregbesola.mathalarm.utils

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.BitmapFactory
import android.graphics.Color
import android.media.AudioAttributes
import android.media.AudioManager
import android.net.Uri
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.VISIBILITY_PUBLIC
import com.timilehinaregbesola.mathalarm.R
import com.timilehinaregbesola.mathalarm.screens.alarmmath.AlarmMathActivity


// Notification ID.
private val NOTIFICATION_ID = 0
private val REQUEST_CODE = 0
private val FLAGS = 0

/**
 * Cancels all notifications.
 *
 */
fun NotificationManager.cancelNotifications() {
    cancelAll()
}

fun setNotification(
    applicationContext: Context,
    messageBody: String,
    extras: String,
    tone: Uri
): Notification {
    val notificationIntent = Intent(applicationContext, AlarmMathActivity::class.java)
    notificationIntent.putExtra(ALARM_EXTRA, extras)
    notificationIntent.flags = Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP
    val notificationPendingIntent = PendingIntent.getActivity(
        applicationContext,
        NOTIFICATION_ID,
        notificationIntent,
        FLAGS
    )
    val vibratePattern = longArrayOf(0, 100, 200, 300)
    val alarmImage = BitmapFactory.decodeResource(
        applicationContext.resources,
        R.drawable.icon
    )
    val bigPicStyle = NotificationCompat.BigPictureStyle()
        .bigPicture(alarmImage)
        .bigLargeIcon(null)
    val notification: Notification
    when {
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.O -> {
            // Create the NotificationChannel, but only on API 26+ because
            // the NotificationChannel class is new and not in the support library
            val name: CharSequence = applicationContext.getString(R.string.notification_title)
            val importance = NotificationManager.IMPORTANCE_HIGH
            val CHANNEL_ID = applicationContext.getString(R.string.alarm_notification_channel_id)
            val channel = NotificationChannel(CHANNEL_ID, name, importance)
            val audioAttributes = AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                .build()
            channel.apply {
                setShowBadge(false)
                enableLights(false)
                lightColor = Color.RED
                setSound(tone, audioAttributes)
                enableVibration(true)
                description =  applicationContext.getString(R.string.math_alarm)
            }
            val notificationBuilder = NotificationCompat.Builder(applicationContext, CHANNEL_ID)

            applicationContext.getSystemService(NotificationManager::class.java).createNotificationChannel(
                channel
            )
            notification = notificationBuilder //the log is PNG file format with a transparent background
                .setSmallIcon(R.drawable.icon)
                .setContentTitle(applicationContext.getString(R.string.notification_title))
                .setContentText(messageBody)
                .setFullScreenIntent(notificationPendingIntent, true)
                .setAutoCancel(true)
                .setStyle(bigPicStyle)
                .setLargeIcon(alarmImage)
                .setVisibility(VISIBILITY_PUBLIC)
                .setSound(tone, AudioManager.STREAM_ALARM)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setVibrate(vibratePattern)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build()
        }
        Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP -> {
            notification = NotificationCompat.Builder(
                applicationContext,
                applicationContext.getString(R.string.alarm_notification_channel_id)
            ) // to be defined in the MainActivity of the app
                .setSmallIcon(R.drawable.icon)
                .setContentTitle(applicationContext.getString(R.string.notification_title))
                .setContentText(messageBody)
                .setFullScreenIntent(notificationPendingIntent, true)
                .setAutoCancel(true)
                .setStyle(bigPicStyle)
                .setLargeIcon(alarmImage)
                .setVisibility(VISIBILITY_PUBLIC)
                .setSound(tone, AudioManager.STREAM_ALARM)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setVibrate(vibratePattern)
                .setPriority(NotificationCompat.PRIORITY_HIGH).build()
        }
        else -> {
            notification = NotificationCompat.Builder(
                applicationContext,
                applicationContext.getString(R.string.alarm_notification_channel_id)
            ) // to be defined in the MainActivity of the app
                .setSmallIcon(R.drawable.icon)
                .setContentTitle(applicationContext.getString(R.string.notification_title))
                .setContentText(messageBody)
                .setFullScreenIntent(notificationPendingIntent, true)
                .setAutoCancel(true)
                .setStyle(bigPicStyle)
                .setLargeIcon(alarmImage)
                .setVisibility(VISIBILITY_PUBLIC)
                .setSound(tone, AudioManager.STREAM_ALARM)
                .setCategory(NotificationCompat.CATEGORY_ALARM)
                .setVibrate(vibratePattern)
                .setPriority(NotificationCompat.PRIORITY_HIGH).build()
        }
    }
    return notification
}
