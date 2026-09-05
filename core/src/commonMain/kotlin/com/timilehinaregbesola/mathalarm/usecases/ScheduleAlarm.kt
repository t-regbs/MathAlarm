package com.timilehinaregbesola.mathalarm.usecases

import com.timilehinaregbesola.mathalarm.data.AlarmRepository
import com.timilehinaregbesola.mathalarm.domain.model.Alarm
import com.timilehinaregbesola.mathalarm.interactors.AlarmInteractor
import com.timilehinaregbesola.mathalarm.interactors.scheduleOccurrences
import com.timilehinaregbesola.mathalarm.provider.AlarmTimeCalculator
import kotlinx.coroutines.CancellationException
import kotlinx.datetime.TimeZone

class ScheduleAlarm(
    private val alarmRepository: AlarmRepository,
    private val alarmInteractor: AlarmInteractor,
    private val alarmTimeCalculator: AlarmTimeCalculator
) {
    suspend operator fun invoke(alarm: Alarm, reschedule: Boolean) {
        val saved = if (alarm.alarmId == 0L) alarmRepository.getLatestAlarm()
            else alarmRepository.findAlarm(alarm.alarmId)
        saved ?: return
        val times = alarmTimeCalculator.calculateAlarmTimes(saved).sorted()
        val planned = saved.copy(isOn = times.isNotEmpty(), pendingTimes = times,
            scheduleInitialized = true, snoozedUntil = null, activeAt = null,
            scheduleError = "Scheduling has not completed", scheduleTimeZone = TimeZone.currentSystemDefault().id)
        // Persist the desired occurrences first so interrupted scheduling can be recovered.
        alarmRepository.updateAlarm(planned)
        try {
            if (reschedule) alarmInteractor.cancel(saved)
            alarmInteractor.scheduleOccurrences(planned, times)
            alarmRepository.updateAlarm(planned.copy(scheduleError = null))
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            alarmRepository.updateAlarm(planned.copy(scheduleError = e.message ?: "Unable to schedule alarm"))
            throw e
        }
    }
}
