package com.timilehinaregbesola.mathalarm.utils

import com.timilehinaregbesola.mathalarm.domain.model.Alarm

sealed class UiEvent {
    data class ShowError(val error: AlarmErrorMessage) : UiEvent()
    object PopBackStack : UiEvent()
    data class Navigate(val alarm: Alarm) : UiEvent()
    data class ShowSnackbar(val message: String, val action: String? = null) : UiEvent()
}
