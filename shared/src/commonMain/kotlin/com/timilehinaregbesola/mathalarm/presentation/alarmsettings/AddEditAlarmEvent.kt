package com.timilehinaregbesola.mathalarm.presentation.alarmsettings

import androidx.compose.ui.text.input.TextFieldValue

sealed class AddEditAlarmEvent {
    object OnSaveTodoClick : AddEditAlarmEvent()
    data class ChangeTime(val value: TimeState) : AddEditAlarmEvent()
    data class EnteredTitle(val value: TextFieldValue) : AddEditAlarmEvent()
    data class ToggleVibrate(val value: Boolean) : AddEditAlarmEvent()
    data class ToggleSnooze(val value: Boolean) : AddEditAlarmEvent()
    data class ChangeMaxSnoozes(val value: Int) : AddEditAlarmEvent()
    data class ChangeSnoozeDuration(val minutes: Int) : AddEditAlarmEvent()
    data class ToggleRepeat(val value: Boolean) : AddEditAlarmEvent()
    data class ToggleDayChooser(val value: String) : AddEditAlarmEvent()
    data class OnToneChange(val value: String) : AddEditAlarmEvent()
    data class OnToneError(val message: String) : AddEditAlarmEvent()
    data class OnChallengeChange(val value: com.timilehinaregbesola.mathalarm.domain.model.MathChallenge) : AddEditAlarmEvent()
    object OnTestClick : AddEditAlarmEvent()
}
