package com.timilehinaregbesola.mathalarm.presentation.alarmlist

import androidx.compose.runtime.snapshotFlow
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.timilehinaregbesola.mathalarm.analytics.AnalyticsEvents
import com.timilehinaregbesola.mathalarm.analytics.AnalyticsTracker
import com.timilehinaregbesola.mathalarm.analytics.NoopAnalyticsTracker
import com.timilehinaregbesola.mathalarm.analytics.trackSafely
import com.timilehinaregbesola.mathalarm.domain.model.Alarm
import com.timilehinaregbesola.mathalarm.framework.Usecases
import com.timilehinaregbesola.mathalarm.framework.app.permission.AlarmPermission
import com.timilehinaregbesola.mathalarm.presentation.appsettings.AlarmPreferences.AlarmSortOrder.TIME
import com.timilehinaregbesola.mathalarm.presentation.appsettings.AlarmPreferencesImpl
import com.timilehinaregbesola.mathalarm.utils.AlarmErrorMessage
import com.timilehinaregbesola.mathalarm.utils.UiEvent
import com.timilehinaregbesola.mathalarm.utils.UiEvent.Navigate
import com.timilehinaregbesola.mathalarm.utils.UiEvent.ShowSnackbar
import com.timilehinaregbesola.mathalarm.utils.UiEvent.SnackbarAction
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class AlarmListViewModel(
    private val usecases: Usecases,
    val permission: AlarmPermission,
    private val preferences: AlarmPreferencesImpl,
    private val logger: Logger,
    private val analytics: AnalyticsTracker = NoopAnalyticsTracker,
) : ViewModel() {
    val alarms = usecases
        .getSavedAlarms()
        .combine(
            snapshotFlow { preferences.alarmSortOrderState.value }
        ) { alarms, sortOrder ->
            if (sortOrder == TIME) {
                alarms.sortedWith(
                    compareBy<Alarm> { it.hour }
                        .thenBy { it.minute }
                        .thenByDescending { it.alarmId }
                )
            } else {
                alarms
            }
        }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), null)

    private val _uiEvent = Channel<UiEvent>()
    val uiEvent = _uiEvent.receiveAsFlow()

    private var recentlyDeletedAlarm: Alarm? = null
    private fun launchCommand(block: suspend Usecases.() -> Unit) {
        viewModelScope.launch {
            try {
                usecases.command(block)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logger.e(e) { "Alarm command failed" }
                sendUiEvent(UiEvent.ShowError(AlarmErrorMessage.UPDATE))
            }
        }
    }

    fun onEvent(event: AlarmListEvent) {
        when (event) {
            is AlarmListEvent.OnEditAlarmClick -> sendUiEvent(Navigate(event.alarm))
            is AlarmListEvent.OnAddAlarmClick -> sendUiEvent(Navigate(Alarm()))
            is AlarmListEvent.OnAlarmOnChange -> setEnabled(event.alarm, event.isOn)
            is AlarmListEvent.OnSkipNextClick -> launchCommand {
                val skippedDate = skipNextAlarm(event.alarmId) ?: return@launchCommand
                analytics.trackSafely(AnalyticsEvents.alarmSkipped)
                sendUiEvent(ShowSnackbar(
                    message = "",
                    skippedDate = skippedDate,
                    actionType = SnackbarAction.UNDO_SKIP,
                    relatedAlarmId = event.alarmId,
                ))
            }
            is AlarmListEvent.OnUndoSkipClick -> launchCommand {
                skipNextAlarm.undo(event.alarmId, event.skippedDate)
            }
            is AlarmListEvent.OnUndoDeleteClick -> launchCommand {
                val restored = recentlyDeletedAlarm ?: return@launchCommand
                addAlarm(restored)
                if (restored.isOn) rescheduleFutureAlarms.restoreAlarm(restored, clearActive = true)
                recentlyDeletedAlarm = null
            }
            is AlarmListEvent.OnDeleteAlarmClick -> launchCommand {
                val latest = findAlarm(event.alarm.alarmId) ?: return@launchCommand
                deleteAlarm(latest)
                recentlyDeletedAlarm = latest
                sendUiEvent(ShowSnackbar(
                    message = "Alarm Deleted",
                    action = "Undo",
                    actionType = SnackbarAction.UNDO_DELETE,
                ))
            }
            is AlarmListEvent.DeleteTestAlarm -> launchCommand { deleteAlarm(event.alarmId) }
            is AlarmListEvent.OnClearAlarmsClick -> launchCommand { clearAlarms(getSavedAlarms().first()) }
            AlarmListEvent.OnClearEmptyAlarmsClick -> sendUiEvent(ShowSnackbar("There are no alarms to clear"))
        }
    }

    fun setEnabled(alarm: Alarm, enabled: Boolean) = launchCommand {
        val latest = findAlarm(alarm.alarmId) ?: return@launchCommand
        if (enabled) {
            scheduleAlarm(latest, true)
        } else {
            cancelAlarm(latest)
            updateAlarm(latest.copy(
                isOn = false,
                pendingTimes = emptyList(),
                snoozedUntil = null,
                activeAt = null,
                skippedDate = null,
                scheduleError = null
            ))
        }
    }

    private fun sendUiEvent(event: UiEvent) {
        viewModelScope.launch {
            _uiEvent.send(event)
        }
    }

    fun scheduleAlarm(alarm: Alarm, reschedule: Boolean, message: String) = launchCommand {
        scheduleAlarm(alarm, reschedule)
        sendUiEvent(ShowSnackbar(message))
    }

    fun expireSkips() = launchCommand { rescheduleFutureAlarms.clearExpiredSkips() }

    fun cancelAlarm(alarm: Alarm) = setEnabled(alarm, false)
}
