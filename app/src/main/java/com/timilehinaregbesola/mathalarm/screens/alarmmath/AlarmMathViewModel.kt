package com.timilehinaregbesola.mathalarm.screens.alarmmath

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.timilehinaregbesola.mathalarm.database.Alarm
import com.timilehinaregbesola.mathalarm.database.AlarmRepository
import kotlinx.coroutines.*

class AlarmMathViewModel(private val repository: AlarmRepository) : ViewModel() {
    var alarm = MutableLiveData<Alarm?>()
    var currentAlarm = MutableLiveData<Alarm?>()

    fun getAlarm(key: Long) = viewModelScope.launch {
        val al = repository.getLatestAlarmFromDatabase()
        val alarmFound = if (key == 0L) repository.findAlarm(al!!.alarmId) else repository.findAlarm(key)
        alarm.postValue(alarmFound)
    }

    fun onUpdate(alarm: Alarm) {
        viewModelScope.launch {
            repository.update(alarm)
            currentAlarm.value = repository.getLatestAlarmFromDatabase()
        }
    }

    // Cancels an alarm - Called when an alarm is turned off, deleted, and rescheduled
    fun cancelAlarm(context: Context?) {
        val cancel = Intent(context, com.timilehinaregbesola.mathalarm.AlarmReceiver::class.java)
        for (i in 0..6) { // For each day of the week
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

    fun onClear() {
        viewModelScope.launch {
            repository.clear()
            currentAlarm.value = null
        }
    }
}
