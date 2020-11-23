package com.timilehinaregbesola.mathalarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.timilehinaregbesola.mathalarm.database.Alarm
import com.timilehinaregbesola.mathalarm.database.AlarmDao
import com.timilehinaregbesola.mathalarm.utils.scheduleAlarm
import kotlinx.coroutines.*
import org.koin.core.KoinComponent
import org.koin.core.inject

class RebootReceiver : BroadcastReceiver() {
    private val myHelper: MyHelper by lazy { MyHelper() }
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action.equals("android.intent.action.BOOT_COMPLETED")) {
            myHelper.onReceive(context, intent)
//            val service = Intent(context, RebootService::class.java)
//            service.putExtra("service_extra", "Reboot")
//            RebootService.enqueueWork(context, service)
        }
    }
}

class MyHelper : KoinComponent {
    private val dataSource: AlarmDao by inject()

    fun onReceive(context: Context, intent: Intent) {
        GlobalScope.launch(Dispatchers.IO) {
            val alarms: List<Alarm> = dataSource.getActiveAlarms(true)
            for (i in alarms.indices) {
                val alarm: Alarm = alarms[i]
                alarm.scheduleAlarm(context)
            }
        }
    }
}
