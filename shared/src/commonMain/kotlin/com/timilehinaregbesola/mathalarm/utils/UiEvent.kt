package com.timilehinaregbesola.mathalarm.utils

import com.timilehinaregbesola.mathalarm.domain.model.Alarm

sealed class UiEvent {
    data class ShowError(val error: AlarmErrorMessage) : UiEvent()
    data class Navigate(val alarm: Alarm) : UiEvent()
    data class ShowSnackbar(
        val code: ListMessage,
        val alarm: Alarm? = null,
        val actionType: SnackbarAction? = null,
        val relatedAlarmId: Long? = null,
        val skippedDate: String? = null,
    ) : UiEvent()

    enum class ListMessage { DELETED, EMPTY, SCHEDULED, SKIPPED }

    enum class SnackbarAction {
        UNDO_DELETE,
        UNDO_SKIP,
    }
}
