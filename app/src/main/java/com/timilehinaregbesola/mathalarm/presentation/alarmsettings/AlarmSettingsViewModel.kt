package com.timilehinaregbesola.mathalarm.presentation.alarmsettings

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.timilehinaregbesola.mathalarm.domain.model.Alarm
import com.timilehinaregbesola.mathalarm.framework.Usecases
import com.timilehinaregbesola.mathalarm.utils.Navigation
import com.timilehinaregbesola.mathalarm.utils.UiEvent
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.receiveAsFlow

class AlarmSettingsViewModel(
    savedStateHandle: SavedStateHandle,
    private val usecases: Usecases
) : ViewModel() {

    var alarm by mutableStateOf<Alarm?>(null)
        private set

    private val _uiEvent = Channel<UiEvent>()
    val uiEvent = _uiEvent.receiveAsFlow()

    init {
        val alarmId = savedStateHandle.get<Long>(Navigation.NAV_SETTINGS_SHEET_ARGUMENT)
        println(savedStateHandle.keys())
        println(alarmId)

    }

    fun onEvent(event: AddEditAlarmEvent) {
        when (event) {
            is AddEditAlarmEvent.OnSaveTodoClick -> {
                viewModelScope.launch {
//                    usecases.addAlarm()
                    sendUiEvent(UiEvent.PopBackStack)
                }
            }
        }
    }

    private fun sendUiEvent(event: UiEvent) {
        viewModelScope.launch {
            _uiEvent.send(event)
        }
    }

    fun setupAlarm(id: Long) {
        if (id != -1L) {
            viewModelScope.launch {
                usecases.findAlarm(id)?.let { alarm ->
                    this@AlarmSettingsViewModel.alarm = alarm
                }
            }
        }
    }
}
