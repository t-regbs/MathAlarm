package com.timilehinaregbesola.mathalarm.presentation.alarmsettings

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.timilehinaregbesola.mathalarm.domain.model.Alarm
import com.timilehinaregbesola.mathalarm.framework.Usecases
import kotlinx.coroutines.*

class AlarmSettingsViewModel(private val usecases: Usecases) : ViewModel() {
    var alarm = MutableLiveData<Alarm?>()

    private val _navigateToAlarmMath = MutableLiveData<Long?>()
    val navigateToAlarmMath
        get() = _navigateToAlarmMath
    private val _removeSpinner = MutableLiveData<Boolean>()
    val removeSpinner
        get() = _removeSpinner
    private var _latestAlarm = MutableLiveData<Alarm?>()
    val latestAlarm: LiveData<Alarm?>
        get() = _latestAlarm

    init {
//        getAlarm(alarmKey)
        initializeCurrentAlarm()
    }

    fun onUpdate(alarm: Alarm) {
        viewModelScope.launch {
            usecases.updateAlarm(alarm)
            _latestAlarm.value = usecases.getLatestAlarm()
        }
    }

    fun onDeleteFromId(alarmId: Long?) {
        viewModelScope.launch {
            val alarm = usecases.findAlarm(alarmId!!)
            if (alarm != null) {
                usecases.deleteAlarm(alarm)
            }
            _latestAlarm.value = usecases.getLatestAlarm()
        }
    }

    fun onDeleteAlarm(alarm: Alarm) {
        viewModelScope.launch {
            usecases.deleteAlarm(alarm)
            _latestAlarm.value = usecases.getLatestAlarm()
        }
    }

    fun getAlarm(key: Long) = viewModelScope.launch {
        val alarmFound = usecases.findAlarm(key)
        alarm.postValue(alarmFound)
    }

    private fun initializeCurrentAlarm() {
        viewModelScope.launch {
            _latestAlarm.value = usecases.getLatestAlarm()
        }
    }

    // Called when add menu is pressed
    fun onAdd(newAlarm: Alarm) {
        viewModelScope.launch {
            val id = usecases.addAlarm(newAlarm)
            _navigateToAlarmMath.value = id
            _latestAlarm.value = usecases.getLatestAlarm()
        }
    }

    fun onAlarmMathNavigated() {
        _navigateToAlarmMath.value = null
    }

    fun stopLoading() {
        _removeSpinner.postValue(true)
    }
}
