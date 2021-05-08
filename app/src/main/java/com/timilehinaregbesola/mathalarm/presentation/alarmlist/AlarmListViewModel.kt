package com.timilehinaregbesola.mathalarm.presentation.alarmlist

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.timilehinaregbesola.mathalarm.domain.model.Alarm
import com.timilehinaregbesola.mathalarm.framework.Interactors
import kotlinx.coroutines.* // ktlint-disable no-wildcard-imports

class AlarmListViewModel(private val interactors: Interactors) : ViewModel() {
    var addClicked = MutableLiveData<Boolean?>()
    val _alarms = MutableLiveData<List<Alarm>>()
    val alarms: LiveData<List<Alarm>>
        get() = _alarms

    private val _openEditSettings = MutableLiveData<Long?>()
    val openEditSettings
        get() = _openEditSettings

    val alarm: MutableState<Alarm?> = mutableStateOf(null)

    fun onUpdate(alarm: Alarm) {
        viewModelScope.launch {
            interactors.updateAlarm(alarm)
            getAlarms()
        }
    }

    fun getAlarms() {
        viewModelScope.launch {
            val alarmList = interactors.getAlarms()
            _alarms.postValue(alarmList)
        }
    }

    fun getAlarm(key: Long) = viewModelScope.launch {
        val alarmFound = interactors.findAlarm(key)
        alarm.value = alarmFound
    }

    // Called when add menu is pressed
    fun onAdd(new: Alarm) {
//        val new = Alarm()
//        val sb = StringBuilder("FFFFFFF")
//        val cal = initCalendar(new)
//        val dayOfTheWeek =
//            getDayOfWeek(cal[Calendar.DAY_OF_WEEK])
//        sb.setCharAt(dayOfTheWeek, 'T')
//        new.repeatDays = sb.toString()
        viewModelScope.launch {
            interactors.addAlarm(new)
//            _navigateToAlarmSettings.value = id
        }
    }

    fun onFabClicked() {
        addClicked.value = true
    }

    fun onDelete(alarm: Alarm) {
        viewModelScope.launch {
            interactors.deleteAlarm(alarm)
            getAlarms()
        }
    }

    fun onClear() {
        viewModelScope.launch {
            interactors.clearAlarms()
            getAlarms()
        }
    }

    fun onEditAlarmClicked(id: Long) {
        addClicked.value = false
        _openEditSettings.value = id
    }

    fun onEditSettingsNavigated() {
        _openEditSettings.value = null
        addClicked.value = null
    }

    fun setTone(alert: String?) {
        if (alarm.value != null && alert != null) {
            alarm.value!!.alarmTone = alert
            onUpdate(alarm.value!!)
        }
    }
}
