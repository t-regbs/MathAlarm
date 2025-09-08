package com.timilehinaregbesola.mathalarm.presentation.alarmmath

import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.timilehinaregbesola.mathalarm.domain.model.Alarm
import com.timilehinaregbesola.mathalarm.framework.Usecases
import com.timilehinaregbesola.mathalarm.interactors.AudioPlayer
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch

class AlarmMathViewModel(
    private val usecases: Usecases,
    val audioPlayer: AudioPlayer,
    private val logger: Logger
) : ViewModel() {
    private val _state = MutableStateFlow<ToneState>(ToneState.Stopped())
    val state: StateFlow<ToneState> = _state.asStateFlow()
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
        with(audioPlayer) {
            _state.value = ToneState.Countdown(duration, currentPosition)
            this@AlarmMathViewModel.currentTimer = viewModelScope.launch {
                timer(duration/1000).collect {
                    _state.value = if (it == 0) {
                        ToneState.Stopped(0)
                    } else {
                        ToneState.Countdown(duration/1000, it)
                    }
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
        var counter = 0
        while(true) {
            delay(1000L)

            counter++
            emit(counter % (seconds + 1))
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
