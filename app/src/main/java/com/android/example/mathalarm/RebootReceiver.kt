package com.android.example.mathalarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.android.example.mathalarm.database.Alarm
import com.android.example.mathalarm.database.AlarmDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import org.koin.core.KoinComponent
import org.koin.core.inject

class RebootReceiver : BroadcastReceiver() {
    private val myHelper: MyHelper by lazy { MyHelper() }

    override fun onReceive(context: Context, intent: Intent) {
        myHelper.onReceive(context, intent)
    }
}

class MyHelper : KoinComponent {
    private val dataSource: AlarmDao by inject()
    private val job = Job()
    private val uiScope = CoroutineScope(Dispatchers.Main + job)

    fun onReceive(context: Context, intent: Intent) {
        uiScope.launch {
            val alarms: List<Alarm> = dataSource.getAlarms()
            for (i in alarms.indices) {
                val alarm: Alarm = alarms[i]
                if (alarm.isOn) {
                    scheduleAlarm(context, alarm)
                }
            }
        }
    }
}