package com.timilehinaregbesola.mathalarm

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import androidx.core.app.JobIntentService
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.timilehinaregbesola.mathalarm.framework.database.AlarmDao
import com.timilehinaregbesola.mathalarm.framework.database.AlarmMapper
import com.timilehinaregbesola.mathalarm.utils.ALARM_EXTRA
import com.timilehinaregbesola.mathalarm.utils.NOTIFICATION_ID
import com.timilehinaregbesola.mathalarm.utils.scheduleAlarm
import com.timilehinaregbesola.mathalarm.utils.setNotification
import org.koin.android.ext.android.inject
import timber.log.Timber

class AlarmService : JobIntentService() {

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
        val mapper: AlarmMapper by inject()
        val id = intent.extras?.getString(ALARM_EXTRA)?.toLong()
        val entity = dataSource.search(id)
        entity?.let {
            val alarm = mapper.mapToDomainModel(entity)
            val tone = alarm.alarmTone
            notification = setNotification(
                applicationContext,
                "Time for alarm!!!",
                (
                    intent.extras
                        ?: throw NullPointerException("Expression 'intent.extras' must not be null")
                    )[ALARM_EXTRA].toString(),
                tone.toUri()
            )
            notification.flags = notification.flags or Notification.FLAG_INSISTENT
            val notificationManager = ContextCompat.getSystemService(
                applicationContext,
                NotificationManager::class.java
            ) as NotificationManager
            notificationManager.notify(id!!.toInt() + NOTIFICATION_ID, notification)
            if (alarm.repeat) {
                alarm.scheduleAlarm(applicationContext, true)
            }
        }
    }
}
