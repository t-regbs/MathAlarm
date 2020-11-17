package com.timilehinaregbesola.mathalarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.room.Room
import com.timilehinaregbesola.mathalarm.database.Alarm
import com.timilehinaregbesola.mathalarm.database.AlarmDatabase
import com.timilehinaregbesola.mathalarm.utils.scheduleAlarm
import kotlinx.coroutines.*

class RebootReceiver : BroadcastReceiver() {
//    private val myHelper: MyHelper by lazy { MyHelper() }
    override fun onReceive(context: Context, intent: Intent) {
//        startKoin {
//            androidLogger(Level.ERROR)
//            androidContext(context)
//            modules(listOf(databaseModule, repositoryModule, viewModelModule))
//        }

//        val service = Intent(context, RebootService::class.java)
//        service.putExtra("service_extra", "Reboot")
//        RebootService.enqueueWork(context, service)
        if (intent.action.equals("android.intent.action.BOOT_COMPLETED")) {
//            myHelper.onReceive(context, intent)
            GlobalScope.launch(Dispatchers.IO) {
                val database = Room.databaseBuilder(
                    context,
                    AlarmDatabase::class.java,
                    "alarm_history_database"
                ).build()
                val dataSource = database.alarmDatabaseDao
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
}

// class MyHelper : KoinComponent {
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
//    }
// }
