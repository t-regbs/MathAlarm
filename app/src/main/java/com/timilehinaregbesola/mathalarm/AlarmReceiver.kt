package com.timilehinaregbesola.mathalarm
/*
* This receives the intent from AlarmManager to start the math fragment
 */
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.timilehinaregbesola.mathalarm.usecases.CompleteAlarm
import com.timilehinaregbesola.mathalarm.usecases.RescheduleFutureAlarms
import com.timilehinaregbesola.mathalarm.usecases.ShowAlarm
import com.timilehinaregbesola.mathalarm.usecases.SnoozeAlarm
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import org.koin.core.KoinComponent
import org.koin.core.inject
import timber.log.Timber

/**
 * [BroadcastReceiver] to be notified by the [android.app.AlarmManager].
 */
class AlarmReceiver : BroadcastReceiver(), KoinComponent {

//    override fun onReceive(context: Context, intent: Intent) {
//        val service = Intent(context, AlarmService::class.java)
//        service.putExtra(ALARM_EXTRA, intent.extras!![ALARM_EXTRA].toString())
//        AlarmService.enqueueWork(context, service)
//    }

    private val completeTaskUseCase: CompleteAlarm by inject()

    private val showAlarmUseCase: ShowAlarm by inject()

    private val snoozeAlarmUseCase: SnoozeAlarm by inject()

    private val rescheduleUseCase: RescheduleFutureAlarms by inject()

    @Suppress("GlobalCoroutineUsage")
    override fun onReceive(context: Context?, intent: Intent?) {
        Timber.d("onReceive() - intent ${intent?.action}")

        GlobalScope.launch {
            handleIntent(intent)
        }
    }

    private suspend fun handleIntent(intent: Intent?) =
        when (intent?.action) {
            ALARM_ACTION -> getAlarmId(intent)?.let { showAlarmUseCase(it) }
            COMPLETE_ACTION -> getAlarmId(intent)?.let { completeTaskUseCase(it) }
            SNOOZE_ACTION -> getAlarmId(intent)?.let { snoozeAlarmUseCase(it) }
            Intent.ACTION_BOOT_COMPLETED -> rescheduleUseCase()
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
