package com.timilehinaregbesola.mathalarm.usecases

import com.timilehinaregbesola.mathalarm.data.AlarmRepository
import com.timilehinaregbesola.mathalarm.domain.model.Alarm
import com.timilehinaregbesola.mathalarm.interactors.AlarmInteractor
import com.timilehinaregbesola.mathalarm.interactors.scheduleOccurrences
import com.timilehinaregbesola.mathalarm.provider.AlarmTimeCalculator
import kotlinx.coroutines.CancellationException
import kotlinx.datetime.TimeZone

@OptIn(kotlin.experimental.ExperimentalObjCRefinement::class)
@kotlin.native.HiddenFromObjC
class ScheduleAlarm(
    private val alarmRepository: AlarmRepository,
    private val alarmInteractor: AlarmInteractor,
    private val alarmTimeCalculator: AlarmTimeCalculator
) {
    suspend operator fun invoke(alarm: Alarm, reschedule: Boolean) {
        val saved = if (alarm.alarmId == 0L) {
            alarmRepository.getLatestAlarm()
        } else {
            alarmRepository.findAlarm(alarm.alarmId)
        } ?: return
        val times = alarmTimeCalculator.calculateAlarmTimes(saved.copy(skippedDate = null)).sorted()
        val planned = saved.copy(
            isOn = times.isNotEmpty(),
            pendingTimes = times,
            scheduleInitialized = true,
            snoozedUntil = null,
            activeAt = null,
            snoozeCount = 0,
            skippedDate = null,
            scheduleError = Alarm.SCHEDULING_IN_PROGRESS,
            scheduleTimeZone = TimeZone.currentSystemDefault().id
        )
        // Persist the desired occurrences first so interrupted scheduling can be recovered.
        alarmRepository.updateAlarm(planned)
        try {
            if (reschedule) {
                // Keep unresolved recovery alive until the replacement schedule is accepted.
                alarmInteractor.cancelRegularOccurrences(saved)
                alarmInteractor.cancelSnooze(saved)
            }
            alarmInteractor.scheduleOccurrences(planned, times)
            alarmInteractor.cancelRecovery(saved)
            alarmRepository.updateAlarm(planned.copy(scheduleError = null))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            alarmRepository.updateAlarm(planned.copy(
                activeAt = saved.activeAt, snoozeCount = saved.snoozeCount, snoozedUntil = saved.snoozedUntil,
                scheduleError = e.message ?: "Unable to schedule alarm"
            ))
            throw e
        }
    }
}
