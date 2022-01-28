package com.timilehinaregbesola.mathalarm.presentation.alarmlist

import androidx.lifecycle.*
import com.timilehinaregbesola.mathalarm.domain.model.Alarm
import com.timilehinaregbesola.mathalarm.framework.Usecases
import com.timilehinaregbesola.mathalarm.utils.Navigation
import com.timilehinaregbesola.mathalarm.utils.UiEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.* // ktlint-disable no-wildcard-imports
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@HiltViewModel
class AlarmListViewModel @Inject constructor(
    private val usecases: Usecases,
    val savedStateHandle: SavedStateHandle
) : ViewModel() {
    var alarms = usecases.getSavedAlarms()

    private val _uiEvent = Channel<UiEvent>()
    val uiEvent = _uiEvent.receiveAsFlow()

    fun onEvent(event: AlarmListEvent) {
        when (event) {
            is AlarmListEvent.OnEditAlarmClick -> {
                // Navigate to bottom sheet
                sendUiEvent(UiEvent.Navigate(Navigation.buildSettingsPath(event.alarmId)))
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
                viewModelScope.launch {
                    val id = usecases.addAlarm(Alarm())
                    sendUiEvent(UiEvent.Navigate(Navigation.buildSettingsPath(id)))
                }
            }
            is AlarmListEvent.OnUndoDeleteClick -> {
            }
            is AlarmListEvent.OnDeleteAlarmClick -> {
                viewModelScope.launch {
                    usecases.deleteAlarm(event.alarm)
                }
            }
            is AlarmListEvent.DeleteTestAlarm -> {
                runBlocking {
                    usecases.deleteAlarmWithId(event.alarmId)
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

    fun scheduleAlarm(alarm: Alarm, reschedule: Boolean) {
        viewModelScope.launch {
            usecases.scheduleAlarm(alarm, reschedule)
        }
    }

    fun onClear() {
        viewModelScope.launch {
            usecases.clearAlarms()
        }
    }
}
