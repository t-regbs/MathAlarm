package com.timilehinaregbesola.mathalarm.presentation.alarmlist

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.timilehinaregbesola.mathalarm.domain.model.Alarm
import com.timilehinaregbesola.mathalarm.framework.Interactors
import com.timilehinaregbesola.mathalarm.presentation.alarmmath.ToneState
import kotlinx.coroutines.* // ktlint-disable no-wildcard-imports
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow

class AlarmListViewModel(private val interactors: Interactors) : ViewModel() {
    private val _alarms = MutableLiveData<List<Alarm>>()
    val alarms: LiveData<List<Alarm>>
        get() = _alarms

    private val _sheetState = MutableLiveData<SheetState>(SheetState.Init)
    val sheetState
        get() = _sheetState

    private val _isSheetOpen = MutableLiveData(false)
    val isSheetOpen
        get() = _isSheetOpen

    private val _state = MutableLiveData<ToneState>(ToneState.Stopped())
    val state: LiveData<ToneState> = _state
    private var currentTimer: Job? = null

    fun onUpdate(alarm: Alarm) {
        viewModelScope.launch {
            interactors.updateAlarm(alarm)
            getAlarms()
        }
    }

    fun getAlarms() {
        viewModelScope.launch {
            val alarmList = interactors.getAlarms()
            _alarms.postValue(alarmList)
        }
    }

    fun getAlarm(key: Long) = viewModelScope.launch {
        val alarmFound = interactors.findAlarm(key)
    }

    fun retrieveAlarm(key: Long) = runBlocking {
        val alarmFound = interactors.findAlarm(key)
        alarmFound
    }

    // Called when add fab is pressed
//    fun onAdd(new: Alarm) {
//        viewModelScope.launch {
//            interactors.addAlarm(new)
//            getAlarms()
//        }
//    }

    fun onAddTestAlarm(new: Alarm): Long {
        var id: Long
        runBlocking {
            id = interactors.addAlarm(new)
        }
        getAlarms()
        return id
    }

    fun onFabClicked() {
        _sheetState.value = SheetState.NewAlarm()
        _isSheetOpen.value = true
    }

    fun onDelete(alarm: Alarm) {
        viewModelScope.launch {
            interactors.deleteAlarm(alarm)
            getAlarms()
        }
    }

    fun onDeleteWithId(alarmId: Long) {
        viewModelScope.launch {
            interactors.deleteAlarmWithId(alarmId)
            getAlarms()
        }
    }

    fun onClear() {
        viewModelScope.launch {
            interactors.clearAlarms()
            getAlarms()
        }
    }

    fun onEditAlarmClicked(id: Long) {
        _sheetState.value = SheetState.EditAlarm(id)
        _isSheetOpen.value = true
    }

    fun setTone(alert: String?) {
        val alarm = retrieveAlarm(_sheetState.value!!.alarmId)
        if (alarm != null && alert != null) {
            alarm.alarmTone = alert
            onUpdate(alarm)
        }
    }

    @InternalCoroutinesApi
    fun startTimer() {
        val currentState = _state.value
        if (currentState !is ToneState.Stopped) {
            return
        }
        val seconds = 1000
        _state.value = ToneState.Countdown(seconds, seconds)
        this.currentTimer = viewModelScope.launch {
            timer(seconds).collect {
                _state.value = if (it == 0) {
                    ToneState.Stopped(0)
                } else {
//                    secondDown()
                    ToneState.Countdown(seconds, it)
                }
            }
        }
    }

    fun stopTimer() {
        currentTimer?.cancel()
        _state.value = ToneState.Stopped(0)
    }

    fun onSheetClose() {
        _isSheetOpen.value = false
        _sheetState.value = SheetState.Init
    }

    private fun timer(seconds: Int): Flow<Int> = flow {
        for (s in 0 until (seconds + 1)) {
            delay(1000L)
            emit(s)
        }
    }
}
