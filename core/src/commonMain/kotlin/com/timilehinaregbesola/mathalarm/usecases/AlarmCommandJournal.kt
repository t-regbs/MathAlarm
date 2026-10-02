package com.timilehinaregbesola.mathalarm.usecases

import com.timilehinaregbesola.mathalarm.data.AlarmRepository
import com.timilehinaregbesola.mathalarm.domain.model.Alarm
import com.timilehinaregbesola.mathalarm.interactors.AlarmInteractor
import com.timilehinaregbesola.mathalarm.interactors.NotificationInteractor

/** Small write-ahead journal in the existing error column; no registration or schema identities change. */
internal object AlarmCommandJournal {
    private const val CLEANUP = "Accepted alarm resolution cleanup is pending\n"
    private const val SNOOZE = "Snooze registration is pending:"

    fun cleanup(previousError: String?): String = CLEANUP + previousError.orEmpty()
    fun needsCleanup(alarm: Alarm): Boolean = alarm.scheduleError?.startsWith(CLEANUP) == true
    fun previousError(alarm: Alarm): String? = alarm.scheduleError?.substringAfter('\n', "")?.takeIf { it.isNotEmpty() }
    fun snooze(time: Long, previousError: String?): String = "$SNOOZE$time\n${previousError.orEmpty()}"
    fun pendingSnooze(alarm: Alarm): Long? = alarm.scheduleError?.takeIf { it.startsWith(SNOOZE) }
        ?.removePrefix(SNOOZE)?.substringBefore('\n')?.toLongOrNull()

    /** The accepted state is already durable. A cleanup failure never changes acceptance. */
    suspend fun cleanupAccepted(
        accepted: Alarm,
        repository: AlarmRepository,
        interactor: AlarmInteractor,
        notifications: NotificationInteractor?,
        onFailure: (Long, Exception) -> Unit,
        onSuccess: (Long) -> Unit,
    ) {
        try {
            interactor.cancelRecovery(accepted)
            notifications?.dismiss(accepted.alarmId)
            repository.updateAlarm(accepted.copy(scheduleError = previousError(accepted)))
            runCatching { onSuccess(accepted.alarmId) }
        } catch (error: Exception) {
            // The marker was written before cleanup and remains for launch reconciliation,
            // even when the native cancellation succeeded but the final write failed.
            runCatching { onFailure(accepted.alarmId, error) }
        }
    }
}
