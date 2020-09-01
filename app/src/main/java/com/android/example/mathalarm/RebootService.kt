package com.android.example.mathalarm

import android.app.IntentService
import android.content.Intent
import com.android.example.mathalarm.database.Alarm
import com.android.example.mathalarm.database.AlarmDao
import org.koin.android.ext.android.inject
import timber.log.Timber

class RebootService : IntentService(RebootService::class.simpleName) {
    private val dataSource: AlarmDao by inject()
//    private val myHelper: MyHelper by lazy { MyHelper() }
    override fun onHandleIntent(intent: Intent?) {
        if (intent?.extras?.get("service_extra") == "Reboot") {
            val alarms: List<Alarm> = dataSource.getActiveAlarms()
            for (i in alarms.indices) {
                val alarm: Alarm = alarms[i]
                Timber.d("alarm id: ${alarm.alarmId}")
                scheduleAlarm(this, alarm)
                Timber.d("Alarm scheduled")
            }
        }

    }
}

//class MyHelper : KoinComponent {
//
//
//    fun onHandleIntent(intent: Intent?, context: Context) {
//
//    }
//}