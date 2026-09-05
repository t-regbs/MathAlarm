package com.timilehinaregbesola.mathalarm.presentation.alarmmath

sealed class MathScreenEvent {
    object OnClearClick : MathScreenEvent()

    data class OnSnoozeClick(val alarm: Long, val preview: Boolean = false) : MathScreenEvent()

    data class OnEnterClick(val problem: MathProblem) : MathScreenEvent()

    data class EnteredAnswer(val value: String) : MathScreenEvent()

    data class OnToneError(val message: String) : MathScreenEvent()
}
