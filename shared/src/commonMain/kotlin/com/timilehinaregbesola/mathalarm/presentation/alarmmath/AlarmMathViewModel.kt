package com.timilehinaregbesola.mathalarm.presentation.alarmmath

import kotlinx.coroutines.CancellationException
import com.timilehinaregbesola.mathalarm.utils.AlarmErrorMessage
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.timilehinaregbesola.mathalarm.domain.model.Alarm
import com.timilehinaregbesola.mathalarm.framework.Usecases
import com.timilehinaregbesola.mathalarm.interactors.AudioPlayer
import com.timilehinaregbesola.mathalarm.platform.stopPlatformAlarmAudio
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
    val currentPosition: Int
        get() = audioPlayer.currentPosition
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
                finishAlarm(event.alarm, preview = event.preview, snooze = true)
            }
            is MathScreenEvent.OnEnterClick -> {
                if (_answerText.value.isNotBlank() && event.problem.answer == _answerText.value.trim().toIntOrNull()) {
                    _answerText.value = ""
                    viewModelScope.launch {
                        _eventFlow.emit(UiEvent.CompleteAndClose)
                    }
                } else {
                    viewModelScope.launch {
                        _eventFlow.emit(UiEvent.ShowError(AlarmErrorMessage.INCORRECT_ANSWER))
                    }
                }
            }
            is MathScreenEvent.EnteredAnswer -> {
                _answerText.value = event.value
            }
            is MathScreenEvent.OnToneError -> {
                viewModelScope.launch {
                    _eventFlow.emit(UiEvent.ShowError(AlarmErrorMessage.TONE))
                }
            }
        }
    }

    private var finishing = false

    private fun finishAlarm(alarmId: Long, preview: Boolean, snooze: Boolean) {
        if (finishing) return
        finishing = true
        viewModelScope.launch {
            try {
                if (!preview) usecases.command {
                    if (snooze) snoozeAlarm(alarmId) else completeAlarm(alarmId)
                }
                stopAudioAndHideKeyboard(preview)
                _eventFlow.emit(UiEvent.Close)
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                logger.e(e) { "Unable to finish alarm" }
                val error = if (snooze) AlarmErrorMessage.SNOOZE else AlarmErrorMessage.DISMISS
                _eventFlow.emit(UiEvent.ShowError(error))
            } finally {
                finishing = false
            }
        }
    }

    fun stopPreview() {
        audioPlayer.stop()
        stopTimer()
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

    fun startAlarmWith(tone: String) {
        try {
            audioPlayer.stop()
        } catch (_: Throwable) {}
        audioPlayer.init()
        audioPlayer.reset()
        audioPlayer.setDataSourceFromString(tone)
        audioPlayer.startAlarmAudio()
    }

    private fun stopTimer() {
        currentTimer?.cancel()
        _state.value = ToneState.Stopped(0)
    }

    private suspend fun stopAudioAndHideKeyboard(preview: Boolean) {
        audioPlayer.stop()
        // Also stop platform-specific alarm audio (iOS alarm manager)
        if (!preview) stopPlatformAlarmAudio()
        stopTimer()
        _eventFlow.emit(UiEvent.StopVibrateAndHideKeyboard)
    }

    private fun timer(seconds: Int): Flow<Int> = flow {
        var counter = 0
        while(true) {
            delay(1000L)

            counter++
            emit(counter % (seconds + 1))
        }
    }

    fun completeAlarm(alarm: Alarm, preview: Boolean = false) =
        finishAlarm(alarm.alarmId, preview, snooze = false)

    sealed class UiEvent {
        data class ShowError(val error: AlarmErrorMessage) : UiEvent()

        object StopVibrateAndHideKeyboard : UiEvent()

        object CompleteAndClose : UiEvent()

        object Close : UiEvent()
    }
}
