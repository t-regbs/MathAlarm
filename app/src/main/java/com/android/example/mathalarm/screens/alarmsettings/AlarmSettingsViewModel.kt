package com.android.example.mathalarm.screens.alarmsettings

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.android.example.mathalarm.database.Alarm
import com.android.example.mathalarm.database.AlarmDao
import kotlinx.coroutines.*

class AlarmSettingsViewModel(alarmKey:Long = 0L, dataSource: AlarmDao): ViewModel() {

    val database = dataSource

    var alarm = MutableLiveData<Alarm?>()

    private val _navigateToAlarmMath = MutableLiveData<Long>()
    val navigateToAlarmMath
        get() = _navigateToAlarmMath

    private var _latestAlarm = MutableLiveData<Alarm?>()
    val latestAlarm: LiveData<Alarm?>
        get() = _latestAlarm


    init {
        getAlarm(alarmKey)
        initializeCurrentAlarm()
    }


    fun onUpdate(alarm: Alarm){
        viewModelScope.launch {
            update(alarm)
            _latestAlarm.value = getLatestAlarmFromDatabase()
        }
    }

    fun onDelete(alarm: Alarm){
        viewModelScope.launch {
            delete(alarm)
            _latestAlarm.value = getLatestAlarmFromDatabase()
        }
    }

    fun getAlarm(key: Long) = viewModelScope.launch {
        val alarmFound = findAlarm(key)
        alarm.postValue(alarmFound)
    }

    private fun initializeCurrentAlarm() {
        viewModelScope.launch {
            _latestAlarm.value = getLatestAlarmFromDatabase()
        }
    }

    private suspend fun getLatestAlarmFromDatabase(): Alarm? {
        return database.getLastAlarm()
    }

    private suspend fun add(alarm: Alarm): Long {
        return database.addAlarm(alarm)
    }

    private suspend fun findAlarm(id: Long): Alarm = database.getAlarm(id)

    private suspend fun update(alarm: Alarm){
        database.updateAlarm(alarm)
    }


    private suspend fun delete(alarm: Alarm) {
        database.deleteAlarm(alarm)
    }

    //Called when add menu is pressed
    fun onAdd(newAlarm: Alarm){
        viewModelScope.launch {
            val id = add(newAlarm)
           _navigateToAlarmMath.value = id
            _latestAlarm.value = getLatestAlarmFromDatabase()
        }
    }

    fun onAlarmMathNavigated() {
        _navigateToAlarmMath.value = null
    }

}