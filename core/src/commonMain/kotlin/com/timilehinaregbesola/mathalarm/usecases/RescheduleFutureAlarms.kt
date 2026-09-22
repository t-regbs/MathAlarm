package com.timilehinaregbesola.mathalarm.usecases

import com.timilehinaregbesola.mathalarm.data.AlarmRepository
import com.timilehinaregbesola.mathalarm.domain.model.Alarm
import com.timilehinaregbesola.mathalarm.interactors.AlarmInteractor
import com.timilehinaregbesola.mathalarm.interactors.scheduleOccurrences
import com.timilehinaregbesola.mathalarm.provider.AlarmTimeCalculator
import com.timilehinaregbesola.mathalarm.provider.activeSkippedDate
import com.timilehinaregbesola.mathalarm.provider.remainingOccurrences
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.datetime.TimeZone

class RescheduleFutureAlarms(
    private val alarmRepository: AlarmRepository,
    private val alarmInteractor: AlarmInteractor,
    private val alarmTimeCalculator: AlarmTimeCalculator,
    private val timeZone: () -> TimeZone = { TimeZone.currentSystemDefault() },
) {
    suspend operator fun invoke(clearActive: Boolean = false) {
        restoreAlarms(preservePendingDelivery = false, clearActive = clearActive)
    }

    /** Leave recently due broadcasts time to finish; older missed occurrences still expire. */
    suspend fun onAppResume() {
        restoreAlarms(preservePendingDelivery = true, clearActive = false)
    }

    private suspend fun restoreAlarms(preservePendingDelivery: Boolean, clearActive: Boolean) {
        clearExpiredSkips()
        val zone = timeZone()
        val alarms = alarmRepository.getSavedAlarms().first().filter { it.isOn }
        for (alarm in alarms) {
            val scheduleIsCurrent = alarm.scheduleInitialized &&
                alarm.scheduleError == null && alarm.scheduleTimeZone == zone.id
            val occurrences = alarm.pendingTimes + listOfNotNull(alarm.snoozedUntil)
            val awaitingDelivery = occurrences.any { time ->
                !alarmTimeCalculator.isInFuture(time) &&
                    alarmTimeCalculator.isInFuture(time + DELIVERY_GRACE_MILLIS)
            }
            if (preservePendingDelivery && scheduleIsCurrent && awaitingDelivery) continue
            try {
                restoreAlarm(alarm, clearActive = clearActive)
            } catch (e: CancellationException) {
                throw e
            } catch (_: Exception) {
                // restoreAlarm records the failure; continue restoring the other alarms.
            }
        }
    }

    /** Restore saved occurrences, including a separate snooze, without starting a new cycle. */
    suspend fun restoreAlarm(alarm: Alarm, clearActive: Boolean = false, preserveSnooze: Boolean = false) {
        try {
            val zone = timeZone()
            val normalized = alarm.copy(skippedDate = alarm.activeSkippedDate(alarmTimeCalculator, zone))
            val times = normalized.remainingOccurrences(alarmTimeCalculator, zone)
            // Skip/Undo change only normal occurrences. Leave the OS snooze untouched
            // while it is future or recently due, so queued delivery remains valid.
            val keepSnooze = preserveSnooze && normalized.snoozedUntil?.let {
                alarmTimeCalculator.isInFuture(it + DELIVERY_GRACE_MILLIS)
            } == true
            val snooze = normalized.snoozedUntil?.takeIf { keepSnooze || alarmTimeCalculator.isInFuture(it) }
            val active = if (clearActive) null else normalized.activeAt
            val planned = normalized.copy(
                pendingTimes = times.sorted(),
                snoozedUntil = snooze,
                activeAt = active,
                scheduleInitialized = true,
                scheduleTimeZone = zone.id,
                scheduleError = Alarm.SCHEDULING_IN_PROGRESS,
                isOn = alarm.repeat || times.isNotEmpty() || snooze != null || active != null
            )
            alarmRepository.updateAlarm(planned)
            // Remove old-zone weekday identities before installing the restored schedule.
            if (keepSnooze) alarmInteractor.cancelRegularOccurrences(alarm)
            else alarmInteractor.cancel(alarm)
            alarmInteractor.scheduleOccurrences(planned, times)
            if (snooze != null && !keepSnooze) alarmInteractor.scheduleSnooze(planned, snooze)
            alarmRepository.updateAlarm(planned.copy(scheduleError = null))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            alarmRepository.findAlarm(alarm.alarmId)?.let { latest ->
                alarmRepository.updateAlarm(latest.copy(scheduleError = e.message ?: "Unable to restore alarm"))
            }
            throw e
        }
    }

    /** Disabled alarms can still carry an undoable final occurrence. Expire those too. */
    suspend fun clearExpiredSkips() {
        val zone = timeZone()
        alarmRepository.getSavedAlarms().first().forEach { alarm ->
            if (alarm.skippedDate != null && alarm.activeSkippedDate(alarmTimeCalculator, zone) == null) {
                alarmRepository.updateAlarm(alarm.copy(skippedDate = null))
            }
        }
    }

    private companion object {
        const val DELIVERY_GRACE_MILLIS = 60_000L
    }

}
