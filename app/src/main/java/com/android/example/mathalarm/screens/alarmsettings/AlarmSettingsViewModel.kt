package com.android.example.mathalarm.screens.alarmsettings

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.example.mathalarm.database.Alarm
import com.android.example.mathalarm.database.AlarmDao
import com.android.example.mathalarm.database.AlarmRepository
import kotlinx.coroutines.*

class AlarmSettingsViewModel(private val repository: AlarmRepository): ViewModel() {
    var alarm = MutableLiveData<Alarm?>()

    private val _navigateToAlarmMath = MutableLiveData<Long>()
    val navigateToAlarmMath
        get() = _navigateToAlarmMath

    private var _latestAlarm = MutableLiveData<Alarm?>()
    val latestAlarm: LiveData<Alarm?>
        get() = _latestAlarm


    init {
//        getAlarm(alarmKey)
        initializeCurrentAlarm()
    }


    fun onUpdate(alarm: Alarm){
        viewModelScope.launch {
            repository.update(alarm)
            _latestAlarm.value = repository.getLatestAlarmFromDatabase()
        }
    }

    fun onDelete(alarm: Alarm){
        viewModelScope.launch {
            repository.delete(alarm)
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

    //Called when add menu is pressed
    fun onAdd(newAlarm: Alarm){
        viewModelScope.launch {
            val id = repository.add(newAlarm)
           _navigateToAlarmMath.value = id
            _latestAlarm.value = repository.getLatestAlarmFromDatabase()
        }
    }

    fun onAlarmMathNavigated() {
        _navigateToAlarmMath.value = null
    }

}