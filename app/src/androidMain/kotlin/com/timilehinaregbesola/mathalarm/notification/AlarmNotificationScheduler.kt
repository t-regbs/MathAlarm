package com.timilehinaregbesola.mathalarm.notification

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import co.touchlab.kermit.Logger
import com.timilehinaregbesola.mathalarm.AlarmReceiver
import com.timilehinaregbesola.mathalarm.AlarmReceiver.Companion.ALARM_ACTION
import com.timilehinaregbesola.mathalarm.AlarmReceiver.Companion.EXTRA_TASK
import com.timilehinaregbesola.mathalarm.domain.model.Alarm
import com.timilehinaregbesola.mathalarm.utils.cancelAlarm
import com.timilehinaregbesola.mathalarm.utils.fullDays
import com.timilehinaregbesola.mathalarm.utils.setExactAlarm
import com.timilehinaregbesola.mathalarm.utils.toIndex
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

/**
 * Alarm manager to schedule an event based on the time from a Alarm.
 */
class AlarmNotificationScheduler(
    private val context: Context,
    private val logger: Logger,
    private val idGenerator: PendingIntentIdGenerator = PendingIntentIdGenerator()
) {

    /**
     * Schedules a single alarm notification at the specified time.
     *
     * @param alarm the alarm to schedule
     * @param timeInMillis the exact time to trigger the alarm
     */
    fun scheduleAlarm(alarm: Alarm, timeInMillis: Long) {
        logger.d("Scheduling alarm: id=${alarm.alarmId}, time=$timeInMillis")
        
        val alarmIntent = createAlarmIntent(alarm)
        
        val tz = TimeZone.currentSystemDefault()
        val alarmDateTime = Instant.fromEpochMilliseconds(timeInMillis)
            .toLocalDateTime(tz)
        val dayIndex = alarmDateTime.dayOfWeek.toIndex()
        
        val intentId = idGenerator.generateId(alarm, dayIndex)
        
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            intentId,
            alarmIntent,
            PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        context.setExactAlarm(timeInMillis, pendingIntent)
        logger.d("Alarm scheduled successfully: id=${alarm.alarmId} at $timeInMillis (day=${fullDays[dayIndex]}, intentId=$intentId)")
    }

    /**
     * Updates an existing alarm notification.
     *
     * @param alarm the alarm to update
     */
    fun updateAlarm(alarm: Alarm) {
        logger.d("Update alarm called for id=${alarm.alarmId} - no action needed on Android")
        // On Android, the notification will trigger a BroadcastReceiver which will always get the
        // most recent Alarm data from the database, so no action needed here.
    }

    /**
     * Cancels all scheduled notifications for an alarm.
     *
     * @param alarm alarm to be canceled
     */
    fun cancelAlarm(alarm: Alarm) {
        logger.d("Canceling alarm: id=${alarm.alarmId}")

        val receiverIntent = createAlarmIntent(alarm)

        // Cancel the base alarm (for backward compatibility with old alarms)
        cancelAlarmWithId(receiverIntent, idGenerator.generateSimpleId(alarm.alarmId))

        // Cancel day-specific alarms for all days (0-6)
        // We cancel all days, not just those marked 'T', because an alarm may have been
        // edited and we need to ensure all previously scheduled instances are removed
        for (i in 0..6) {
            val intentId = idGenerator.generateId(alarm, i)
            cancelAlarmWithId(receiverIntent, intentId)
            logger.d("Canceled alarm for day $i (${fullDays[i]})")
        }

        logger.d("Alarm canceled: id=${alarm.alarmId}")
    }

    private fun createAlarmIntent(alarm: Alarm): Intent {
        return Intent(context, AlarmReceiver::class.java).apply {
            action = ALARM_ACTION
            putExtra(EXTRA_TASK, alarm.alarmId)
        }
    }

    private fun cancelAlarmWithId(intent: Intent, intentId: Int) {
        val pendingIntent = PendingIntent.getBroadcast(
            context,
            intentId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        context.cancelAlarm(pendingIntent)
        pendingIntent.cancel()
    }
}
