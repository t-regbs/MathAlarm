package com.android.example.mathalarm.screens.alarmsettings

import android.app.AlarmManager
import android.app.Application
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
import java.util.*

class AlarmSettingsViewModel(
    private val alarmKey:Long = 0L,
    dataSource: AlarmDao): ViewModel() {

    val database = dataSource

//    var mAlarm: Alarm? = null


    private var viewModelJob = Job()

    private val alarm: LiveData<Alarm>
    val alarmm: LiveData<Alarm>
        get() = alarm

    fun getAlarm() = alarm

    private val uiScope = CoroutineScope(Dispatchers.Main + viewModelJob)

    private var _currentAlarm = MutableLiveData<Alarm?>()

    val currentAlarm: LiveData<Alarm?>
        get() = _currentAlarm

    var activeAlarm: LiveData<Alarm?> = currentAlarm



    val alarms = database.getAlarms()

    init {
        alarm = database.getAlarm(alarmKey)
        initializeCurrentAlarm()
    }

    fun createAlarm(){
        uiScope.launch {
            var mAlarm = Alarm()
            add(mAlarm)
            val cal = Calendar.getInstance()

            mAlarm.hour = (cal[Calendar.HOUR_OF_DAY])
            mAlarm.minute = (cal[Calendar.MINUTE])

            update(mAlarm)

            _currentAlarm.value = getCurrentAlarmFromDatabase()
        }
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


    //Cancels an alarm - Called when an alarm is turned off, deleted, and rescheduled
    fun cancelAlarm(context: Context?) {
        val cancel = Intent(context, AlarmReceiver::class.java)
        for (i in 0..6) { //For each day of the week
            if (_currentAlarm.value!!.repeatDays[i] == 'T') {
                val stringId: StringBuilder = StringBuilder().append(i)
                    .append(_currentAlarm.value!!.hour).append(_currentAlarm.value!!.minute)
                val intentId = stringId.toString().toInt()
                val cancelAlarmPI = PendingIntent.getBroadcast(
                    context, intentId, cancel,
                    PendingIntent.FLAG_CANCEL_CURRENT
                )
                val alarmManager = context
                    ?.getSystemService(Context.ALARM_SERVICE) as AlarmManager
                alarmManager.cancel(cancelAlarmPI)
                cancelAlarmPI.cancel()
            }
        }
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
    fun onAdd(newAlarm: Alarm){
        uiScope.launch {
            add(newAlarm)
            _currentAlarm.value = getCurrentAlarmFromDatabase()
        }
    }

    fun onClear(){
        uiScope.launch {
            clear()

            _currentAlarm.value = null
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