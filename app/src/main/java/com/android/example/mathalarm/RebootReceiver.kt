package com.android.example.mathalarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.android.example.mathalarm.database.Alarm
import com.android.example.mathalarm.database.AlarmDao
import com.android.example.mathalarm.database.AlarmDatabase
import com.android.example.mathalarm.utils.scheduleAlarm
import kotlinx.coroutines.*
import org.koin.core.KoinComponent
import org.koin.core.inject
import timber.log.Timber

class RebootReceiver : BroadcastReceiver() {
    private val myHelper: MyHelper by lazy { MyHelper() }
    override fun onReceive(context: Context, intent: Intent) {
//        val service = Intent(context, RebootService::class.java)
//        service.putExtra("service_extra", "Reboot")
//        context.startService(service)
        Timber.d("onReceive entered")
        myHelper.onReceive(context, intent)
//    val dataSource = AlarmDatabase.getInstance(context.applicationContext).alarmDatabaseDao
//    if (intent.action == "android.intent.action.BOOT_COMPLETED") {
//        val alarms: List<Alarm> = dataSource.getActiveAlarms()
//        for (i in alarms.indices) {
//            val alarm: Alarm = alarms[i]
//            if (alarm.isOn) {
//                alarm.scheduleAlarm(context)
//                Timber.d("alarm scheduled")
//            }
//        }
//    }
    }
}

class MyHelper : KoinComponent {
    private val dataSource: AlarmDao by inject()
    val job = Job()
    val scope = Dispatchers.Main + job

    fun onReceive(context: Context, intent: Intent) {
        CoroutineScope(scope).launch {
            val alarms: List<Alarm> = dataSource.getActiveAlarms()
            for (i in alarms.indices) {
                val alarm: Alarm = alarms[i]
                if (alarm.isOn) {
                    alarm.scheduleAlarm(context)
                }
            }
        }

    }
}