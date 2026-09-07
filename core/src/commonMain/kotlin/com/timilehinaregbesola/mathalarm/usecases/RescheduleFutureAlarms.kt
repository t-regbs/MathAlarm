package com.timilehinaregbesola.mathalarm.usecases

import com.timilehinaregbesola.mathalarm.data.AlarmRepository
import com.timilehinaregbesola.mathalarm.domain.model.Alarm
import com.timilehinaregbesola.mathalarm.interactors.AlarmInteractor
import com.timilehinaregbesola.mathalarm.interactors.scheduleOccurrences
import com.timilehinaregbesola.mathalarm.provider.AlarmTimeCalculator
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.first
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

class RescheduleFutureAlarms(
    private val alarmRepository: AlarmRepository,
    private val alarmInteractor: AlarmInteractor,
    private val alarmTimeCalculator: AlarmTimeCalculator
) {
    suspend operator fun invoke(clearActive: Boolean = false) {
        restoreAlarms(preservePendingDelivery = false, clearActive = clearActive)
    }

    /** Leave recently due broadcasts time to finish; older missed occurrences still expire. */
    suspend fun onAppResume() {
        restoreAlarms(preservePendingDelivery = true, clearActive = false)
    }

    private suspend fun restoreAlarms(preservePendingDelivery: Boolean, clearActive: Boolean) {
        val zone = TimeZone.currentSystemDefault()
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
    suspend fun restoreAlarm(alarm: Alarm, clearActive: Boolean = false) {
        try {
            val zone = TimeZone.currentSystemDefault()
            val times = remainingTimes(alarm, zone)
            val snooze = alarm.snoozedUntil?.takeIf(alarmTimeCalculator::isInFuture)
            val active = if (clearActive) null else alarm.activeAt
            val planned = alarm.copy(
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
            alarmInteractor.cancel(alarm)
            alarmInteractor.scheduleOccurrences(planned, times)
            if (snooze != null) alarmInteractor.scheduleSnooze(planned, snooze)
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

    private fun remainingTimes(alarm: Alarm, zone: TimeZone): List<Long> {
        if (alarm.repeat || !alarm.scheduleInitialized) {
            return alarmTimeCalculator.calculateAlarmTimes(alarm)
        }
        val previousZone = alarm.scheduleTimeZone?.let(TimeZone::of) ?: zone
        // Preserve the original local dates of one-time occurrences across zone changes.
        return alarm.pendingTimes.map { time ->
            Instant.fromEpochMilliseconds(time).toLocalDateTime(previousZone)
                .toInstant(zone).toEpochMilliseconds()
        }.filter(alarmTimeCalculator::isInFuture)
    }

    private companion object {
        const val DELIVERY_GRACE_MILLIS = 60_000L
    }

}
