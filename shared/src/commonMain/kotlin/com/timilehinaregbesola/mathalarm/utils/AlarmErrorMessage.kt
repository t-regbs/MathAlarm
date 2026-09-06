package com.timilehinaregbesola.mathalarm.utils

import com.timilehinaregbesola.mathalarm.utils.strings.Strings

/** Resolve at the screen so messages follow the currently selected language. */
enum class AlarmErrorMessage {
    SAVE, UPDATE, DISMISS, SNOOZE, TONE, INCORRECT_ANSWER;

    fun resolve(strings: Strings): String = when (this) {
        SAVE -> strings.alarmSaveFailed
        UPDATE -> strings.alarmUpdateFailed
        DISMISS -> strings.alarmDismissFailed
        SNOOZE -> strings.alarmSnoozeFailed
        TONE -> strings.toneUnavailable
        INCORRECT_ANSWER -> strings.incorrectAnswer
    }
}
