package com.timilehinaregbesola.mathalarm.presentation.alarmlist

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.timilehinaregbesola.mathalarm.domain.model.Alarm
import com.timilehinaregbesola.mathalarm.framework.Usecases
import com.timilehinaregbesola.mathalarm.presentation.alarmmath.ToneState
import kotlinx.coroutines.* // ktlint-disable no-wildcard-imports
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow

class AlarmListViewModel(private val usecases: Usecases) : ViewModel() {
    private val _alarms = MutableLiveData<List<Alarm>>()
    val alarms: LiveData<List<Alarm>>
        get() = _alarms

    private val _sheetState = MutableLiveData<SheetState>(SheetState.Init)
    val sheetState
        get() = _sheetState

    private val _state = MutableLiveData<ToneState>(ToneState.Stopped())
    val state: LiveData<ToneState> = _state
    private var currentTimer: Job? = null

    fun onUpdate(alarm: Alarm) {
        viewModelScope.launch {
            usecases.updateAlarm(alarm)
            getAlarms()
        }
    }

    fun getAlarms() {
        viewModelScope.launch {
            val alarmList = usecases.getAlarms()
            _alarms.postValue(alarmList)
        }
    }

    fun getAlarm(key: Long) = viewModelScope.launch {
        val alarmFound = usecases.findAlarm(key)
    }

    fun scheduleAlarm(alarm: Alarm, reschedule: Boolean) {
        viewModelScope.launch {
            usecases.scheduleAlarm(alarm, reschedule)
        }
    }

    fun retrieveAlarm(key: Long) = runBlocking {
        val alarmFound = usecases.findAlarm(key)
        alarmFound
    }

    // Called when add fab is pressed
    fun onAdd(new: Alarm) {
        viewModelScope.launch {
            usecases.addAlarm(new)
            getAlarms()
        }
    }

    fun onAddTestAlarm(new: Alarm): Long {
        var id: Long
        runBlocking {
            id = usecases.addAlarm(new)
        }
        getAlarms()
        return id
    }

    fun onFabClicked() {
        _sheetState.value = SheetState.NewAlarm()
    }

    fun onDelete(alarm: Alarm) {
        viewModelScope.launch {
            usecases.deleteAlarm(alarm)
            getAlarms()
        }
    }

    fun onDeleteWithId(alarmId: Long) {
        viewModelScope.launch {
            usecases.deleteAlarmWithId(alarmId)
            getAlarms()
        }
    }

    fun onClear() {
        viewModelScope.launch {
            usecases.clearAlarms()
            getAlarms()
        }
    }

    fun onEditAlarmClicked(id: Long) {
        _sheetState.value = SheetState.EditAlarm(id)
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
        _sheetState.value = SheetState.Init
    }

    private fun timer(seconds: Int): Flow<Int> = flow {
        for (s in 0 until (seconds + 1)) {
            delay(1000L)
            emit(s)
        }
    }
}
