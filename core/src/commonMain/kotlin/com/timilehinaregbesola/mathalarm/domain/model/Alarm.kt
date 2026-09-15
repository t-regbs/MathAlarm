package com.timilehinaregbesola.mathalarm.domain.model

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock

data class Alarm(
    var alarmId: Long = 0L,
    val newDateTime: LocalDateTime = Clock.System.now().toLocalDateTime(TimeZone.currentSystemDefault()),
    val newHour: Int = newDateTime.hour,
    val newMinute: Int = newDateTime.minute,
    var hour: Int = newHour,
    var minute: Int = newMinute,
    var repeat: Boolean = false,
    var repeatDays: String = "FFFFFFF",
    var isOn: Boolean = false,
    var difficulty: Int = 0,
    val questionCount: Int = 1,
    val challengeOperations: String = "+−×÷",
    val additionRange: Int = 0,
    val factorRange: Int = 0,
    val difficultyMix: String = "",
    var alarmTone: String = "",
    var vibrate: Boolean = false,
    var snooze: Int = 5,
    var title: String = "",
    var isSaved: Boolean = false,
    // Concrete occurrences survive process death and reboot. Empty is meaningful once initialized.
    val pendingTimes: List<Long> = emptyList(),
    val scheduleInitialized: Boolean = false,
    val snoozedUntil: Long? = null,
    val activeAt: Long? = null,
    // Local calendar date (yyyy-MM-dd) of the occurrence omitted by Skip next.
    val skippedDate: String? = null,
    val scheduleError: String? = null,
    val scheduleTimeZone: String? = null
) {
    companion object {
        // Retain the persisted value for alarms saved by schema version 5.
        const val SCHEDULING_IN_PROGRESS = "Scheduling has not completed"
    }
}
