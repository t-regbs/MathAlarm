package com.timilehinaregbesola.mathalarm.presentation.alarmsettings

import co.touchlab.kermit.Logger
import com.timilehinaregbesola.mathalarm.utils.AlarmErrorMessage
import com.timilehinaregbesola.mathalarm.platform.getDefaultAlarmTone
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.timilehinaregbesola.mathalarm.domain.model.Alarm
import com.timilehinaregbesola.mathalarm.framework.Usecases
import com.timilehinaregbesola.mathalarm.utils.getFormatTime
import com.timilehinaregbesola.mathalarm.utils.initLocalDateTimeInSystemZone
import com.timilehinaregbesola.mathalarm.utils.toIndex
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.CancellationException
import kotlinx.datetime.LocalDateTime

class AlarmSettingsViewModel(
    private val usecases: Usecases,
) : ViewModel() {

    private var isNewAlarm: Boolean? = null

    private var isRescheduled: Boolean? = null

    private val _alarmTime = mutableStateOf(TimeState())
    val alarmTime: State<TimeState> = _alarmTime

    private val _alarmTitle = mutableStateOf(TextFieldValue("Good day"))
    val alarmTitle: MutableState<TextFieldValue> = _alarmTitle

    private val _dayChooser = mutableStateOf("FFFFFFF")
    val dayChooser: State<String> = _dayChooser

    private val _repeatWeekly = mutableStateOf(false)
    val repeatWeekly: State<Boolean>
        get() = _repeatWeekly

    private val _vibrate = mutableStateOf(false)
    val vibrate: State<Boolean> = _vibrate

    private val _snoozeEnabled = mutableStateOf(true)
    val snoozeEnabled: State<Boolean> = _snoozeEnabled

    private val _difficulty = mutableIntStateOf(0)
    val difficulty: State<Int> = _difficulty

    private val _tone = mutableStateOf("")
    val tone: State<String> = _tone

    private val _isOn = mutableStateOf(false)
    val isOn: State<Boolean> = _isOn

    private val _isSaved = mutableStateOf(false)
    val isSaved: State<Boolean> = _isSaved

    private val _eventFlow = MutableSharedFlow<UiEvent>()
    val eventFlow = _eventFlow.asSharedFlow()

    var currentAlarmId: Long? = null

    fun onEvent(event: AddEditAlarmEvent) {
        when (event) {
            is AddEditAlarmEvent.OnSaveTodoClick -> {
                val edited = createAlarm().copy(isSaved = true)
                viewModelScope.launch {
                    try {
                        usecases.command {
                            val old = findAlarm(edited.alarmId)
                            val alarm = edited.copy(
                                pendingTimes = old?.pendingTimes.orEmpty(),
                                scheduleInitialized = old?.scheduleInitialized ?: false,
                                snoozedUntil = old?.snoozedUntil,
                                activeAt = old?.activeAt,
                                scheduleError = old?.scheduleError,
                                scheduleTimeZone = old?.scheduleTimeZone
                            )
                            // Cancel using the old snapshot as well, for pre-migration identities.
                            if (old != null && isRescheduled == true) cancelAlarm(old)
                            val id = addAlarm(alarm)
                            val saved = alarm.copy(
                                alarmId = if (alarm.alarmId == 0L) id else alarm.alarmId
                            )
                            currentAlarmId = saved.alarmId
                            if (saved.isOn && (isNewAlarm == true || isRescheduled == true)) {
                                scheduleAlarm(saved, true)
                            } else if (saved.isOn) {
                                updateAlarm(saved)
                            }
                        }
                        _eventFlow.emit(UiEvent.SaveAlarm)
                    } catch (e: CancellationException) {
                        throw e
                    } catch (e: Exception) {
                        Logger.e(e) { "Unable to save alarm" }
                        _eventFlow.emit(UiEvent.ShowError(AlarmErrorMessage.SAVE))
                    }
                }
            }
            is AddEditAlarmEvent.OnTestClick -> {
                viewModelScope.launch {
                    _eventFlow.emit(UiEvent.TestAlarm(createAlarm()))
                }
            }
            is AddEditAlarmEvent.ChangeTime -> {
                markExistingAlarmForReschedule()
                _alarmTime.value = event.value
            }
            is AddEditAlarmEvent.EnteredTitle -> {
                _alarmTitle.value = event.value
            }
            is AddEditAlarmEvent.ToggleRepeat -> {
                markExistingAlarmForReschedule()
                _repeatWeekly.value = event.value
            }
            is AddEditAlarmEvent.ToggleVibrate -> {
                _vibrate.value = event.value
            }
            is AddEditAlarmEvent.ToggleSnooze -> {
                if (isNewAlarm == false && _isOn.value) {
                    isRescheduled = true
                }
                _snoozeEnabled.value = event.value
            }
            is AddEditAlarmEvent.ToggleDayChooser -> {
                markExistingAlarmForReschedule()
                _dayChooser.value = event.value
            }
            is AddEditAlarmEvent.OnDifficultyChange -> {
                _difficulty.intValue = event.value
            }
            is AddEditAlarmEvent.OnToneChange -> {
                _tone.value = event.value
            }
            is AddEditAlarmEvent.OnToneError -> {
                viewModelScope.launch {
                    _eventFlow.emit(UiEvent.ShowError(AlarmErrorMessage.TONE))
                }
            }
        }
    }

    private fun createAlarm() = Alarm(
        alarmId = currentAlarmId!!,
        hour = _alarmTime.value.hour,
        minute = _alarmTime.value.minute,
        repeat = _repeatWeekly.value,
        repeatDays = _dayChooser.value,
        isOn = isNewAlarm?.let {
            if (it) true else _isOn.value
        } ?: false,
        vibrate = _vibrate.value,
        title = _alarmTitle.value.text,
        difficulty = _difficulty.value,
        alarmTone = _tone.value,
        isSaved = _isSaved.value,
        snooze = if (_snoozeEnabled.value) DEFAULT_SNOOZE_MINUTES else 0,
    )

    private fun initDateTime(alarm: Alarm): LocalDateTime = alarm.initLocalDateTimeInSystemZone()

    private fun markExistingAlarmForReschedule() {
        isNewAlarm?.let {
            if (!it) {
                isRescheduled = true
            }
        }
    }

    fun setAlarm(curAlarm: Alarm) {
        if (currentAlarmId == null) {
            curAlarm.let { alarm ->
                currentAlarmId = alarm.alarmId
                _alarmTime.value = TimeState(
                    hour = alarm.hour,
                    minute = alarm.minute,
                    formattedTime = alarm.getFormatTime().toString(),
                )
                if (alarm.alarmId == 0L) {
                    isNewAlarm = true
                    val sb = StringBuilder("FFFFFFF")
                    val dateTime = initDateTime(alarm)
                    val dayOfTheWeek = dateTime.date.dayOfWeek.toIndex()
                    sb[dayOfTheWeek] = 'T'
                    _dayChooser.value = sb.toString()
                } else {
                    isNewAlarm = false
                    _dayChooser.value = alarm.repeatDays
                }
                _repeatWeekly.value = alarm.repeat
                _vibrate.value = alarm.vibrate
                _snoozeEnabled.value = alarm.snooze != 0
                _difficulty.intValue = alarm.difficulty
                if (alarm.alarmTone == "") {
                    _tone.value = getDefaultAlarmTone()
                } else {
                    _tone.value = alarm.alarmTone
                }
                val formattedTitle = alarm.title.replace('+', ' ')
                _alarmTitle.value = TextFieldValue(formattedTitle)
                _isOn.value = alarm.isOn
                _isSaved.value = alarm.isSaved
            }
        }
    }

    sealed class UiEvent {
        data class ShowError(val error: AlarmErrorMessage) : UiEvent()
        object SaveAlarm : UiEvent()
        data class TestAlarm(val alarm: Alarm) : UiEvent()
    }
}

private const val DEFAULT_SNOOZE_MINUTES = 5
