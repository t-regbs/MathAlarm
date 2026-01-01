package com.timilehinaregbesola.mathalarm
/*
* This receives the intent from AlarmManager to start the math fragment
 */
import android.app.AlarmManager
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.PowerManager
import co.touchlab.kermit.Logger
import com.timilehinaregbesola.mathalarm.coroutines.AppCoroutineScope
import com.timilehinaregbesola.mathalarm.framework.Usecases
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/**
 * [BroadcastReceiver] to be notified by the [android.app.AlarmManager].
 */ 
class AlarmReceiver : BroadcastReceiver(), KoinComponent {
    private val usecases: Usecases by inject()
    private val appScope: AppCoroutineScope by inject()

    override fun onReceive(context: Context, intent: Intent) {
        Logger.d("onReceive() - intent ${intent.action}")

        if (intent.action == ALARM_ACTION) {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            val wakelock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "mathalarm:notificationreceiver"
            )
            wakelock.acquire(3000)
        }
        
        appScope.launch {
            handleIntent(intent)
        }
    }

    private suspend fun handleIntent(intent: Intent?) {
        when (intent?.action) {
            ALARM_ACTION -> getAlarmId(intent)?.let { usecases.showAlarm(it) }
            COMPLETE_ACTION -> getAlarmId(intent)?.let { usecases.completeAlarm(it) }
            SNOOZE_ACTION -> getAlarmId(intent)?.let { usecases.snoozeAlarm(it) }
            Intent.ACTION_BOOT_COMPLETED,
            "android.intent.action.QUICKBOOT_POWERON",
            "android.intent.action.MY_PACKAGE_REPLACED",
            AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED -> {
                Logger.d("Rescheduling alarms after system event")
                usecases.rescheduleFutureAlarms()
            }
            else -> {
                Logger.e("action: ${intent?.action}")
                Logger.e("Action not supported")
            }
        }
    }

    private fun getAlarmId(intent: Intent?) = intent?.getLongExtra(EXTRA_TASK, 0)

    companion object {

        const val EXTRA_TASK = "extra_task"

        const val ALARM_ACTION = "com.timilehinaregbesola.mathalarm.SET_ALARM"

        const val COMPLETE_ACTION = "com.timilehinaregbesola.mathalarm.SET_COMPLETE"

        const val SNOOZE_ACTION = "com.timilehinaregbesola.mathalarm.SNOOZE"
    }
}
