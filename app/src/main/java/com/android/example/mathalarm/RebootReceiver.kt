package com.android.example.mathalarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.lifecycle.LiveData
import androidx.lifecycle.ViewModelProvider
import com.android.example.mathalarm.database.Alarm
import com.android.example.mathalarm.database.AlarmDatabase
import com.android.example.mathalarm.screens.MainActivity
import com.android.example.mathalarm.screens.alarmlist.AlarmFragment
import com.android.example.mathalarm.screens.alarmlist.AlarmListViewModel
import com.android.example.mathalarm.screens.alarmlist.AlarmListViewModelFactory

class RebootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val application = context.applicationContext

        //Creating an instance of the ViewModel Factory
        val dataSource = AlarmDatabase.getInstance(application).alarmDatabaseDao
        if (intent.action == "android.intent.action.BOOT_COMPLETED") {
            val alarms: LiveData<List<Alarm>> = dataSource.getAlarms()
            for (i in alarms.value!!.indices) {
                val alarm: Alarm = alarms.value!![i]
                if (alarm.isOn) {
                    scheduleAlarm(context, alarm)
                }
            }
        }
    }
}