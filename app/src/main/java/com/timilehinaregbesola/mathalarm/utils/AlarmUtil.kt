package com.timilehinaregbesola.mathalarm.utils

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.text.format.DateFormat
import android.widget.Toast
import com.timilehinaregbesola.mathalarm.AlarmReceiver
import com.timilehinaregbesola.mathalarm.R
import com.timilehinaregbesola.mathalarm.domain.model.Alarm
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

val days = listOf("S", "M", "T", "W", "T", "F", "S")
val fullDays = listOf("Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")

const val ALARM_EXTRA = "alarm_extra"

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

// Schedules all the alarm of the object at once including repeating ones
fun Alarm.scheduleAlarm(context: Context, reschedule: Boolean): Boolean {
    Timber.d("Schedule alarm..")
    val alarm = Intent(context, AlarmReceiver::class.java)
    alarm.putExtra(ALARM_EXTRA, alarmId)
    val alarmIntent: MutableList<PendingIntent> = ArrayList()
    val time: MutableList<Calendar> = ArrayList()
    // If there is no day set, set the alarm on the closest possible date
    if (repeatDays == "FFFFFFF") {
        val cal = initCalendar()
        var dayOfTheWeek = getDayOfWeek(cal[Calendar.DAY_OF_WEEK])
        if (cal.timeInMillis > System.currentTimeMillis()) { // set it today
            val sb = StringBuilder("FFFFFFF")
            sb.setCharAt(dayOfTheWeek, 'T')
            repeatDays = sb.toString()
        } else { // alarm time already passed for the day so set it tomorrow
            val sb = StringBuilder("FFFFFFF")
            if (dayOfTheWeek == SAT) { // if it is saturday
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
                (currentDay == i && cal.timeInMillis < System.currentTimeMillis())
            ) {
                // days left till end of week(sat) + the day of the week of the alarm
                // EX: alarm = i = tues = 2; current = wed = 3; end of week = sat = 6
                // end - current = 6 - 3 = 3 -> 3 days till saturday/end of week
                // end of week + 1 (to sunday) + day of week alarm is on = 3 + 1 + 2 = 6
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
            // check if a previous alarm has been set
            if (PendingIntent.getBroadcast(
                    context, intentId, alarm, PendingIntent.FLAG_NO_CREATE
                ) != null
            ) {
                if (!reschedule) {
                    Toast.makeText(
                        context, context.getString(R.string.alarm_duplicate_toast_text),
                        Toast.LENGTH_SHORT
                    ).show()
                }
                return false
            }
            val pendingIntent = PendingIntent.getBroadcast(
                context, intentId, alarm, PendingIntent.FLAG_CANCEL_CURRENT
            )
            alarmIntent.add(pendingIntent)
            time.add(cal)
        }
    }
    for (i in alarmIntent.indices) {
        val pendingIntent = alarmIntent[i]
        val cal = time[i]
        val alarmManager = context
            .getSystemService(Context.ALARM_SERVICE) as AlarmManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            alarmManager.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pendingIntent)
        } else {
            alarmManager.setExact(AlarmManager.RTC_WAKEUP, cal.timeInMillis, pendingIntent)
        }
        Timber.d("scheduled new alarm")
    }
    return true
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
