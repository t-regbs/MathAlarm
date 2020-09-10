package com.android.example.mathalarm

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.annotation.Nullable
import androidx.core.app.JobIntentService
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.android.example.mathalarm.database.AlarmDao
import com.android.example.mathalarm.utils.ALARM_EXTRA
import com.android.example.mathalarm.utils.setNotification
import kotlinx.coroutines.*
import org.koin.android.ext.android.inject
import timber.log.Timber
import java.util.*


class AlarmService: JobIntentService() {

    companion object {
        // Service unique ID
        const val SERVICE_JOB_ID = 50


        fun enqueueWork(context: Context, service: Intent) {
            enqueueWork(context, AlarmService::class.java, SERVICE_JOB_ID, service)
        }
    }


    override fun onHandleWork(intent: Intent) {
        onHandleIntent(intent)
    }


    private fun onHandleIntent(intent: Intent) {
        Timber.d("service intent")
        val notification: Notification
        val dataSource: AlarmDao by inject()
        val id = intent.extras?.getString(ALARM_EXTRA)!!.toLong()
        val alarm = dataSource.search(id)
        val tone = alarm.alarmTone
        notification = setNotification(
            baseContext, "Time for alarm!!!", intent.extras!![ALARM_EXTRA].toString(), tone.toUri()
        )
        notification.flags = notification.flags or Notification.FLAG_INSISTENT
        val notificationManager = ContextCompat.getSystemService(
            baseContext,
            NotificationManager::class.java
        ) as NotificationManager
        notificationManager.notify(0, notification)

    }

}
