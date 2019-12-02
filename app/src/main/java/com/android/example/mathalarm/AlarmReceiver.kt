package com.android.example.mathalarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.android.example.mathalarm.database.Alarm

class AlarmReceiver: BroadcastReceiver() {
    override fun onReceive(p0: Context?, p1: Intent?) {
//        val service = Intent(context, AlarmService::class.java)
//        service.putExtra(Alarm.ALARM_EXTRA, intent.getExtras().get(Alarm.ALARM_EXTRA).toString())
//        AlarmService.enqueueWork(context, service)
   }
}