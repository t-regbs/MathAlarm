package com.android.example.mathalarm.screens.alarmlist

import android.app.Application
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.android.example.mathalarm.database.Alarm
import com.android.example.mathalarm.database.AlarmDao
import kotlinx.coroutines.*
import java.util.*

class AlarmListViewModel(
    dataSource: AlarmDao,
    application: Application): ViewModel(){

    val database = dataSource

    private var viewModelJob = Job()

    private val uiScope = CoroutineScope(Dispatchers.Main + viewModelJob)

    var currentAlarm = MutableLiveData<Alarm?>()

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
            var mAlarm = Alarm()
            add(mAlarm)

            val cal = Calendar.getInstance()
            mAlarm.hour = cal[Calendar.HOUR_OF_DAY]
            mAlarm.minute = cal[Calendar.MINUTE]

            update(mAlarm)

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