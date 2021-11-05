package com.timilehinaregbesola.mathalarm
/*
* This receives the intent from AlarmManager to start the math fragment
 */
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.timilehinaregbesola.mathalarm.framework.Usecases
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import timber.log.Timber

/**
 * [BroadcastReceiver] to be notified by the [android.app.AlarmManager].
 */
class AlarmReceiver : BroadcastReceiver(), KoinComponent {
    private val usecases: Usecases by inject()

    @Suppress("GlobalCoroutineUsage")
    override fun onReceive(context: Context?, intent: Intent?) {
        Timber.d("onReceive() - intent ${intent?.action}")

        GlobalScope.launch {
            handleIntent(intent)
        }
    }

    private suspend fun handleIntent(intent: Intent?) =
        when (intent?.action) {
            ALARM_ACTION -> getAlarmId(intent)?.let { usecases.showAlarm(it) }
            COMPLETE_ACTION -> getAlarmId(intent)?.let { usecases.completeAlarm(it) }
            SNOOZE_ACTION -> getAlarmId(intent)?.let { usecases.snoozeAlarm(it) }
            Intent.ACTION_BOOT_COMPLETED -> usecases.rescheduleFutureAlarms()
            "android.intent.action.QUICKBOOT_POWERON" -> usecases.rescheduleFutureAlarms()
            else -> Timber.e("Action not supported")
        }

    private fun getAlarmId(intent: Intent?) = intent?.getLongExtra(EXTRA_TASK, 0)

    companion object {

        const val EXTRA_TASK = "extra_task"

        const val ALARM_ACTION = "com.timilehinaregbesola.mathalarm.SET_ALARM"

        const val COMPLETE_ACTION = "com.timilehinaregbesola.mathalarm.SET_COMPLETE"

        const val SNOOZE_ACTION = "com.timilehinaregbesola.mathalarm.SNOOZE"
    }
}
