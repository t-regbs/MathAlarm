package com.timilehinaregbesola.mathalarm.utils

import android.annotation.SuppressLint
import android.app.Notification
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
import kotlinx.coroutines.InternalCoroutinesApi
import org.koin.android.ext.android.inject
import androidx.core.net.toUri

class FirebaseMessagingService : FirebaseMessagingService() {
    val channel: MathAlarmNotificationChannel by inject()
    override fun onMessageReceived(remoteMessage: RemoteMessage) {
        val data = remoteMessage.data
        if (data["updateApp"]?.toIntOrNull() != 1) return
        val versionCode = data["versionCode"]?.toIntOrNull() ?: return
        if (versionCode <= BuildConfig.VERSION_CODE) return
        val title = data["title"] ?: return
        val message = data["message"] ?: return
        sendUpdateApp(title, message)
    }

    @SuppressLint("UnspecifiedImmutableFlag", "MutableImplicitPendingIntent")
    @OptIn(
        ExperimentalFoundationApi::class,
        ExperimentalMaterial3Api::class,
        ExperimentalComposeUiApi::class,
        InternalCoroutinesApi::class,
        ExperimentalAnimationApi::class
    )
    private fun sendUpdateApp(title: String, body: String) {
        val sharingIntent = Intent()
        sharingIntent.action = Intent.ACTION_VIEW
        sharingIntent.data = "https://play.google.com/store/apps/details?id=$packageName".toUri()
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
        val notificationBuilder = NotificationCompat.Builder(this, channel.getUpdateChannelId())
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
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setOnlyAlertOnce(true)
            .setColor(ContextCompat.getColor(applicationContext, R.color.white))

        if (notificationManager != null) {
            val id = System.currentTimeMillis().toInt()
            notificationManager.notify(id, notificationBuilder.build())
        }
    }
}
