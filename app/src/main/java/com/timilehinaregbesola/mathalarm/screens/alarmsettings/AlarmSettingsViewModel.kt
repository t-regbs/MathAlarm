package com.timilehinaregbesola.mathalarm.screens.alarmsettings

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.timilehinaregbesola.mathalarm.domain.model.Alarm
import com.timilehinaregbesola.mathalarm.framework.RoomAlarmDataSource
import kotlinx.coroutines.*

class AlarmSettingsViewModel(private val repository: RoomAlarmDataSource) : ViewModel() {
    var alarm = MutableLiveData<Alarm?>()

    private val _navigateToAlarmMath = MutableLiveData<Long>()
    val navigateToAlarmMath
        get() = _navigateToAlarmMath
    private val _removeSpinner = MutableLiveData<Boolean>()
    val removeSpinner
        get() = _removeSpinner
    private var _latestAlarm = MutableLiveData<Alarm?>()
    val latestAlarm: LiveData<Alarm?>
        get() = _latestAlarm

    init {
//        getAlarm(alarmKey)
        initializeCurrentAlarm()
    }

    fun onUpdate(alarm: Alarm) {
        viewModelScope.launch {
            repository.updateAlarm(alarm)
            _latestAlarm.value = repository.getLatestAlarmFromDatabase()
        }
    }

    fun onDeleteFromId(alarmId: Long?) {
        viewModelScope.launch {
            val alarm = repository.findAlarm(alarmId!!)
            repository.deleteAlarm(alarm)
            _latestAlarm.value = repository.getLatestAlarmFromDatabase()
        }
    }

    fun onDeleteAlarm(alarm: Alarm) {
        viewModelScope.launch {
            repository.deleteAlarm(alarm)
            _latestAlarm.value = repository.getLatestAlarmFromDatabase()
        }
    }

    fun getAlarm(key: Long) = viewModelScope.launch {
        val alarmFound = repository.findAlarm(key)
        alarm.postValue(alarmFound)
    }

    private fun initializeCurrentAlarm() {
        viewModelScope.launch {
            _latestAlarm.value = repository.getLatestAlarmFromDatabase()
        }
    }

    // Called when add menu is pressed
    fun onAdd(newAlarm: Alarm) {
        viewModelScope.launch {
            val id = repository.addAlarm(newAlarm)
            _navigateToAlarmMath.value = id
            _latestAlarm.value = repository.getLatestAlarmFromDatabase()
        }
    }

    fun onAlarmMathNavigated() {
        _navigateToAlarmMath.value = null
    }

    fun stopLoading() {
        _removeSpinner.postValue(true)
    }
}
