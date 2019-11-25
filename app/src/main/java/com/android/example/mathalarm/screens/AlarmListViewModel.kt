package com.android.example.mathalarm.screens

import android.app.Application
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.android.example.mathalarm.database.Alarm
import com.android.example.mathalarm.database.AlarmDao
import kotlinx.coroutines.*

class AlarmListViewModel(
    dataSource: AlarmDao,
    application: Application): ViewModel(){

    val database = dataSource

    private var viewModelJob = Job()

    private val uiScope = CoroutineScope(Dispatchers.Main + viewModelJob)

    private var currentAlarm = MutableLiveData<Alarm?>()

    val alarms = database.getAlarms()

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
    fun onAdd(){
        uiScope.launch {
            var newAlarm = Alarm()
            add(newAlarm)
            currentAlarm.value = getCurrentAlarmFromDatabase()
        }
    }

    override fun onCleared() {
        super.onCleared()
        viewModelJob.cancel()
    }
}