package com.timilehinaregbesola.mathalarm.presentation.alarmmath

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.timilehinaregbesola.mathalarm.domain.model.Alarm
import com.timilehinaregbesola.mathalarm.framework.Usecases
import com.timilehinaregbesola.mathalarm.interactors.AudioPlayer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

@HiltViewModel
class AlarmMathViewModel @Inject constructor(
    private val usecases: Usecases,
    val audioPlayer: AudioPlayer,
) : ViewModel() {
    private val _state = MutableLiveData<ToneState>(ToneState.Stopped())
    val state: LiveData<ToneState> = _state
    private val _answerText = mutableStateOf("")
    val answerText: State<String> = _answerText
    private val _eventFlow = MutableSharedFlow<UiEvent>()
    val eventFlow = _eventFlow.asSharedFlow()
    private var currentTimer: Job? = null

    fun onEvent(event: MathScreenEvent) {
        when (event) {
            is MathScreenEvent.OnClearClick -> {
                _answerText.value = ""
            }
            is MathScreenEvent.OnSnoozeClick -> {
                snoozeAlarm(event.alarm)
                stopAudioAndHideKeyboard()
            }
            is MathScreenEvent.OnEnterClick -> {
                if (_answerText.value.isNotBlank() && event.problem.answer == _answerText.value.trim().toInt()) {
                    _answerText.value = ""
                    stopAudioAndHideKeyboard()
                    viewModelScope.launch {
                        _eventFlow.emit(UiEvent.CompleteAndClose)
                    }
                } else {
                    viewModelScope.launch {
                        _eventFlow.emit(UiEvent.ShowSnackbar("Incorrect"))
                    }
                }
            }
            is MathScreenEvent.EnteredAnswer -> {
                _answerText.value = event.value
            }
            is MathScreenEvent.OnToneError -> {
                viewModelScope.launch {
                    _eventFlow.emit(UiEvent.ShowSnackbar(event.message))
                }
            }
        }
    }

    private fun snoozeAlarm(alarmId: Long) {
        viewModelScope.launch {
            usecases.snoozeAlarm(alarmId)
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

    private fun stopTimer() {
        currentTimer?.cancel()
        _state.value = ToneState.Stopped(0)
    }

    private fun stopAudioAndHideKeyboard() {
        audioPlayer.stop()
        stopTimer()
        viewModelScope.launch {
            _eventFlow.emit(UiEvent.StopVibrateAndHideKeyboard)
        }
    }

    private fun timer(seconds: Int): Flow<Int> = flow {
        for (s in 0 until (seconds + 1)) {
            delay(1000L)
            emit(s)
        }
    }

    fun completeAlarm(alarm: Alarm) {
        viewModelScope.launch {
            usecases.completeAlarm(alarm)
        }
    }

    sealed class UiEvent {
        data class ShowSnackbar(val message: String) : UiEvent()

        object StopVibrateAndHideKeyboard : UiEvent()

        object CompleteAndClose : UiEvent()
    }
}
