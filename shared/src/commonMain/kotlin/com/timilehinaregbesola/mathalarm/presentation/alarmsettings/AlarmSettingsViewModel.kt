package com.timilehinaregbesola.mathalarm.presentation.alarmsettings

import co.touchlab.kermit.Logger
import com.rickclephas.kmp.nativecoroutines.NativeCoroutines
import com.rickclephas.kmp.nativecoroutines.NativeCoroutinesState
import com.rickclephas.kmp.observableviewmodel.MutableStateFlow
import com.timilehinaregbesola.mathalarm.analytics.AnalyticsEvents
import com.timilehinaregbesola.mathalarm.analytics.AnalyticsTracker
import com.timilehinaregbesola.mathalarm.analytics.NoopAnalyticsTracker
import com.timilehinaregbesola.mathalarm.analytics.trackSafely
import com.timilehinaregbesola.mathalarm.domain.model.Alarm
import com.timilehinaregbesola.mathalarm.domain.model.MathChallenge
import com.timilehinaregbesola.mathalarm.domain.model.mathChallenge
import com.timilehinaregbesola.mathalarm.framework.Usecases
import com.timilehinaregbesola.mathalarm.framework.app.permission.AlarmPermission
import com.timilehinaregbesola.mathalarm.platform.getDefaultAlarmTone
import com.timilehinaregbesola.mathalarm.presentation.SharedFeatureViewModel
import com.timilehinaregbesola.mathalarm.utils.AlarmErrorMessage
import com.timilehinaregbesola.mathalarm.utils.initLocalDateTimeInSystemZone
import com.timilehinaregbesola.mathalarm.utils.toIndex
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.mapNotNull
import kotlinx.coroutines.flow.update

/** Immutable draft and durable outcomes. Cursor, focus, formatting and navigation belong to the view. */
data class AlarmEditorState(
    val alarmId: Long? = null,
    val alarmTime: TimeState = TimeState(),
    val alarmTitle: String = "Good day",
    val dayChooser: String = "FFFFFFF",
    val repeatWeekly: Boolean = false,
    val vibrate: Boolean = false,
    val snoozeEnabled: Boolean = true,
    val maxSnoozes: Int = 3,
    val snoozeMinutes: Int = 5,
    val challenge: MathChallenge = MathChallenge(),
    val tone: String = "",
    val isOn: Boolean = false,
    val isSaved: Boolean = false,
    val hasUnsavedChanges: Boolean = false,
    val isSaving: Boolean = false,
    val validation: AlarmEditorValidation = AlarmEditorValidation.NOT_INITIALIZED,
    val results: List<AlarmEditorResult> = emptyList(),
)

enum class AlarmEditorValidation { NONE, NOT_INITIALIZED, INVALID_TIME, INVALID_DAYS }

data class AlarmEditorResult(val id: Long, val event: AlarmSettingsViewModel.UiEvent)

class AlarmSettingsViewModel internal constructor(
    private val usecases: Usecases,
    private val permission: AlarmPermission,
    private val analytics: AnalyticsTracker = NoopAnalyticsTracker,
) : SharedFeatureViewModel() {
    private val mutableState = MutableStateFlow(viewModelScope, AlarmEditorState())
    @NativeCoroutinesState
    val state: StateFlow<AlarmEditorState> = mutableState.asStateFlow()

    private var initialDraft: Alarm? = null
    private var isNewAlarm = false
    private var isRescheduled = false
    private var nextResultId = 0L
    val currentAlarmId: Long? get() = state.value.alarmId
    val hasUnsavedChanges: Boolean get() = state.value.hasUnsavedChanges

    /** Waiting observes retained outcomes only. Cancellation cannot cancel an accepted save. */
    @NativeCoroutines
    suspend fun awaitResult(afterId: Long): AlarmEditorResult = state
        .mapNotNull { current -> current.results.firstOrNull { it.id > afterId } }
        .first()

    /** Results replay until the native owner acknowledges the matching stable ID. */
    fun acknowledgeResult(id: Long) {
        mutableState.update { it.copy(results = it.results.filterNot { result -> result.id == id }) }
    }

    fun onEvent(event: AddEditAlarmEvent) {
        if (isClosed) return
        when (event) {
            AddEditAlarmEvent.OnSaveTodoClick -> save()
            AddEditAlarmEvent.OnTestClick -> {
                if (state.value.validation == AlarmEditorValidation.NONE) {
                    publish(UiEvent.TestAlarm(createAlarm()))
                } else publish(UiEvent.ValidationFailed(state.value.validation))
            }
            is AddEditAlarmEvent.ChangeTime -> edit(reschedule = true) { copy(alarmTime = event.value) }
            is AddEditAlarmEvent.EnteredTitle -> edit { copy(alarmTitle = event.value) }
            is AddEditAlarmEvent.ToggleRepeat -> edit(reschedule = true) { copy(repeatWeekly = event.value) }
            is AddEditAlarmEvent.ToggleVibrate -> edit { copy(vibrate = event.value) }
            is AddEditAlarmEvent.ToggleSnooze -> edit { copy(snoozeEnabled = event.value) }
            is AddEditAlarmEvent.ChangeMaxSnoozes -> {
                if (event.value in listOf(0, 1, 2, 3, 5)) edit { copy(maxSnoozes = event.value) }
            }
            is AddEditAlarmEvent.ChangeSnoozeDuration -> {
                if (event.minutes in 1..30) edit { copy(snoozeMinutes = event.minutes) }
            }
            is AddEditAlarmEvent.ToggleDayChooser -> edit(reschedule = true) { copy(dayChooser = event.value) }
            is AddEditAlarmEvent.OnChallengeChange -> edit { copy(challenge = event.value.normalized()) }
            is AddEditAlarmEvent.OnToneChange -> edit { copy(tone = event.value) }
            AddEditAlarmEvent.OnToneError -> publish(UiEvent.ShowError(AlarmErrorMessage.TONE))
        }
    }

    private fun edit(reschedule: Boolean = false, change: AlarmEditorState.() -> AlarmEditorState) {
        // Freeze the accepted save snapshot until its authoritative outcome returns.
        if (state.value.isSaving) return
        if (reschedule && !isNewAlarm) isRescheduled = true
        mutableState.update { change(it) }
        refreshDraftStatus()
    }

    private fun refreshDraftStatus() {
        val draft = state.value
        val validation = when {
            draft.alarmId == null -> AlarmEditorValidation.NOT_INITIALIZED
            draft.alarmTime.hour !in 0..23 || draft.alarmTime.minute !in 0..59 -> AlarmEditorValidation.INVALID_TIME
            draft.dayChooser.length != 7 || draft.dayChooser.any { it != 'T' && it != 'F' } -> AlarmEditorValidation.INVALID_DAYS
            else -> AlarmEditorValidation.NONE
        }
        val dirty = initialDraft?.let { initial ->
            // Constructor timestamps are defaults, not editable fields.
            createAlarm().copy(newDateTime = initial.newDateTime,
                newHour = initial.newHour, newMinute = initial.newMinute) != initial
        } ?: false
        mutableState.update { it.copy(validation = validation, hasUnsavedChanges = dirty) }
    }

    private fun save() {
        if (state.value.isSaving) return
        if (state.value.validation != AlarmEditorValidation.NONE) {
            publish(UiEvent.ValidationFailed(state.value.validation))
            return
        }
        val edited = createAlarm().copy(isSaved = true)
        val reschedule = isRescheduled
        val wasNew = isNewAlarm
        mutableState.update { it.copy(isSaving = true) }
        // The application owns an accepted save. Removing the UI observer or closing
        // this ViewModel cannot abort storage/scheduling or lose its retained result.
        usecases.launchCommand {
            try {
                val accepted = command {
                    val old = findAlarm(edited.alarmId)
                    val alarm = edited.copy(
                        isOn = old?.isOn ?: edited.isOn,
                        pendingTimes = old?.pendingTimes.orEmpty(),
                        scheduleInitialized = old?.scheduleInitialized ?: false,
                        snoozedUntil = old?.snoozedUntil,
                        activeAt = old?.activeAt,
                        snoozeCount = old?.snoozeCount ?: 0,
                        skippedDate = old?.skippedDate.takeIf { !reschedule },
                        scheduleError = old?.scheduleError,
                        scheduleTimeZone = old?.scheduleTimeZone,
                    )
                    if (alarm.isOn && !permission.hasExactAlarmPermission()) return@command null
                    val id = addAlarm(alarm)
                    val saved = alarm.copy(alarmId = if (alarm.alarmId == 0L) id else alarm.alarmId)
                    // Insertion can succeed before OS scheduling fails. Retain its allocated
                    // identity immediately so retry updates this row instead of creating another.
                    mutableState.update { it.copy(alarmId = saved.alarmId) }
                    refreshDraftStatus()
                    if (saved.isOn && (wasNew || reschedule)) scheduleAlarm(saved, true)
                    else updateAlarm(saved)
                    findAlarm(saved.alarmId) ?: saved
                }
                if (accepted != null) {
                    mutableState.update { it.copy(alarmId = accepted.alarmId, isSaved = true, isOn = accepted.isOn) }
                    isNewAlarm = false
                    isRescheduled = false
                    initialDraft = createAlarm()
                    refreshDraftStatus()
                    analytics.trackSafely(AnalyticsEvents.alarmSaved(edited, wasNew))
                    publish(UiEvent.SaveAlarm)
                } else {
                    analytics.trackSafely(AnalyticsEvents.alarmSaveFailed("exact_alarm_permission"))
                    publish(UiEvent.RequestExactAlarmPermission)
                }
            } catch (error: CancellationException) {
                throw error
            } catch (error: Exception) {
                Logger.e(error) { "Unable to save alarm" }
                analytics.trackSafely(AnalyticsEvents.alarmSaveFailed("operation_error"))
                publish(UiEvent.ShowError(AlarmErrorMessage.SAVE))
            } finally {
                mutableState.update { it.copy(isSaving = false) }
            }
        }
    }

    private fun publish(event: UiEvent) {
        val result = AlarmEditorResult(++nextResultId, event)
        mutableState.update { it.copy(results = it.results + result) }
    }

    private fun createAlarm(): Alarm = with(state.value) {
        Alarm(
            alarmId = alarmId ?: 0L, hour = alarmTime.hour, minute = alarmTime.minute,
            repeat = repeatWeekly, repeatDays = dayChooser,
            isOn = if (isNewAlarm) true else isOn, vibrate = vibrate, title = alarmTitle,
            difficulty = challenge.difficulty, questionCount = challenge.questionCount,
            challengeOperations = challenge.operations, additionRange = challenge.additionRange,
            factorRange = challenge.factorRange, difficultyMix = challenge.difficultyMix,
            alarmTone = tone, isSaved = isSaved,
            snooze = if (snoozeEnabled) snoozeMinutes else 0, maxSnoozes = maxSnoozes,
        )
    }

    /** One initialization per retained editing session; nested views never overwrite the draft. */
    fun setAlarm(alarm: Alarm) {
        if (isClosed || currentAlarmId != null) return
        isNewAlarm = alarm.alarmId == 0L
        val days = if (isNewAlarm) {
            StringBuilder("FFFFFFF").apply {
                this[alarm.initLocalDateTimeInSystemZone().date.dayOfWeek.toIndex()] = 'T'
            }.toString()
        } else alarm.repeatDays
        mutableState.update {
            it.copy(
                alarmId = alarm.alarmId, alarmTime = TimeState(alarm.hour, alarm.minute),
                dayChooser = days, repeatWeekly = alarm.repeat, vibrate = alarm.vibrate,
                snoozeEnabled = alarm.snooze != 0,
                snoozeMinutes = alarm.snooze.takeIf { duration -> duration > 0 }?.coerceAtMost(30) ?: 5,
                maxSnoozes = alarm.maxSnoozes, challenge = alarm.mathChallenge,
                tone = alarm.alarmTone.ifEmpty { getDefaultAlarmTone() },
                alarmTitle = alarm.title.replace('+', ' '), isOn = alarm.isOn, isSaved = alarm.isSaved,
            )
        }
        initialDraft = createAlarm()
        refreshDraftStatus()
        analytics.trackSafely(AnalyticsEvents.alarmEditorOpened(isNewAlarm))
    }

    sealed class UiEvent {
        object RequestExactAlarmPermission : UiEvent()
        data class ShowError(val error: AlarmErrorMessage) : UiEvent()
        data class ValidationFailed(val validation: AlarmEditorValidation) : UiEvent()
        object SaveAlarm : UiEvent()
        data class TestAlarm(val alarm: Alarm) : UiEvent()
    }
}
