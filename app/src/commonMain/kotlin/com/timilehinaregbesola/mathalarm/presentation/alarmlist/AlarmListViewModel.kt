package com.timilehinaregbesola.mathalarm.presentation.alarmlist

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.timilehinaregbesola.mathalarm.domain.model.Alarm
import com.timilehinaregbesola.mathalarm.framework.Usecases
import com.timilehinaregbesola.mathalarm.framework.app.permission.AlarmPermission
import com.timilehinaregbesola.mathalarm.utils.UiEvent
import com.timilehinaregbesola.mathalarm.utils.UiEvent.Navigate
import com.timilehinaregbesola.mathalarm.utils.UiEvent.ShowSnackbar
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.flow.takeWhile
import kotlinx.coroutines.launch

class AlarmListViewModel(
    private val usecases: Usecases,
    val permission: AlarmPermission,
    private val logger: Logger
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
                sendUiEvent(Navigate(event.alarm))
            }
            is AlarmListEvent.OnAlarmOnChange -> {
                logger.d("OnAlarmOnChange event received: alarmId=${event.alarm.alarmId}, isOn=${event.isOn}")
                viewModelScope.launch {
                    logger.d("Updating alarm in database: alarmId=${event.alarm.alarmId}, setting isOn=${event.isOn}")
                    usecases.addAlarm(event.alarm.copy(isOn = event.isOn))
                    if (event.isOn) {
                        logger.d("Alarm is being turned ON, scheduling: alarmId=${event.alarm.alarmId}")
                        usecases.scheduleAlarm(event.alarm, false)
                    } else {
                        logger.d("Alarm is being turned OFF: alarmId=${event.alarm.alarmId}")
                        // Cancel
                    }
                }
            }
            is AlarmListEvent.OnAddAlarmClick -> {
                // Navigate to bottom sheet
                sendUiEvent(Navigate(Alarm()))
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
                    sendUiEvent(ShowSnackbar(message = "Alarm Deleted", action = "Undo"))
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
                        logger.d("öooo")
                        usecases.clearAlarms(list)
                        clearAlarmsSelected = false
                    }
                }
            }

            AlarmListEvent.OnClearEmptyAlarmsClick -> {
                sendUiEvent(ShowSnackbar(message = "There are no alarms to clear"))
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
        logger.d("scheduleAlarm called: alarmId=${alarm.alarmId}, time=${alarm.hour}:${alarm.minute}, repeat=${alarm.repeat}, repeatDays=${alarm.repeatDays}, reschedule=$reschedule")
        viewModelScope.launch {
            logger.d("Calling usecases.scheduleAlarm for alarmId=${alarm.alarmId}")
            usecases.scheduleAlarm(alarm, reschedule)
            logger.d("scheduleAlarm completed for alarmId=${alarm.alarmId}, showing snackbar message: $message")
            sendUiEvent(ShowSnackbar(message = message))
        }
    }

    fun cancelAlarm(alarm: Alarm) {
        logger.d("cancelAlarm called: alarmId=${alarm.alarmId}, time=${alarm.hour}:${alarm.minute}, repeat=${alarm.repeat}, repeatDays=${alarm.repeatDays}")
        viewModelScope.launch {
            logger.d("Calling usecases.cancelAlarm for alarmId=${alarm.alarmId}")
            usecases.cancelAlarm(alarm)
            logger.d("cancelAlarm completed for alarmId=${alarm.alarmId}")
        }
    }
}
