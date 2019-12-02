package com.android.example.mathalarm

import android.content.Context
import android.content.Intent
import androidx.core.app.JobIntentService
import com.android.example.mathalarm.database.Alarm
import com.android.example.mathalarm.screens.AlarmMathFragment

//class AlarmService: JobIntentService() {
//    // Service unique ID
//    val SERVICE_JOB_ID = 50
//
////    public AlarmService() {
////        super("AlarmService");
////    }
//
//    //    public AlarmService() {
////        super("AlarmService");
////    }
//    fun enqueueWork(context: Context?, service: Intent?) {
//        enqueueWork(context!!, AlarmService::class.java, SERVICE_JOB_ID, service!!)
//    }
//
//    override fun onHandleWork(intent: Intent) {
//        onHandleIntent(intent)
//    }
//
//
//    fun onHandleIntent(intent: Intent) {
//        val mathFragment = Intent(this, AlarmMathFragment::class.java)
//        mathFragment.putExtra(
//            Alarm.ALARM_EXTRA,
//            intent.extras!![Alarm.ALARM_EXTRA].toString()
//        )
//        mathFragment.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
//        startActivity(mathFragment)
//    }
//}