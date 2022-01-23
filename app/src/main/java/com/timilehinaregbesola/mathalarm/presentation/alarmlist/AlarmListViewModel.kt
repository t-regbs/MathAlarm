package com.timilehinaregbesola.mathalarm.presentation.alarmlist

import android.media.MediaPlayer
import androidx.lifecycle.*
import com.timilehinaregbesola.mathalarm.domain.model.Alarm
import com.timilehinaregbesola.mathalarm.framework.Usecases
import com.timilehinaregbesola.mathalarm.presentation.alarmmath.ToneState
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
    val audioPlayer: MediaPlayer,
    val savedStateHandle: SavedStateHandle
) : ViewModel() {
    val alarms = usecases.getAlarms()

    private val _state = MutableLiveData<ToneState>(ToneState.Stopped())
    val state: LiveData<ToneState> = _state
    private var currentTimer: Job? = null

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

    fun getAlarm(key: Long) = viewModelScope.launch {
        val alarmFound = usecases.findAlarm(key)
    }

    fun scheduleAlarm(alarm: Alarm, reschedule: Boolean) {
        viewModelScope.launch {
            usecases.scheduleAlarm(alarm, reschedule)
        }
    }

    fun snoozeAlarm(alarmId: Long) {
        viewModelScope.launch {
            usecases.snoozeAlarm(alarmId)
        }
    }

    fun retrieveAlarm(key: Long) = runBlocking {
        val alarmFound = usecases.findAlarm(key)
        alarmFound
    }

    // Called when add fab is pressed
    fun onAdd(new: Alarm) {
        viewModelScope.launch {
            usecases.addAlarm(new)
        }
    }

    fun onAddTestAlarm(new: Alarm): Long {
        var id: Long
        runBlocking {
            id = usecases.addAlarm(new)
        }
        return id
    }

    fun onDelete(alarm: Alarm) {
        viewModelScope.launch {
            usecases.deleteAlarm(alarm)
        }
    }

    fun onClear() {
        viewModelScope.launch {
            usecases.clearAlarms()
        }
    }

    @InternalCoroutinesApi
    fun startTimer() {
        val currentState = _state.value
        if (currentState !is ToneState.Stopped) {
            return
        }
        val seconds = 1000
        _state.value = ToneState.Countdown(seconds, seconds)
        this.currentTimer = viewModelScope.launch {
            timer(seconds).collect {
                _state.value = if (it == 0) {
                    ToneState.Stopped(0)
                } else {
                    ToneState.Countdown(seconds, it)
                }
            }
        }
    }

    fun stopTimer() {
        currentTimer?.cancel()
        _state.value = ToneState.Stopped(0)
    }

    private fun timer(seconds: Int): Flow<Int> = flow {
        for (s in 0 until (seconds + 1)) {
            delay(1000L)
            emit(s)
        }
    }

    fun arrangeDelete(alarmId: Long) {
        savedStateHandle.set("testAlarmId", alarmId)
    }
}
