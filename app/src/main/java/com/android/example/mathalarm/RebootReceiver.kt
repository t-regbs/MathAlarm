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
//    private val myHelper: MyHelper by lazy { MyHelper() }
    override fun onReceive(context: Context, intent: Intent) {
        val service = Intent(context, RebootService::class.java)
        service.putExtra("service_extra", "Reboot")
        RebootService.enqueueWork(context, service)
    }
}

//class MyHelper : KoinComponent {
//    private val dataSource: AlarmDao by inject()
//    val job = Job()
//    val scope = Dispatchers.Main + job
//
//    fun onReceive(context: Context, intent: Intent) {
//        CoroutineScope(scope).launch {
//            val alarms: List<Alarm> = dataSource.getActiveAlarms()
//            for (i in alarms.indices) {
//                val alarm: Alarm = alarms[i]
//                if (alarm.isOn) {
//                    alarm.scheduleAlarm(context)
//                }
//            }
//        }
//
//    }
//}