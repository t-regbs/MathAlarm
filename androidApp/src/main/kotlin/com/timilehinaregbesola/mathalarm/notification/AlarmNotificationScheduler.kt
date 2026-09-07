package com.timilehinaregbesola.mathalarm.notification

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.net.toUri
import co.touchlab.kermit.Logger
import com.timilehinaregbesola.mathalarm.AlarmReceiver
import com.timilehinaregbesola.mathalarm.domain.model.Alarm
import com.timilehinaregbesola.mathalarm.utils.cancelAlarm
import com.timilehinaregbesola.mathalarm.utils.setExactAlarm
import com.timilehinaregbesola.mathalarm.utils.toIndex
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

/** Stable, URI-based identities; mutable time and integer hashes are not alarm identities. */
class AlarmNotificationScheduler(
    private val context: Context,
    private val logger: Logger,
    private val idGenerator: PendingIntentIdGenerator = PendingIntentIdGenerator()
) {
    private val occurrences = context.getSharedPreferences("scheduled_alarm_occurrences", Context.MODE_PRIVATE)

    fun scheduleAlarm(alarm: Alarm, timeInMillis: Long) = schedule(alarm, timeInMillis, false)
    fun scheduleSnooze(alarm: Alarm, timeInMillis: Long) = schedule(alarm, timeInMillis, true)

    private fun schedule(alarm: Alarm, timeInMillis: Long, snooze: Boolean) {
        val day = Instant.fromEpochMilliseconds(timeInMillis).toLocalDateTime(TimeZone.currentSystemDefault()).dayOfWeek.toIndex()
        val intent = occurrenceIntent(alarm.alarmId, if (snooze) "snooze" else "day/$day").apply {
            putExtra(AlarmReceiver.EXTRA_TRIGGER_AT, timeInMillis)
            putExtra(AlarmReceiver.EXTRA_SNOOZED, snooze)
        }
        val pending = PendingIntent.getBroadcast(context, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        try {
            context.setExactAlarm(timeInMillis, pending)
            check(occurrences.edit().putLong(intent.dataString, timeInMillis).commit()) { "Cannot persist scheduled occurrence" }
            AlarmDeliveryLog.record(context, "scheduled", alarm.alarmId, timeInMillis)
        } catch (e: Exception) {
            context.cancelAlarm(pending)
            pending.cancel()
            occurrences.edit().remove(intent.dataString).commit()
            AlarmDeliveryLog.record(context, "schedule_failed", alarm.alarmId, timeInMillis, e.message)
            throw e
        }
    }

    @OptIn(
        androidx.compose.animation.ExperimentalAnimationApi::class,
        androidx.compose.foundation.ExperimentalFoundationApi::class,
        androidx.compose.material3.ExperimentalMaterial3Api::class,
        androidx.compose.ui.ExperimentalComposeUiApi::class,
        kotlinx.coroutines.InternalCoroutinesApi::class
    )
    fun updateAlarm(alarm: Alarm) {
        AlarmService.updateAlarm(context, alarm)
        logger.d("Updated playback metadata for alarm ${alarm.alarmId}")
    }

    fun hasPendingOccurrence(alarm: Alarm): Boolean {
        val prefix = "mathalarm://alarm/${alarm.alarmId}/"
        return occurrences.all.any { (key, value) -> key.startsWith(prefix) && (value as? Long ?: 0) > System.currentTimeMillis() }
    }

    fun cancelSnooze(alarm: Alarm) = cancel(occurrenceIntent(alarm.alarmId, "snooze"))

    fun cancelAlarm(alarm: Alarm) {
        for (day in 0..6) cancel(occurrenceIntent(alarm.alarmId, "day/$day"))
        cancelSnooze(alarm)
        // Cancel identities from earlier releases for the saved time. Orphan legacy broadcasts
        // from older edits are also rejected by the receiver after occurrence-state migration.
        val legacy = Intent(context, AlarmReceiver::class.java).setAction(AlarmReceiver.ALARM_ACTION)
        cancel(legacy, idGenerator.generateSimpleId(alarm.alarmId))
        for (day in 0..6) cancel(legacy, idGenerator.generateId(alarm, day))
        AlarmDeliveryLog.record(context, "canceled", alarm.alarmId)
    }

    fun consume(intent: Intent) {
        val key = intent.dataString ?: return
        val trigger = intent.getLongExtra(AlarmReceiver.EXTRA_TRIGGER_AT, Long.MIN_VALUE)
        // A delayed/duplicate delivery must not cancel a newer occurrence using the same identity.
        if (occurrences.getLong(key, Long.MIN_VALUE) == trigger) cancel(intent)
    }

    private fun occurrenceIntent(id: Long, slot: String) = Intent(context, AlarmReceiver::class.java).apply {
        action = AlarmReceiver.ALARM_ACTION
        data = "mathalarm://alarm/$id/$slot".toUri()
        putExtra(AlarmReceiver.EXTRA_TASK, id)
    }

    private fun cancel(intent: Intent, requestCode: Int = 0) {
        PendingIntent.getBroadcast(context, requestCode, intent,
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)?.let {
            context.cancelAlarm(it)
            it.cancel()
        }
        intent.dataString?.let { occurrences.edit().remove(it).commit() }
    }
}
