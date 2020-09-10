package com.android.example.mathalarm

import android.app.IntentService
import android.content.Context
import android.content.Intent
import androidx.core.app.JobIntentService
import com.android.example.mathalarm.database.Alarm
import com.android.example.mathalarm.database.AlarmDao
import com.android.example.mathalarm.utils.scheduleAlarm
import org.koin.android.ext.android.inject
import timber.log.Timber

class RebootService : JobIntentService() {
    companion object {
        // Service unique ID
        const val SERVICE_JOB_ID = 55


        fun enqueueWork(context: Context, service: Intent) {
            enqueueWork(context, AlarmService::class.java, SERVICE_JOB_ID, service)
        }
    }


    override fun onHandleWork(intent: Intent) {
        onHandleIntent(intent)
    }
    private val dataSource: AlarmDao by inject()
//    private val myHelper: MyHelper by lazy { MyHelper() }
    private fun onHandleIntent(intent: Intent?) {
        if (intent?.extras?.get("service_extra") == "Reboot") {
            val alarms: List<Alarm> = dataSource.getActiveAlarms()
            for (i in alarms.indices) {
                val alarm: Alarm = alarms[i]
                Timber.d("alarm id: ${alarm.alarmId}")
                alarm.scheduleAlarm(this)
                Timber.d("Alarm scheduled")
            }
        }
    }
}

