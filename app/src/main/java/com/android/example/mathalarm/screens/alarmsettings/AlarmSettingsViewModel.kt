package com.android.example.mathalarm.screens.alarmsettings

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.android.example.mathalarm.AlarmReceiver
import com.android.example.mathalarm.database.Alarm
import com.android.example.mathalarm.database.AlarmDao
import kotlinx.coroutines.*

class AlarmSettingsViewModel(alarmKey:Long = 0L, dataSource: AlarmDao): ViewModel() {

    val database = dataSource

    val alarms = database.getAlarms()

    private var viewModelJob = Job()

    var alarm: LiveData<Alarm>

    private val _navigateToAlarmMath = MutableLiveData<Long>()
    val navigateToAlarmMath
        get() = _navigateToAlarmMath

    private val uiScope = CoroutineScope(Dispatchers.Main + viewModelJob)

    private var _currentAlarm = MutableLiveData<Alarm?>()
    val currentAlarm: LiveData<Alarm?>
        get() = _currentAlarm


    init {
        this.alarm = database.getAlarm(alarmKey)
        initializeCurrentAlarm()
    }


    fun onUpdate(alarm: Alarm){
        uiScope.launch {
            update(alarm)
            _currentAlarm.value = getCurrentAlarmFromDatabase()
        }
    }

    fun onDelete(alarm: Alarm){
        uiScope.launch {
            delete(alarm)
            _currentAlarm.value = getCurrentAlarmFromDatabase()
        }
    }

    fun getAlarm(key: Long) = uiScope.launch {
        alarm = database.getAlarm(key)
    }

    private fun initializeCurrentAlarm() {
        uiScope.launch {
            _currentAlarm.value = getCurrentAlarmFromDatabase()
        }
    }

    private suspend fun getCurrentAlarmFromDatabase(): Alarm? {
        return withContext(Dispatchers.IO){
            database.getLastAlarm()
        }
    }

    private suspend fun add(alarm: Alarm){
        withContext(Dispatchers.IO){
            database.addAlarm(alarm)
        }
    }

    private suspend fun update(alarm: Alarm){
        withContext(Dispatchers.IO) {
            database.updateAlarm(alarm)
        }
    }


    private suspend fun delete(alarm: Alarm) {
        withContext(Dispatchers.IO){
            database.deleteAlarm(alarm)
        }
    }

    //Called when add menu is pressed
    fun onAdd(newAlarm: Alarm){
        uiScope.launch {
            add(newAlarm)
           _navigateToAlarmMath.value = getCurrentAlarmFromDatabase()!!.alarmId
            _currentAlarm.value = getCurrentAlarmFromDatabase()
        }
    }

    override fun onCleared() {
        super.onCleared()
        viewModelJob.cancel()
    }

    fun onAlarmMathNavigated(){
        _navigateToAlarmMath.value = null
    }

}