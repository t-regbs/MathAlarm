package com.timilehinaregbesola.mathalarm.domain.model

import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

data class Alarm @OptIn(ExperimentalTime::class) constructor(
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
    val scheduleError: String? = null,
    val scheduleTimeZone: String? = null
)
