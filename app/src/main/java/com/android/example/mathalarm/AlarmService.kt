package com.android.example.mathalarm

import android.content.Context
import android.content.Intent
import androidx.core.app.JobIntentService
import com.android.example.mathalarm.screens.alarmmath.AlarmMathActivity

class AlarmService: JobIntentService() {

    companion object {
        // Service unique ID
        val SERVICE_JOB_ID = 50


        fun enqueueWork(context: Context, service: Intent) {
            enqueueWork(context, AlarmService::class.java, SERVICE_JOB_ID, service)
        }
    }


    override fun onHandleWork(intent: Intent) {
        onHandleIntent(intent)
    }


    fun onHandleIntent(intent: Intent) {
        val mathActivity = Intent(this, AlarmMathActivity::class.java)
        mathActivity.putExtra(ALARM_EXTRA, intent.extras!![ALARM_EXTRA].toString())
        mathActivity.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(mathActivity)
    }
}