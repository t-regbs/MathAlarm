package com.timilehinaregbesola.mathalarm.presentation.alarmlist

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.timilehinaregbesola.mathalarm.domain.model.Alarm
import com.timilehinaregbesola.mathalarm.framework.Usecases
import com.timilehinaregbesola.mathalarm.framework.app.permission.AlarmPermission
import com.timilehinaregbesola.mathalarm.provider.DateTimeProvider
import com.timilehinaregbesola.mathalarm.utils.UiEvent
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@HiltViewModel
class AlarmListViewModel @Inject constructor(
    private val usecases: Usecases,
    val calender: DateTimeProvider,
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
                Timber.d("OnAlarmOnChange event received: alarmId=${event.alarm.alarmId}, isOn=${event.isOn}")
                viewModelScope.launch {
                    Timber.d("Updating alarm in database: alarmId=${event.alarm.alarmId}, setting isOn=${event.isOn}")
                    usecases.addAlarm(event.alarm.copy(isOn = event.isOn))
                    if (event.isOn) {
                        Timber.d("Alarm is being turned ON, scheduling: alarmId=${event.alarm.alarmId}")
                        usecases.scheduleAlarm(event.alarm, false)
                    } else {
                        Timber.d("Alarm is being turned OFF: alarmId=${event.alarm.alarmId}")
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
        Timber.d("scheduleAlarm called: alarmId=${alarm.alarmId}, time=${alarm.hour}:${alarm.minute}, repeat=${alarm.repeat}, repeatDays=${alarm.repeatDays}, reschedule=$reschedule")
        viewModelScope.launch {
            Timber.d("Calling usecases.scheduleAlarm for alarmId=${alarm.alarmId}")
            val result = usecases.scheduleAlarm(alarm, reschedule)
            Timber.d("scheduleAlarm completed for alarmId=${alarm.alarmId}, showing snackbar message: $message")
            sendUiEvent(UiEvent.ShowSnackbar(message = message))
        }
    }

    fun cancelAlarm(alarm: Alarm) {
        Timber.d("cancelAlarm called: alarmId=${alarm.alarmId}, time=${alarm.hour}:${alarm.minute}, repeat=${alarm.repeat}, repeatDays=${alarm.repeatDays}")
        viewModelScope.launch {
            Timber.d("Calling usecases.cancelAlarm for alarmId=${alarm.alarmId}")
            usecases.cancelAlarm(alarm)
            Timber.d("cancelAlarm completed for alarmId=${alarm.alarmId}")
        }
    }
}
