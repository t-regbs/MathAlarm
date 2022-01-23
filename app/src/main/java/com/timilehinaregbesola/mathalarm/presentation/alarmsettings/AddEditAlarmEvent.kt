package com.timilehinaregbesola.mathalarm.presentation.alarmsettings

import androidx.compose.ui.text.input.TextFieldValue

sealed class AddEditAlarmEvent {
    object OnSaveTodoClick : AddEditAlarmEvent()
    data class ChangeTime(val value: TimeState) : AddEditAlarmEvent()
    data class EnteredTitle(val value: TextFieldValue) : AddEditAlarmEvent()
    data class ToggleVibrate(val value: Boolean) : AddEditAlarmEvent()
    data class ToggleRepeat(val value: Boolean) : AddEditAlarmEvent()
    data class ToggleDayChooser(val value: String) : AddEditAlarmEvent()
    data class OnDifficultyChange(val value: Int) : AddEditAlarmEvent()
    data class OnToneChange(val value: String) : AddEditAlarmEvent()
    data class OnToneError(val message: String) : AddEditAlarmEvent()
    object OnTestClick : AddEditAlarmEvent()
}
