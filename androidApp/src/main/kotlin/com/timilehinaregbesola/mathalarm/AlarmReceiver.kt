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
    private val scheduler: com.timilehinaregbesola.mathalarm.notification.AlarmNotificationScheduler by inject()
    private val appScope: AppCoroutineScope by inject()

    override fun onReceive(context: Context, intent: Intent) {
        Logger.d("onReceive() - intent ${intent.action}")

        var wakeLock: PowerManager.WakeLock? = null
        if (intent.action == ALARM_ACTION) {
            val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
            val wakelock = powerManager.newWakeLock(
                PowerManager.PARTIAL_WAKE_LOCK,
                "mathalarm:notificationreceiver"
            )
            wakelock.acquire(10_000)
            wakeLock = wakelock
        }
        
        // Keep the broadcast active until persistence and the service handoff finish.
        val pendingResult = goAsync()
        
        appScope.launch {
            try {
                usecases.command { handleIntent(context, intent) }
            } catch (e: Exception) {
                Logger.e("Error handling intent in AlarmReceiver", e)
            } finally {
                wakeLock?.let { if (it.isHeld) it.release() }
                // Release the async result to tell Android we're done
                // Note: pendingResult can be null in test environments
                try {
                    pendingResult?.finish()
                } catch (e: IllegalStateException) {
                    // finish() can throw if already finished - safe to ignore
                    Logger.w("PendingResult already finished", e)
                }
            }
        }
    }

    private suspend fun handleIntent(context: Context, intent: Intent?) {
        when (intent?.action) {
            ALARM_ACTION -> getAlarmId(intent)?.let {
                val trigger = if (intent.hasExtra(EXTRA_TRIGGER_AT)) intent.getLongExtra(EXTRA_TRIGGER_AT, 0) else null
                val alarm = usecases.findAlarm(it) ?: return
                // Pre-migration PendingIntents have no occurrence timestamp. They cannot override
                // the new, persisted schedule after an upgrade or an edit.
                if (trigger == null && alarm.scheduleInitialized) return
                scheduler.consume(intent)
                com.timilehinaregbesola.mathalarm.notification.AlarmDeliveryLog.record(context, "received", it, trigger)
                usecases.showAlarm(it, trigger, intent.getBooleanExtra(EXTRA_SNOOZED, false))
            }
            COMPLETE_ACTION -> getAlarmId(intent)?.let { usecases.completeAlarm(it) }
            SNOOZE_ACTION -> getAlarmId(intent)?.let { usecases.snoozeAlarm(it) }
            DISMISS_ACTION -> {
                // User swiped away the notification - immediately re-show it
                Logger.d("Notification dismissed by user, re-showing alarm")
                getAlarmId(intent)?.let { usecases.showAlarm(it) }
            }
            Intent.ACTION_TIME_CHANGED,
            Intent.ACTION_TIMEZONE_CHANGED,
            Intent.ACTION_BOOT_COMPLETED,
            "android.intent.action.QUICKBOOT_POWERON",
            "android.intent.action.MY_PACKAGE_REPLACED",
            AlarmManager.ACTION_SCHEDULE_EXACT_ALARM_PERMISSION_STATE_CHANGED -> {
                Logger.d("Rescheduling alarms after system event")
                usecases.rescheduleFutureAlarms(clearActive = intent.action == Intent.ACTION_BOOT_COMPLETED ||
                    intent.action == "android.intent.action.QUICKBOOT_POWERON")
            }
            else -> {
                Logger.e("action: ${intent?.action}")
                Logger.e("Action not supported")
            }
        }
    }

    private fun getAlarmId(intent: Intent?) = intent?.getLongExtra(EXTRA_TASK, 0)

    companion object {

        const val EXTRA_TRIGGER_AT = "trigger_at"
        const val EXTRA_SNOOZED = "snoozed"

        const val EXTRA_TASK = "extra_task"

        const val EXTRA_PENDING_INTENT_ID = "extra_pending_intent_id"

        const val ALARM_ACTION = "com.timilehinaregbesola.mathalarm.SET_ALARM"

        const val COMPLETE_ACTION = "com.timilehinaregbesola.mathalarm.SET_COMPLETE"

        const val SNOOZE_ACTION = "com.timilehinaregbesola.mathalarm.SNOOZE"

        const val DISMISS_ACTION = "com.timilehinaregbesola.mathalarm.NOTIFICATION_DISMISSED"
    }
}
