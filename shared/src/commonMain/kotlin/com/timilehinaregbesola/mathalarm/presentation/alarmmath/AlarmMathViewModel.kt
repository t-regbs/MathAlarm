package com.timilehinaregbesola.mathalarm.presentation.alarmmath

import com.timilehinaregbesola.mathalarm.domain.model.mathChallenge
import kotlinx.coroutines.CancellationException
import com.timilehinaregbesola.mathalarm.utils.AlarmErrorMessage
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import co.touchlab.kermit.Logger
import com.timilehinaregbesola.mathalarm.domain.model.Alarm
import com.timilehinaregbesola.mathalarm.framework.Usecases
import com.timilehinaregbesola.mathalarm.interactors.AudioPlayer
import com.timilehinaregbesola.mathalarm.platform.stopPlatformAlarmAudio
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch

class AlarmMathViewModel(
    private val usecases: Usecases,
    private val audioPlayer: AudioPlayer,
    private val logger: Logger,
    private val progressStore: ChallengeProgressStore,
) : ViewModel() {
    private val _answerText = mutableStateOf("")
    val answerText: State<String> = _answerText
    private val _eventFlow = MutableSharedFlow<UiEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    private var challengeKey: String? = null
    private var occurrence: Pair<Long, Long>? = null
    private var problems by mutableStateOf<List<MathProblem>>(emptyList())
    private val _questionIndex = mutableStateOf(0)
    val questionIndex: State<Int> = _questionIndex
    val questionCount: Int get() = problems.size.coerceAtLeast(1)
    val currentProblem: MathProblem? get() = problems.getOrNull(_questionIndex.value)

    suspend fun initializeChallenge(alarm: Alarm, preview: Boolean) {
        val key = "${alarm.alarmId}:${alarm.activeAt}:$preview"
        if (challengeKey == key) return
        // Notifications (including older iOS payloads) identify the alarm; its saved settings
        // are authoritative. Test Alarm deliberately uses the unsaved editor draft instead.
        val saved = if (!preview && alarm.alarmId != 0L) {
            try { usecases.findAlarm(alarm.alarmId) }
            catch (e: CancellationException) { throw e }
            catch (e: Exception) { logger.e(e) { "Unable to load challenge settings" }; null }
        } else {
            null
        }
        occurrence = if (preview) null else (saved?.activeAt ?: alarm.activeAt)?.let { alarm.alarmId to it }
        val restored = occurrence?.let { (id, activeAt) -> progressStore.load(id, activeAt) }
        problems = restored?.problems ?: generateChallengeProblems(challenge = (saved ?: alarm).mathChallenge)
        _answerText.value = ""
        _questionIndex.value = restored?.questionIndex ?: 0
        persistProgress()
        challengeKey = key
    }

    fun onEvent(event: MathScreenEvent) {
        when (event) {
            is MathScreenEvent.OnClearClick -> {
                _answerText.value = ""
            }
            is MathScreenEvent.OnSnoozeClick -> {
                finishAlarm(event.alarm, preview = event.preview, snooze = true)
            }
            is MathScreenEvent.OnEnterClick -> {
                // Ignore a queued Enter from the previous question after advancing.
                val problem = currentProblem ?: return
                if (event.problem != problem) return
                if (_answerText.value.isNotBlank() && problem.answer == _answerText.value.trim().toIntOrNull()) {
                    _answerText.value = ""
                    if (_questionIndex.value < problems.lastIndex) {
                        _questionIndex.value += 1
                        persistProgress()
                    } else {
                        viewModelScope.launch {
                            _eventFlow.emit(UiEvent.CompleteAndClose)
                        }
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

    private fun persistProgress() {
        occurrence?.let { (id, activeAt) ->
            progressStore.save(id, ChallengeProgressStore.Progress(activeAt, problems, _questionIndex.value))
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
                if (!preview) occurrence?.let { (id, activeAt) -> progressStore.clear(id, activeAt) }
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

    private suspend fun stopAudioAndHideKeyboard(preview: Boolean) {
        audioPlayer.stop()
        // Also stop platform-specific alarm audio (iOS alarm manager)
        if (!preview) stopPlatformAlarmAudio()
        _eventFlow.emit(UiEvent.StopVibrateAndHideKeyboard)
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
