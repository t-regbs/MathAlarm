package com.android.example.mathalarm

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.core.app.JobIntentService
import androidx.core.content.ContextCompat
import androidx.core.net.toUri
import com.android.example.mathalarm.database.AlarmDao
import com.android.example.mathalarm.utils.ALARM_EXTRA
import com.android.example.mathalarm.utils.copyStream
import com.android.example.mathalarm.utils.sendNotification
import kotlinx.coroutines.*
import org.koin.android.ext.android.inject
import java.io.File
import java.io.FileOutputStream
import java.io.IOException


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
//        val mathActivity = Intent(this, AlarmMathActivity::class.java)
//        mathActivity.putExtra(ALARM_EXTRA, intent.extras!![ALARM_EXTRA].toString())
//        mathActivity.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
//        startActivity(mathActivity)
        val dataSource: AlarmDao by inject()
        val id = intent.extras?.getString(ALARM_EXTRA)!!.toLong()
        val alarm = dataSource.search(id)
        val tone = alarm.alarmTone
        val notificationManager = ContextCompat.getSystemService(
            baseContext,
            NotificationManager::class.java
        ) as NotificationManager

        notificationManager.sendNotification(
            baseContext, "Time for alarm!!!", intent.extras!![ALARM_EXTRA].toString(), tone.toUri()
        )
    }

}