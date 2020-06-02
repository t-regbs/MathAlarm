package com.android.example.mathalarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModelProvider
import com.android.example.mathalarm.database.Alarm
import com.android.example.mathalarm.screens.alarmlist.AlarmFragment
import com.android.example.mathalarm.screens.alarmlist.AlarmListViewModel

class RebootReceiver : BroadcastReceiver() {
    private lateinit var alarmViewModel: AlarmListViewModel

    override fun onReceive(context: Context, intent: Intent) {
        alarmViewModel = ViewModelProvider(AlarmFragment()).get(AlarmListViewModel::class.java)
        if (intent.action == "android.intent.action.BOOT_COMPLETED") {
            val alarms: LiveData<List<Alarm>> = alarmViewModel.alarms
            for (i in alarms.value!!.indices) {
                val alarm: Alarm = alarms.value!![i]
                if (alarm.isOn) {
                    scheduleAlarm(context, alarm)
                }
            }
        }
    }
}