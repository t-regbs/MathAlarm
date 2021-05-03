package com.timilehinaregbesola.mathalarm.presentation.alarmmath

import androidx.lifecycle.ViewModel
import com.timilehinaregbesola.mathalarm.framework.Interactors
import kotlinx.coroutines.*

class AlarmMathViewModel(private val interactors: Interactors) : ViewModel() {
//    var alarm = MutableLiveData<Alarm?>()
//    var currentAlarm = MutableLiveData<Alarm?>()
//
//    fun getAlarm(key: Long) = viewModelScope.launch {
//        val al = interactors.getLatestAlarm()
//        val alarmFound = if (key == 0L) interactors.findAlarm(al!!.alarmId) else interactors.findAlarm(key)
//        alarm.postValue(alarmFound)
//    }
//
//    fun onUpdate(alarm: Alarm) {
//        viewModelScope.launch {
//            interactors.updateAlarm(alarm)
//            currentAlarm.value = interactors.getLatestAlarm()
//        }
//    }
//
//    // Cancels an alarm - Called when an alarm is turned off, deleted, and rescheduled
//    fun cancelAlarm(context: Context?) {
//        val cancel = Intent(context, com.timilehinaregbesola.mathalarm.AlarmReceiver::class.java)
//        for (i in 0..6) { // For each day of the week
//            if (currentAlarm.value!!.repeatDays[i] == 'T') {
//                val stringId: StringBuilder = StringBuilder().append(i)
//                    .append(currentAlarm.value!!.hour).append(currentAlarm.value!!.minute)
//                val intentId = stringId.toString().toInt()
//                val cancelAlarmPI = PendingIntent.getBroadcast(
//                    context, intentId, cancel,
//                    PendingIntent.FLAG_CANCEL_CURRENT
//                )
//                val alarmManager = context
//                    ?.getSystemService(Context.ALARM_SERVICE) as AlarmManager
//                alarmManager.cancel(cancelAlarmPI)
//                cancelAlarmPI.cancel()
//            }
//        }
//    }
//
//    fun onClear() {
//        viewModelScope.launch {
//            interactors.clearAlarms()
//            currentAlarm.value = null
//        }
//    }
}
