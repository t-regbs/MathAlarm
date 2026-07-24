package com.timilehinaregbesola.mathalarm.presentation.alarmmath

sealed class ToneState(val total: Int) {
    class Stopped(seconds: Int = 0) : ToneState(seconds)
    class Countdown(total: Int, val seconds: Int) : ToneState(total)
}
