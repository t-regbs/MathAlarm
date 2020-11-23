package com.timilehinaregbesola.mathalarm

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import androidx.core.app.JobIntentService
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.timilehinaregbesola.mathalarm.database.AlarmDao
import com.timilehinaregbesola.mathalarm.utils.ALARM_EXTRA
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
        val alarmManager = baseContext.getSystemService(Context.ALARM_SERVICE) as AlarmManager
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
//        if (alarm.repeat) {
//            val alarmClockInfo = AlarmManager.AlarmClockInfo(cal.timeInMillis, pendingIntent)
//            alarmManager.setAlarmClock(alarmClockInfo, pendingIntent)
//            alarmManager.setRepeating(
//                AlarmManager.RTC_WAKEUP, cal.timeInMillis,
//                AlarmManager.INTERVAL_DAY * 7, pendingIntent
//            )
//        }
    }
}
