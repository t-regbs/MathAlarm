package com.timilehinaregbesola.mathalarm.domain.model

import kotlinx.datetime.*

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

    var alarmTone: String = "",

    var vibrate: Boolean = false,

    var snooze: Int = 5,

    var title: String = "",

    var isSaved: Boolean = false
)
