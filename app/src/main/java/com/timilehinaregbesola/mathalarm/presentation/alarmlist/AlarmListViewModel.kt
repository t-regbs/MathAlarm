package com.timilehinaregbesola.mathalarm.presentation.alarmlist

import androidx.lifecycle.*
import com.timilehinaregbesola.mathalarm.domain.model.Alarm
import com.timilehinaregbesola.mathalarm.framework.Usecases
import com.timilehinaregbesola.mathalarm.framework.app.permission.AlarmPermission
import com.timilehinaregbesola.mathalarm.provider.CalendarProvider
import com.timilehinaregbesola.mathalarm.utils.UiEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.* // ktlint-disable no-wildcard-imports
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import timber.log.Timber
import java.util.*
import javax.inject.Inject

@HiltViewModel
class AlarmListViewModel @Inject constructor(
    private val usecases: Usecases,
    val calender: CalendarProvider,
    val permission: AlarmPermission,
    val savedStateHandle: SavedStateHandle
) : ViewModel() {
    var alarms = usecases.getSavedAlarms()

    private val _uiEvent = Channel<UiEvent>()
    val uiEvent = _uiEvent.receiveAsFlow()

    private var recentlyDeletedAlarm: Alarm? = null
    private var clearAlarmsSelected = false

    fun onEvent(event: AlarmListEvent) {
        when (event) {
            is AlarmListEvent.OnEditAlarmClick -> {
                // Navigate to bottom sheet
                sendUiEvent(UiEvent.Navigate(event.alarm))
            }
            is AlarmListEvent.OnAlarmOnChange -> {
                viewModelScope.launch {
                    usecases.addAlarm(event.alarm.copy(isOn = event.isOn))
                    if (event.isOn) {
                        usecases.scheduleAlarm(event.alarm, false)
                    } else {
                        // Cancel
                    }
                }
            }
            is AlarmListEvent.OnAddAlarmClick -> {
                // Navigate to bottom sheet
                sendUiEvent(UiEvent.Navigate(Alarm()))
            }
            is AlarmListEvent.OnUndoDeleteClick -> {
                viewModelScope.launch {
                    usecases.addAlarm(recentlyDeletedAlarm ?: return@launch)
                    recentlyDeletedAlarm = null
                }
            }
            is AlarmListEvent.OnDeleteAlarmClick -> {
                viewModelScope.launch {
                    usecases.deleteAlarm(event.alarm)
                    recentlyDeletedAlarm = event.alarm
                    sendUiEvent(UiEvent.ShowSnackbar(message = "Alarm Deleted", action = "Undo"))
                }
            }
            is AlarmListEvent.DeleteTestAlarm -> {
                viewModelScope.launch {
                    usecases.deleteAlarm(event.alarmId)
                }
            }
            is AlarmListEvent.OnClearAlarmsClick -> {
                clearAlarmsSelected = true
                viewModelScope.launch {
                    alarms.takeWhile { clearAlarmsSelected }.collect { list ->
                        Timber.d("öooo")
                        usecases.clearAlarms(list)
                        clearAlarmsSelected = false
                    }
                }
            }
        }
    }

    private fun sendUiEvent(event: UiEvent) {
        viewModelScope.launch {
            _uiEvent.send(event)
        }
    }

    fun onUpdate(alarm: Alarm) {
        viewModelScope.launch {
            usecases.updateAlarm(alarm)
        }
    }

    fun scheduleAlarm(alarm: Alarm, reschedule: Boolean, message: String) {
        viewModelScope.launch {
            usecases.scheduleAlarm(alarm, reschedule)
            sendUiEvent(UiEvent.ShowSnackbar(message = message))
        }
    }

    fun cancelAlarm(alarm: Alarm) {
        viewModelScope.launch {
            usecases.cancelAlarm(alarm)
        }
    }
}
