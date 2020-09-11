package com.android.example.mathalarm.utils

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.text.format.DateFormat
import android.widget.Toast
import com.android.example.mathalarm.AlarmReceiver
import com.android.example.mathalarm.R
import com.android.example.mathalarm.database.Alarm
import timber.log.Timber
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

const val ALARM_EXTRA = "alarm_extra"

//Get the formatted time (example: 12:00 AM)
fun Alarm.getFormatTime(): CharSequence? {
    val cal = Calendar.getInstance()
    cal[Calendar.HOUR_OF_DAY] = hour
    cal[Calendar.MINUTE] = minute
    return DateFormat.format("hh:mm a", cal)
}

//Schedules all the alarm of the object at once including repeating ones
fun Alarm.scheduleAlarm(context: Context): Boolean {
    Timber.d("Schedule alarm..")
    val alarm = Intent(context, AlarmReceiver::class.java)
    alarm.putExtra(ALARM_EXTRA, alarmId)
    val alarmIntent: MutableList<PendingIntent> = ArrayList()
    val time: MutableList<Calendar> = ArrayList()
    // If there is no day set, set the alarm on the closest possible date
    if (repeatDays == "FFFFFFF") {
        val cal = initCalendar()
        var dayOfTheWeek = getDayOfWeek(cal[Calendar.DAY_OF_WEEK])
        if (cal.timeInMillis > System.currentTimeMillis()) { //set it today
            val sb = StringBuilder("FFFFFFF")
            sb.setCharAt(dayOfTheWeek, 'T')
            repeatDays = sb.toString()
        } else { //alarm time already passed for the day so set it tomorrow
            val sb = StringBuilder("FFFFFFF")
            if (dayOfTheWeek == SAT) { //if it is saturday
                dayOfTheWeek = SUN
            } else {
                dayOfTheWeek++
            }
            sb.setCharAt(dayOfTheWeek, 'T')
            repeatDays = sb.toString()
        }
    }
    for (i in SUN..SAT) {
        if (repeatDays[i] == 'T') {
            var daysUntilAlarm: Int
            val cal = initCalendar()
            val currentDay = getDayOfWeek(cal[Calendar.DAY_OF_WEEK])
            Timber.d("current day: $currentDay")
            if (currentDay > i ||
                (currentDay == i && cal.timeInMillis < System.currentTimeMillis())) {
                //days left till end of week(sat) + the day of the week of the alarm
                // EX: alarm = i = tues = 2; current = wed = 3; end of week = sat = 6
                //end - current = 6 - 3 = 3 -> 3 days till saturday/end of week
                //end of week + 1 (to sunday) + day of week alarm is on = 3 + 1 + 2 = 6
                daysUntilAlarm = SAT - currentDay + 1 + i
                cal.add(Calendar.DAY_OF_YEAR, daysUntilAlarm)
                Timber.d("days until alarm: $daysUntilAlarm")
            } else {
                daysUntilAlarm = i - currentDay
                cal.add(Calendar.DAY_OF_YEAR, daysUntilAlarm)
                Timber.d("days until alarm: $daysUntilAlarm")
            }
            val stringId: StringBuilder = StringBuilder().append(i)
                .append(hour).append(minute)
            val id = stringId.toString().split("-").joinToString("")
            val intentId = id.toInt()
            Timber.d("intent id: $intentId")
            //check if a previous alarm has been set
            if (PendingIntent.getBroadcast(
                    context, intentId, alarm,
                    PendingIntent.FLAG_NO_CREATE) != null) {
                Toast.makeText(
                    context, context.getString(R.string.alarm_duplicate_toast_text),
                    Toast.LENGTH_SHORT
                ).show()
                return false
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context, intentId, alarm, PendingIntent.FLAG_CANCEL_CURRENT)
            alarmIntent.add(pendingIntent)
            time.add(cal)
        }
    }
    for (i in alarmIntent.indices) {
        val pendingIntent = alarmIntent[i]
        val cal = time[i]
        val alarmManager = context
            .getSystemService(Context.ALARM_SERVICE) as AlarmManager
        if (repeat) {
            alarmManager.setRepeating(
                AlarmManager.RTC_WAKEUP, cal.timeInMillis,
                AlarmManager.INTERVAL_DAY * 7, pendingIntent
            )
            Timber.d("scheduled new alarm")
        } else {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
                alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pendingIntent)
            } else {
                alarmManager.setExact(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pendingIntent)
            }
            Timber.d("scheduled new alarm")
        }
    }
    return true
}

//This gets called if snooze get pressed
fun Alarm.scheduleSnooze(context: Context) {
    val alarm = Intent(context, AlarmReceiver::class.java)
    alarm.putExtra(ALARM_EXTRA, alarmId)
    val cal = Calendar.getInstance()
    cal.add(Calendar.MINUTE, snooze)
    val alarmIntent = PendingIntent.getBroadcast(
        context, 0, alarm,
        PendingIntent.FLAG_CANCEL_CURRENT
    )
    val alarmManager = context
        .getSystemService(Context.ALARM_SERVICE) as AlarmManager
    alarmManager.set(AlarmManager.RTC_WAKEUP, cal.timeInMillis, alarmIntent)
    if (snooze == 1) {
        Toast.makeText(
            context,
            "${context.getString(R.string.alarm_set_begin_msg)} ${snooze}${context.getString(
                R.string.alarm_minute
            )} ${context.getString(R.string.alarm_set_end_msg)}",
            Toast.LENGTH_SHORT
        ).show()
    } else {
        Toast.makeText(
            context,
            "${context.getString(R.string.alarm_set_begin_msg)} ${snooze}${context.getString(
                R.string.alarm_minutes
            )} ${context.getString(R.string.alarm_set_end_msg)}",
            Toast.LENGTH_SHORT
        ).show()
    }
}

//Cancels an alarm - Called when an alarm is turned off, deleted, and rescheduled
fun Alarm.cancelAlarm(context: Context) {
    val cancel = Intent(context, AlarmReceiver::class.java)
    for (i in 0..6) { //For each day of the week
        if (repeatDays[i] == 'T') {
            val stringId: StringBuilder = StringBuilder().append(i)
                .append(hour).append(minute)
            val intentId = stringId.toString().toInt()
            val cancelAlarmPI = PendingIntent.getBroadcast(
                context, intentId, cancel,
                PendingIntent.FLAG_CANCEL_CURRENT
            )
            val alarmManager = context
                .getSystemService(Context.ALARM_SERVICE) as AlarmManager
            alarmManager.cancel(cancelAlarmPI)
            cancelAlarmPI.cancel()
        }
    }
}

//Used for displaying the toast for the the remaining time until the next alarm
fun Alarm.getTimeLeftMessage(context: Context): String? {
    val message: String
    val cal = Calendar.getInstance()
    cal[Calendar.HOUR_OF_DAY] = hour
    cal[Calendar.MINUTE] = minute
    cal[Calendar.SECOND] = 0
    val today =
        getDayOfWeek(cal[Calendar.DAY_OF_WEEK])
    var i: Int
    var lastAlarmDay: Int
    var nextAlarmDay: Int
    if (System.currentTimeMillis() > cal.timeInMillis) {
        nextAlarmDay = today + 1
        lastAlarmDay = today
        if (nextAlarmDay == 7) {
            nextAlarmDay = 0
        }
    } else {
        nextAlarmDay = today
        lastAlarmDay = today - 1
        if (lastAlarmDay == -1) {
            lastAlarmDay = 6
        }
    }
    i = nextAlarmDay
    while (i != lastAlarmDay) {
        if (i == 7) {
            i = 0
        }
        if (repeatDays.get(i) == 'T') {
            break
        }
        i++
    }
    if (i < today || i == today && cal.timeInMillis < System.currentTimeMillis()) {
        val daysUntilAlarm: Int = SAT - today + 1 + i
        cal.add(Calendar.DAY_OF_YEAR, daysUntilAlarm)
    } else {
        val daysUntilAlarm = i - today
        cal.add(Calendar.DAY_OF_YEAR, daysUntilAlarm)
    }
    val alarmTime = cal.timeInMillis
    val remainderTime = alarmTime - System.currentTimeMillis()
    val minutes = (remainderTime / (1000 * 60) % 60).toInt()
    val hours = (remainderTime / (1000 * 60 * 60) % 24).toInt()
    val days = (remainderTime / (1000 * 60 * 60 * 24)).toInt()
    val mString: String
    val hString: String
    val dString: String
    mString = if (minutes == 1) {
        context.getString(R.string.alarm_minute)
    } else {
        context.getString(R.string.alarm_minutes)
    }
    hString = if (hours == 1) {
        context.getString(R.string.alarm_hour)
    } else {
        context.getString(R.string.alarm_hours)
    }
    dString = if (days == 1) {
        context.getString(R.string.alarm_day)
    } else {
        context.getString(R.string.alarm_days)
    }
    message = if (days == 0) {
        if (hours == 0) {
            ("${context.getString(R.string.alarm_set_begin_msg)} $minutes $mString ${context.getString(
                R.string.alarm_set_end_msg
            )}")
        } else {
            ("${context.getString(R.string.alarm_set_begin_msg)} $hours $hString $minutes $mString ${context.getString(
                R.string.alarm_set_end_msg
            )}")
        }
    } else {
        (context.getString(R.string.alarm_set_begin_msg) + " "
                + days + " " + dString + " " + hours + " " + hString + " " + minutes + " " +
                mString + " " + context.getString(R.string.alarm_set_end_msg))
    }
    return message
}

fun Alarm.initCalendar(): Calendar {
    val cal = Calendar.getInstance()
    cal[Calendar.HOUR_OF_DAY] = hour
    cal[Calendar.MINUTE] = minute
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
