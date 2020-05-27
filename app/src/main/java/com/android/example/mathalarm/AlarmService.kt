package com.android.example.mathalarm

import android.content.Context
import android.content.Intent
import androidx.core.app.JobIntentService
import com.android.example.mathalarm.screens.alarmmath.AlarmMathActivity
import com.android.example.mathalarm.screens.alarmmath.AlarmMathFragment

class AlarmService: JobIntentService() {

    companion object {
        // Service unique ID
        val SERVICE_JOB_ID = 50


        fun enqueueWork(context: Context?, service: Intent?) {
            enqueueWork(context!!, AlarmService::class.java, SERVICE_JOB_ID, service!!)
        }
    }


    override fun onHandleWork(intent: Intent) {
        onHandleIntent(intent)
    }


    fun onHandleIntent(intent: Intent) {
        val mathFragment = Intent(this, AlarmMathActivity::class.java)
        mathFragment.putExtra(
            ALARM_EXTRA,
            intent.extras!![ALARM_EXTRA].toString()
        )
        mathFragment.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        startActivity(mathFragment)

    }
}