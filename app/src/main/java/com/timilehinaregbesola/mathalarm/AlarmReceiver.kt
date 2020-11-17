package com.timilehinaregbesola.mathalarm
/*
* This receives the intent from AlarmManager to start the math fragment
 */
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.timilehinaregbesola.mathalarm.utils.ALARM_EXTRA

class AlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        val service = Intent(context, AlarmService::class.java)
        service.putExtra(ALARM_EXTRA, intent.extras!![ALARM_EXTRA].toString())
        AlarmService.enqueueWork(context, service)
    }
}
