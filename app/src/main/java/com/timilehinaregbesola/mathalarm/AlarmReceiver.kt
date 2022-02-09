package com.timilehinaregbesola.mathalarm
/*
* This receives the intent from AlarmManager to start the math fragment
 */
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.timilehinaregbesola.mathalarm.framework.Usecases
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

/**
 * [BroadcastReceiver] to be notified by the [android.app.AlarmManager].
 */
@AndroidEntryPoint
class AlarmReceiver : BroadcastReceiver() {
    @Inject lateinit var usecases: Usecases

    @Suppress("GlobalCoroutineUsage")
    override fun onReceive(context: Context?, intent: Intent?) {
        Timber.d("onReceive() - intent ${intent?.action}")

        GlobalScope.launch {
            handleIntent(intent)
        }
    }

    private suspend fun handleIntent(intent: Intent?): Unit? {
        return when (intent?.action) {
            ALARM_ACTION -> getAlarmId(intent)?.let { usecases.showAlarm(it) }
            COMPLETE_ACTION -> getAlarmId(intent)?.let { usecases.completeAlarm(it) }
            SNOOZE_ACTION -> getAlarmId(intent)?.let { usecases.snoozeAlarm(it) }
            Intent.ACTION_BOOT_COMPLETED -> {
                Timber.d("Reboot Reboot!!")
                usecases.rescheduleFutureAlarms()
            }
            "android.intent.action.QUICKBOOT_POWERON" -> {
                Timber.d("Rebooted!!")
                usecases.rescheduleFutureAlarms()
            }
            "android.intent.action.MY_PACKAGE_REPLACED" -> usecases.rescheduleFutureAlarms()
            else -> Timber.e("Action not supported")
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
