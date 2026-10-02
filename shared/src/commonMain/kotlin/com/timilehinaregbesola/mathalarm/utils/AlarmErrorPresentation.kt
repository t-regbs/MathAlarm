package com.timilehinaregbesola.mathalarm.utils

import com.timilehinaregbesola.mathalarm.utils.strings.Strings

/** Native UI resolves semantic error codes using its own catalog. */
fun AlarmErrorMessage.resolve(strings: Strings): String = when (this) {
    AlarmErrorMessage.SAVE -> strings.alarmSaveFailed
    AlarmErrorMessage.RECOVERY, AlarmErrorMessage.UPDATE, AlarmErrorMessage.INITIALIZATION -> strings.alarmUpdateFailed
    AlarmErrorMessage.DISMISS, AlarmErrorMessage.STALE_OCCURRENCE -> strings.alarmDismissFailed
    AlarmErrorMessage.SNOOZE -> strings.alarmSnoozeFailed
    AlarmErrorMessage.TONE -> strings.toneUnavailable
    AlarmErrorMessage.INCORRECT_ANSWER -> strings.incorrectAnswer
}
