package com.timilehinaregbesola.mathalarm.presentation.alarmmath

import android.media.MediaPlayer
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.timilehinaregbesola.mathalarm.domain.model.Alarm
import com.timilehinaregbesola.mathalarm.framework.Usecases
import com.timilehinaregbesola.mathalarm.presentation.alarmmath.components.ToneState
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.flow
import javax.inject.Inject

@HiltViewModel
class AlarmMathViewModel @Inject constructor(
    private val usecases: Usecases,
    val audioPlayer: MediaPlayer
) : ViewModel() {
    private val _state = MutableLiveData<ToneState>(ToneState.Stopped())
    val state: LiveData<ToneState> = _state
    private var currentTimer: Job? = null

    fun retrieveAlarm(key: Long) = runBlocking {
        val alarmFound = usecases.findAlarm(key)
        alarmFound
    }

    fun snoozeAlarm(alarmId: Long) {
        viewModelScope.launch {
            usecases.snoozeAlarm(alarmId)
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
                    ToneState.Countdown(seconds, it)
                }
            }
        }
    }

    fun stopTimer() {
        currentTimer?.cancel()
        _state.value = ToneState.Stopped(0)
    }

    private fun timer(seconds: Int): Flow<Int> = flow {
        for (s in 0 until (seconds + 1)) {
            delay(1000L)
            emit(s)
        }
    }

    fun completeAlarm(alarm: Alarm) {
        viewModelScope.launch {
            usecases.completeAlarm(alarm)
        }
    }
}
