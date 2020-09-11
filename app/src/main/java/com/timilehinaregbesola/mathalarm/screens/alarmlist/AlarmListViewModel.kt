package com.timilehinaregbesola.mathalarm.screens.alarmlist

import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.timilehinaregbesola.mathalarm.database.Alarm
import com.timilehinaregbesola.mathalarm.database.AlarmRepository
import com.timilehinaregbesola.mathalarm.utils.getDayOfWeek
import kotlinx.coroutines.*
import java.util.*

class AlarmListViewModel(private val repository: AlarmRepository): ViewModel(){
    var addClicked = MutableLiveData<Boolean?>()
    val alarms = MutableLiveData<List<Alarm>>()

    private val _navigateToAlarmSettings = MutableLiveData<Long>()
    val navigateToAlarmSettings
        get() = _navigateToAlarmSettings


    fun onUpdate(alarm: Alarm){
        viewModelScope.launch {
            repository.update(alarm)
            getAlarms()
        }
    }

    fun getAlarms() {
        viewModelScope.launch {
            val alarmList = repository.getAlarms()
            alarms.postValue(alarmList)
        }
    }

    //Called when add menu is pressed
    fun onAdd(){
        val new = Alarm()
        val sb = StringBuilder("FFFFFFF")
        val cal = initCalendar(new)
        val dayOfTheWeek =
            getDayOfWeek(cal[Calendar.DAY_OF_WEEK])
        sb.setCharAt(dayOfTheWeek, 'T')
        new.repeatDays = sb.toString()
        viewModelScope.launch {
            val id = repository.add(new)
            addClicked.value = true
            _navigateToAlarmSettings.value = id
        }
    }

    private  fun initCalendar(alarm: Alarm): Calendar {
        val cal = Calendar.getInstance()
        cal[Calendar.HOUR_OF_DAY] = alarm.hour
        cal[Calendar.MINUTE] = alarm.minute
        cal[Calendar.SECOND] = 0
        return cal
    }

    fun onDelete(alarm: Alarm){
        viewModelScope.launch {
            repository.delete(alarm)
            getAlarms()
        }
    }


    fun onClear(){
        viewModelScope.launch {
            repository.clear()
            getAlarms()
        }
    }


    fun onAlarmClicked(id: Long){
        addClicked.value = false
        _navigateToAlarmSettings.value = id
    }

    fun onAlarmSettingsNavigated(){
        _navigateToAlarmSettings.value = null
    }

}