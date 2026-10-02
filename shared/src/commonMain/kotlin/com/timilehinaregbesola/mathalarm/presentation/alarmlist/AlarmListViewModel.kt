package com.timilehinaregbesola.mathalarm.presentation.alarmlist

import com.timilehinaregbesola.mathalarm.presentation.SharedFeatureViewModel
import com.rickclephas.kmp.observableviewmodel.MutableStateFlow
import com.rickclephas.kmp.observableviewmodel.coroutineScope
import com.rickclephas.kmp.observableviewmodel.stateIn
import com.rickclephas.kmp.nativecoroutines.NativeCoroutinesState
import co.touchlab.kermit.Logger
import com.timilehinaregbesola.mathalarm.analytics.AnalyticsEvents
import com.timilehinaregbesola.mathalarm.analytics.AnalyticsTracker
import com.timilehinaregbesola.mathalarm.analytics.NoopAnalyticsTracker
import com.timilehinaregbesola.mathalarm.analytics.trackSafely
import com.timilehinaregbesola.mathalarm.domain.model.Alarm
import com.timilehinaregbesola.mathalarm.framework.Usecases
import com.timilehinaregbesola.mathalarm.framework.app.permission.AlarmPermission
import com.timilehinaregbesola.mathalarm.platform.supportsSkipNext
import com.timilehinaregbesola.mathalarm.presentation.appsettings.AlarmPreferences.AlarmSortOrder.TIME
import com.timilehinaregbesola.mathalarm.presentation.appsettings.AlarmPreferencesImpl
import com.timilehinaregbesola.mathalarm.utils.AlarmErrorMessage
import com.timilehinaregbesola.mathalarm.utils.UiEvent
import com.timilehinaregbesola.mathalarm.utils.UiEvent.Navigate
import com.timilehinaregbesola.mathalarm.utils.UiEvent.ShowSnackbar
import com.timilehinaregbesola.mathalarm.utils.UiEvent.SnackbarAction
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AlarmListViewModel internal constructor(
    private val usecases: Usecases,
    private val permission: AlarmPermission,
    private val preferences: AlarmPreferencesImpl,
    private val logger: Logger,
    private val analytics: AnalyticsTracker = NoopAnalyticsTracker,
    private val skipNextSupported: Boolean = supportsSkipNext(),
) : SharedFeatureViewModel() {
    val canSchedule: Boolean get() = permission.hasExactAlarmPermission()
    internal val alarms = usecases
        .getSavedAlarms()
        .combine(
            preferences.alarmSortOrderState
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

    private val _state = MutableStateFlow(viewModelScope, AlarmListState())
    @NativeCoroutinesState
    val state: kotlinx.coroutines.flow.StateFlow<AlarmListState> = _state.asStateFlow()
    private var resultId = 0L
    init {
        viewModelScope.coroutineScope.launch {
            alarms.collect { value -> _state.value = _state.value.copy(alarms = value.orEmpty(), loading = value == null) }
        }
    }
    fun acknowledgeResult(id: Long) {
        _state.value = _state.value.copy(results = _state.value.results.filterNot { it.id == id })
    }

    private var recentlyDeletedAlarm: Alarm? = null
    private fun launchCommand(block: suspend Usecases.() -> Unit) {
        if (isClosed) return
        usecases.launchCommand {
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
        if (isClosed) return
        when (event) {
            is AlarmListEvent.OnEditAlarmClick -> sendUiEvent(Navigate(event.alarm))
            is AlarmListEvent.OnAddAlarmClick -> sendUiEvent(Navigate(Alarm()))
            is AlarmListEvent.OnAlarmOnChange -> setEnabled(event.alarm, event.isOn)
            is AlarmListEvent.OnSkipNextClick -> launchCommand {
                if (!skipNextSupported) return@launchCommand
                val skippedDate = skipNextAlarm(event.alarmId) ?: return@launchCommand
                analytics.trackSafely(AnalyticsEvents.alarmSkipped)
                sendUiEvent(ShowSnackbar(
                    code = UiEvent.ListMessage.SKIPPED,
                    skippedDate = skippedDate,
                    actionType = SnackbarAction.UNDO_SKIP,
                    relatedAlarmId = event.alarmId,
                ))
            }
            is AlarmListEvent.OnUndoSkipClick -> launchCommand {
                if (!skipNextSupported) return@launchCommand
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
                    code = UiEvent.ListMessage.DELETED,
                    actionType = SnackbarAction.UNDO_DELETE,
                ))
            }
            is AlarmListEvent.DeleteTestAlarm -> launchCommand { deleteAlarm(event.alarmId) }
            is AlarmListEvent.OnClearAlarmsClick -> launchCommand { clearAlarms(getSavedAlarms().first()) }
            AlarmListEvent.OnClearEmptyAlarmsClick -> sendUiEvent(ShowSnackbar(UiEvent.ListMessage.EMPTY))
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
        _state.value = _state.value.copy(results = _state.value.results + AlarmListResult(++resultId, event))
    }

    fun scheduleAlarm(alarm: Alarm, reschedule: Boolean) = launchCommand {
        scheduleAlarm(alarm, reschedule)
        sendUiEvent(ShowSnackbar(UiEvent.ListMessage.SCHEDULED, alarm = alarm))
    }

    fun expireSkips() = launchCommand { rescheduleFutureAlarms.clearExpiredSkips() }

    fun cancelAlarm(alarm: Alarm) = setEnabled(alarm, false)
}

/** Immutable semantic results are replayed until native presentation acknowledges their IDs. */
data class AlarmListState(val alarms: List<Alarm> = emptyList(), val loading: Boolean = true, val results: List<AlarmListResult> = emptyList())
data class AlarmListResult(val id: Long, val event: UiEvent)
