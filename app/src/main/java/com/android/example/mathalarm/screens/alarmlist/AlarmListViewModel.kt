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

    private suspend fun getListSize(): Int{
        var size = 0
        withContext(Dispatchers.IO){
            size = database.getSize()
        }
        return size
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

            mAlarm.hour = (cal[Calendar.HOUR_OF_DAY])
            mAlarm.minute = (cal[Calendar.MINUTE])

            update(mAlarm)

            currentAlarm.value = getCurrentAlarmFromDatabase()
        }
    }

    fun onClear(){
        uiScope.launch {
            clear()

            currentAlarm.value = null
        }
    }

    fun onGetListSize(): Int{
        var size = 0
        uiScope.launch {
            size = getListSize()
        }
        return size
    }

    override fun onCleared() {
        super.onCleared()
        viewModelJob.cancel()
    }
}