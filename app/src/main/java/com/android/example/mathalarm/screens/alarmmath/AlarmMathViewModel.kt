package com.android.example.mathalarm.screens.alarmmath

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
import java.util.*

class AlarmMathViewModel(
    private val alarmKey:Long = 0L,
    dataSource: AlarmDao
): ViewModel() {

    val database = dataSource

//    var mAlarm: Alarm? = null


    private var viewModelJob = Job()

    var alarm: LiveData<Alarm>

    private val uiScope = CoroutineScope(Dispatchers.Main + viewModelJob)

    var currentAlarm = MutableLiveData<Alarm?>()

    val alarms = database.getAlarms()

    init {
        alarm = database.getAlarm(alarmKey)
    }

    fun getAlarm(alarmId: Long): LiveData<Alarm>{
        return database.getAlarm(alarmId)
    }
    fun createAlarm(){
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

    fun onUpdate(alarm: Alarm){
        uiScope.launch {
            update(alarm)
            currentAlarm.value = getCurrentAlarmFromDatabase()
        }
    }

    fun onDelete(alarm: Alarm){
        uiScope.launch {
            delete(alarm)
            currentAlarm.value = getCurrentAlarmFromDatabase()
        }
    }


    //Cancels an alarm - Called when an alarm is turned off, deleted, and rescheduled
    fun cancelAlarm(context: Context?) {
        val cancel = Intent(context, AlarmReceiver::class.java)
        for (i in 0..6) { //For each day of the week
            if (currentAlarm.value!!.repeatDays[i] == 'T') {
                val stringId: StringBuilder = StringBuilder().append(i)
                    .append(currentAlarm.value!!.hour).append(currentAlarm.value!!.minute)
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