package com.android.example.mathalarm.screens.alarmmath

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
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

    var alarm = MutableLiveData<Alarm?>()

    var currentAlarm = MutableLiveData<Alarm?>()

    val alarms = database.getAlarms()

    init {
        getAlarm(alarmKey)
    }

    fun getAlarm(key: Long) = viewModelScope.launch {
        val alarmFound = findAlarm(key)
        alarm.postValue(alarmFound)
    }


    fun onUpdate(alarm: Alarm){
        viewModelScope.launch {
            update(alarm)
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

    private suspend fun findAlarm(id: Long): Alarm = database.getAlarm(id)

    private suspend fun getCurrentAlarmFromDatabase(): Alarm? {
        return withContext(Dispatchers.IO){
            database.getLastAlarm()
        }
    }


    private suspend fun update(alarm: Alarm){
        database.updateAlarm(alarm)
    }

    private suspend fun clear() = database.clear()


    fun onClear(){
        viewModelScope.launch {
            clear()

            currentAlarm.value = null
        }
    }
}