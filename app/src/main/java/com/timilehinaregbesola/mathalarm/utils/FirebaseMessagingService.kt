package com.timilehinaregbesola.mathalarm.utils

import android.annotation.SuppressLint
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import android.graphics.BitmapFactory
import android.media.RingtoneManager
import android.net.Uri
import android.os.Build
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.core.app.NotificationCompat
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.timilehinaregbesola.mathalarm.BuildConfig
import com.timilehinaregbesola.mathalarm.R
import com.timilehinaregbesola.mathalarm.notification.MathAlarmNotificationChannel
import com.timilehinaregbesola.mathalarm.presentation.MainActivity
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.InternalCoroutinesApi
import javax.inject.Inject

@AndroidEntryPoint
class FirebaseMessagingService : FirebaseMessagingService() {
    @Inject
    lateinit var channel: MathAlarmNotificationChannel
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        if (remoteMessage.data.isNotEmpty()) {
            val updateApp = remoteMessage.data["updateApp"]!!.toInt()
            val versionCode = remoteMessage.data["versionCode"]!!.toInt()
            val title = remoteMessage.data["title"]
            val message = remoteMessage.data["message"]
            if (updateApp == 1) {
                if (BuildConfig.VERSION_CODE < versionCode) {
                    if (title != null && message != null) {
                        sendUpdateApp(title, message)
                    }
                }
            }
        }
    }

    @SuppressLint("UnspecifiedImmutableFlag", "MutableImplicitPendingIntent")
    @OptIn(
        ExperimentalFoundationApi::class,
        ExperimentalMaterial3Api::class,
        ExperimentalMaterial3Api::class,
        ExperimentalComposeUiApi::class,
        ExperimentalComposeUiApi::class,
        InternalCoroutinesApi::class,
        ExperimentalAnimationApi::class
    )
    private fun sendUpdateApp(title: String, body: String) {
        val sharingIntent = Intent()
        sharingIntent.action = Intent.ACTION_VIEW
        sharingIntent.data = Uri.parse("https://play.google.com/store/apps/details?id=$packageName")
        val sendIntent = if (Build.VERSION.SDK_INT >= 34) {
            PendingIntent.getActivity(
                this,
                0,
                sharingIntent,
                PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_MUTABLE or PendingIntent.FLAG_ALLOW_UNSAFE_IMPLICIT_INTENT
            )
        } else if (Build.VERSION.SDK_INT >= 31) {
            PendingIntent.getActivity(
                this,
                0,
                sharingIntent,
                PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_MUTABLE
            )
        } else {
            PendingIntent.getActivity(this, 0, sharingIntent, PendingIntent.FLAG_ONE_SHOT)
        }
        val notificationManager = getNotificationManager()
        val intent = Intent(this, MainActivity::class.java)
        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
        val pendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            PendingIntent.getActivity(
                this,
                0,
                intent,
                PendingIntent.FLAG_ONE_SHOT or PendingIntent.FLAG_MUTABLE
            )
        } else {
            PendingIntent.getActivity(this, 0, intent, PendingIntent.FLAG_ONE_SHOT)
        }
        val soundUri: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            @SuppressLint("WrongConstant") val notificationChannel = NotificationChannel(
                channel.getChannelId(),
                getString(R.string.channel_alarm_name),
                NotificationManager.IMPORTANCE_MAX
            )
            notificationChannel.description = getString(R.string.channel_alarm_name)
            notificationChannel.name = getString(R.string.channel_alarm_name)
            notificationManager!!
                .createNotificationChannel(notificationChannel)
        }
        val notificationBuilder: NotificationCompat.Builder =
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                NotificationCompat.Builder(this, channel.getChannelId())
                    .setSmallIcon(R.drawable.icon)
                    .setLargeIcon(BitmapFactory.decodeResource(resources, R.drawable.icon))
                    .setStyle(
                        NotificationCompat.BigTextStyle()
                            .bigText(body)
                    )
                    .setContentTitle(title)
                    .setContentText(body)
                    .addAction(R.drawable.ic_baseline_add_24, "Update", sendIntent)
                    .setAutoCancel(true)
                    .setSound(soundUri)
                    .setContentIntent(pendingIntent)
                    .setDefaults(Notification.DEFAULT_ALL)
                    .setPriority(NotificationManager.IMPORTANCE_MAX)
                    .setOnlyAlertOnce(true)
                    .setChannelId(channel.getChannelId())
                    .setColor(ContextCompat.getColor(applicationContext, R.color.white))
            } else {
                NotificationCompat.Builder(this, channel.getChannelId())
                    .setSmallIcon(R.drawable.icon)
                    .setLargeIcon(BitmapFactory.decodeResource(resources, R.drawable.icon))
                    .setStyle(
                        NotificationCompat.BigTextStyle()
                            .bigText(body)
                    )
                    .setContentTitle(title)
                    .setContentText(body)
                    .addAction(R.drawable.ic_baseline_add_24, "Update", sendIntent)
                    .setAutoCancel(true)
                    .setSound(soundUri)
                    .setContentIntent(pendingIntent)
                    .setDefaults(Notification.DEFAULT_ALL)
                    .setOnlyAlertOnce(true)
                    .setChannelId(channel.getChannelId())
                    .setColor(ContextCompat.getColor(applicationContext, R.color.white))
            }
        if (notificationManager != null) {
            val id = System.currentTimeMillis().toInt()
            notificationManager.notify(id, notificationBuilder.build())
        }
    }
}
