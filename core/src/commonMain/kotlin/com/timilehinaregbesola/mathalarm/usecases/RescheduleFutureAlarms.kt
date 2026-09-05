package com.timilehinaregbesola.mathalarm.usecases

import com.timilehinaregbesola.mathalarm.data.AlarmRepository
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
        val zone = TimeZone.currentSystemDefault()
        for (alarm in alarmRepository.getSavedAlarms().first().filter { it.isOn }) {
            try {
                val times = if (alarm.repeat || !alarm.scheduleInitialized) {
                    alarmTimeCalculator.calculateAlarmTimes(alarm)
                } else {
                    // Keep the original local dates of one-time occurrences across zone changes.
                    alarm.pendingTimes.map { time ->
                        val previousZone = alarm.scheduleTimeZone?.let(TimeZone::of) ?: zone
                        Instant.fromEpochMilliseconds(time).toLocalDateTime(previousZone)
                            .toInstant(zone).toEpochMilliseconds()
                    }.filter(alarmTimeCalculator::isInFuture)
                }
                val snooze = alarm.snoozedUntil?.takeIf(alarmTimeCalculator::isInFuture)
                val planned = alarm.copy(pendingTimes = times.sorted(), snoozedUntil = snooze,
                    activeAt = if (clearActive) null else alarm.activeAt,
                    scheduleInitialized = true, scheduleTimeZone = zone.id, scheduleError = "Scheduling has not completed",
                    isOn = alarm.repeat || times.isNotEmpty() || snooze != null || (alarm.activeAt != null && !clearActive))
                alarmRepository.updateAlarm(planned)
                // Also remove old-zone weekday identities before installing the new schedule.
                alarmInteractor.cancel(alarm)
                alarmInteractor.scheduleOccurrences(planned, times)
                if (snooze != null) alarmInteractor.scheduleSnooze(planned, snooze)
                alarmRepository.updateAlarm(planned.copy(scheduleError = null))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                val latest = alarmRepository.findAlarm(alarm.alarmId) ?: continue
                alarmRepository.updateAlarm(latest.copy(scheduleError = e.message ?: "Unable to restore alarm"))
            }
        }
    }
}
