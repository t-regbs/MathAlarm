package com.timilehinaregbesola.mathalarm

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.timilehinaregbesola.mathalarm.domain.model.Alarm
import com.timilehinaregbesola.mathalarm.framework.database.AlarmDao
import com.timilehinaregbesola.mathalarm.framework.database.AlarmMapper
import com.timilehinaregbesola.mathalarm.utils.scheduleAlarm
import kotlinx.coroutines.*
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

class RebootReceiver : BroadcastReceiver() {
    private val myHelper: MyHelper by lazy { MyHelper() }
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action.equals("android.intent.action.BOOT_COMPLETED") ||
            intent.action.equals("android.intent.action.QUICKBOOT_POWERON")
        ) {
            myHelper.onReceive(context, intent)
        }
    }
}

class MyHelper : KoinComponent {
    private val dataSource: AlarmDao by inject()

    fun onReceive(context: Context, intent: Intent) {
        GlobalScope.launch(Dispatchers.IO) {
            val mapper = AlarmMapper()
            val alarms: List<Alarm> = mapper.toDomainList(dataSource.getActiveAlarms(true))
            for (i in alarms.indices) {
                val alarm: Alarm = alarms[i]
                alarm.scheduleAlarm(context, false)
            }
        }
    }
}
