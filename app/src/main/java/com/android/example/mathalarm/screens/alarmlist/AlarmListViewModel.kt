package com.android.example.mathalarm.screens.alarmlist

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.example.mathalarm.database.Alarm
import com.android.example.mathalarm.database.AlarmDao
import kotlinx.coroutines.*

class AlarmListViewModel(
    dataSource: AlarmDao): ViewModel(){

    val database = dataSource

    var addClicked = MutableLiveData<Boolean?>()

    val alarms = database.getAlarms()

    private val _navigateToAlarmSettings = MutableLiveData<Long>()
    val navigateToAlarmSettings
        get() = _navigateToAlarmSettings


    fun onUpdate(alarm: Alarm){
        viewModelScope.launch {
            update(alarm)
        }
    }

    //Called when add menu is pressed
    fun onAdd(){
        viewModelScope.launch {
            val id = add(Alarm())
            addClicked.value = true
            _navigateToAlarmSettings.value = id
        }
    }

    fun onDelete(alarm: Alarm){
        viewModelScope.launch {
            delete(alarm)
        }
    }


    fun onClear(){
        viewModelScope.launch {
            clear()
        }
    }


    fun onAlarmClicked(id: Long){
        addClicked.value = false
        _navigateToAlarmSettings.value = id
    }

    fun onAlarmSettingsNavigated(){
        _navigateToAlarmSettings.value = null
    }

}