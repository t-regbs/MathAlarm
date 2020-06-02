package com.android.example.mathalarm.screens.alarmlist

import android.app.Application
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.android.example.mathalarm.database.Alarm
import com.android.example.mathalarm.database.AlarmDao
import kotlinx.coroutines.*
import java.util.*

class AlarmListViewModel(
    dataSource: AlarmDao): ViewModel(){

    val database = dataSource

    private var viewModelJob = Job()

    private val uiScope = CoroutineScope(Dispatchers.Main + viewModelJob)

    var currentAlarm = MutableLiveData<Alarm?>()

    var isFromAdd = MutableLiveData<Boolean?>()

    val alarms = database.getAlarms()

    private val _navigateToAlarmSettings = MutableLiveData<Long>()
    val navigateToAlarmSettings
        get() = _navigateToAlarmSettings

    init {
        initializeCurrentAlarm()
    }

    private fun initializeCurrentAlarm() {
        uiScope.launch {
            currentAlarm.value = getCurrentAlarmFromDatabase()
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

    fun onUpdate(alarm: Alarm){
        uiScope.launch {
            update(alarm)
        }
    }

    private suspend fun update(alarm: Alarm){
        withContext(Dispatchers.IO) {
            database.updateAlarm(alarm)
        }
    }


    private suspend fun clear() {
        withContext(Dispatchers.IO) {
            database.clear()
        }
    }

    //Called when add menu is pressed
    fun onAdd(){
        uiScope.launch {
            val mAlarm = Alarm()
            add(mAlarm)
            isFromAdd.value = true
            _navigateToAlarmSettings.value = getCurrentAlarmFromDatabase()!!.alarmId
        }
    }


    fun onClear(){
        uiScope.launch {
            clear()

            currentAlarm.value = null
        }
    }


    fun onAlarmClicked(id: Long){
        isFromAdd.value = false
        _navigateToAlarmSettings.value = id
    }

    fun onAlarmSettingsNavigated(){
        _navigateToAlarmSettings.value = null
    }

    override fun onCleared() {
        super.onCleared()
        viewModelJob.cancel()
    }
}