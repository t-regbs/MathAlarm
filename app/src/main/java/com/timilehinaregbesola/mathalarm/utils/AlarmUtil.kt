package com.timilehinaregbesola.mathalarm.utils

import android.text.format.DateFormat
import com.timilehinaregbesola.mathalarm.domain.model.Alarm
import java.util.*

const val SUN = 0
const val MON = 1
const val TUE = 2
const val WED = 3
const val THU = 4
const val FRI = 5
const val SAT = 6
const val EASY = 0
const val MEDIUM = 1
const val HARD = 2

val days = listOf("S", "M", "T", "W", "T", "F", "S")
val fullDays = listOf("Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")

// Get the formatted time (example: 12:00 AM)
fun Alarm.getFormatTime(): CharSequence? {
    val cal = Calendar.getInstance()
    cal[Calendar.HOUR_OF_DAY] = hour
    cal[Calendar.MINUTE] = minute
    return DateFormat.format("hh:mm a", cal)
}

fun Alarm.getTime(): Calendar {
    val cal = Calendar.getInstance()
    cal[Calendar.HOUR_OF_DAY] = hour
    cal[Calendar.MINUTE] = minute
    return cal
}

fun Alarm.initCalendar(): Calendar {
    val cal = Calendar.getInstance()
    cal[Calendar.HOUR_OF_DAY] = hour
    cal[Calendar.MINUTE] = minute
    cal[Calendar.SECOND] = 0
    return cal
}

fun Alarm.getTimeLeft(time: Long, cal: Calendar): String {
    val message: String
    val calender = getCalendarFromAlarm(alarm = this, cal = cal)
    val today = getDayOfWeek(calender[Calendar.DAY_OF_WEEK])
    var i: Int
    val lastAlarmDay: Int
    val nextAlarmDay: Int
    if (System.currentTimeMillis() > time) {
        nextAlarmDay = if (today + 1 == 7) 0 else today + 1
        lastAlarmDay = today
    } else {
        nextAlarmDay = today
        lastAlarmDay = if (today - 1 == -1) 6 else today - 1
    }
    i = nextAlarmDay
    while (i != lastAlarmDay) {
        if (i == 7) {
            i = 0
        }
        if (repeatDays[i] == 'T') {
            break
        }
        i++
    }
    if (i < today || i == today && calender.timeInMillis < System.currentTimeMillis()) {
        val daysUntilAlarm: Int = SAT - today + 1 + i
        calender.add(Calendar.DAY_OF_YEAR, daysUntilAlarm)
    } else {
        val daysUntilAlarm = i - today
        calender.add(Calendar.DAY_OF_YEAR, daysUntilAlarm)
    }
    val alarmTime = calender.timeInMillis
    val remainderTime = alarmTime - System.currentTimeMillis()
    val minutes = (remainderTime / (1000 * 60) % 60).toInt()
    val hours = (remainderTime / (1000 * 60 * 60) % 24).toInt()
    val days = (remainderTime / (1000 * 60 * 60 * 24)).toInt()
    val mString = if (minutes == 1) "minute" else "minutes"
    val hString = if (hours == 1) "hour" else "hours"
    val dString = if (days == 1) "day" else "days"
    message = if (days == 0) {
        if (hours == 0) {
            ("$minutes $mString")
        } else {
            ("$hours $hString $minutes $mString")
        }
    } else {
        (
            " $days $dString $hours $hString $minutes $mString "
            )
    }
    return message
}

fun getCalendarFromAlarm(alarm: Alarm, cal: Calendar): Calendar {
    cal[Calendar.DAY_OF_WEEK] = alarm.repeatDays.toList().indexOfFirst { it == 'T' } + 1
    cal[Calendar.HOUR_OF_DAY] = alarm.hour
    cal[Calendar.MINUTE] = alarm.minute
    cal[Calendar.SECOND] = 0
    return cal
}

fun getDayOfWeek(day: Int): Int {
    val dayOfWeek: Int
    val errorValue = 8
    dayOfWeek = when (day) {
        Calendar.SUNDAY -> SUN
        Calendar.MONDAY -> MON
        Calendar.TUESDAY -> TUE
        Calendar.WEDNESDAY -> WED
        Calendar.THURSDAY -> THU
        Calendar.FRIDAY -> FRI
        Calendar.SATURDAY -> SAT
        else -> return errorValue
    }
    return dayOfWeek
}
