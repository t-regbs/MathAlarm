package com.timilehinaregbesola.mathalarm.usecases

import com.timilehinaregbesola.mathalarm.data.AlarmRepository
import com.timilehinaregbesola.mathalarm.interactors.NotificationInteractor
import kotlinx.coroutines.CancellationException
import kotlin.time.Clock

class ShowAlarm(
    private val alarmRepository: AlarmRepository,
    private val notificationInteractor: NotificationInteractor,
    private val scheduleNextAlarm: ScheduleNextAlarm,
) {
    suspend operator fun invoke(alarmId: Long, triggerAt: Long? = null, snoozed: Boolean = false) {
        val saved = alarmRepository.findAlarm(alarmId) ?: return
        if (!saved.isOn) return
        if (triggerAt != null && saved.scheduleInitialized) {
            val expected = if (snoozed) saved.snoozedUntil == triggerAt else triggerAt in saved.pendingTimes
            if (!expected && saved.activeAt != triggerAt) return // obsolete or canceled broadcast
        }
        val active = triggerAt ?: saved.activeAt ?: Clock.System.now().toEpochMilliseconds()
        val alarm = saved.copy(activeAt = active,
            pendingTimes = if (triggerAt == null) saved.pendingTimes else saved.pendingTimes - triggerAt,
            snoozedUntil = if (snoozed) null else saved.snoozedUntil)
        alarmRepository.updateAlarm(alarm)
        // Start playback before doing next-week scheduling; rearming failure must not silence today.
        try {
            notificationInteractor.show(alarm)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            alarmRepository.updateAlarm(alarm.copy(scheduleError = e.message ?: "Alarm playback could not start"))
            throw e
        }
        if (alarm.repeat && saved.activeAt != active) {
            try {
                val times = scheduleNextAlarm(alarm)
                alarmRepository.updateAlarm(alarm.copy(pendingTimes = times, scheduleError = null))
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                alarmRepository.updateAlarm(alarm.copy(scheduleError = e.message ?: "Unable to schedule next alarm"))
            }
        }
    }
}
