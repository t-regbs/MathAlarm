package com.timilehinaregbesola.mathalarm.domain.model

import java.util.*

data class Alarm(
    var alarmId: Long = 0L,

    val newCal: Calendar = Calendar.getInstance(),

    val newHour: Int = newCal[Calendar.HOUR_OF_DAY],

    val newMinute: Int = newCal[Calendar.MINUTE],

    var hour: Int = newHour,

    var minute: Int = newMinute,

    var repeat: Boolean = false,

    var repeatDays: String = "FFFFFFF",

    var isOn: Boolean = false,

    var difficulty: Int = 0,

    var alarmTone: String = "",

    var vibrate: Boolean = false,

    var snooze: Int = 5
)
