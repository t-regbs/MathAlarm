package com.timilehinaregbesola.mathalarm.presentation.alarmlist

import com.timilehinaregbesola.mathalarm.domain.model.Alarm

sealed class AlarmListEvent {
    data class OnDeleteAlarmClick(val alarm: Alarm) : AlarmListEvent()
    data class OnAlarmOnChange(val alarm: Alarm, val isOn: Boolean) : AlarmListEvent()
    object OnUndoDeleteClick : AlarmListEvent()
    data class OnEditAlarmClick(val alarm: Alarm) : AlarmListEvent()
    object OnAddAlarmClick : AlarmListEvent()
    object OnClearAlarmsClick : AlarmListEvent()
    object OnClearEmptyAlarmsClick : AlarmListEvent()
    data class DeleteTestAlarm(val alarmId: Long) : AlarmListEvent()
}
