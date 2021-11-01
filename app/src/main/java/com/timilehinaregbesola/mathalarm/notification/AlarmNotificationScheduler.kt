package com.timilehinaregbesola.mathalarm.notification

import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import com.timilehinaregbesola.mathalarm.AlarmReceiver
import com.timilehinaregbesola.mathalarm.AlarmReceiver.Companion.ALARM_ACTION
import com.timilehinaregbesola.mathalarm.AlarmReceiver.Companion.EXTRA_TASK
import com.timilehinaregbesola.mathalarm.R
import com.timilehinaregbesola.mathalarm.domain.model.Alarm
import com.timilehinaregbesola.mathalarm.utils.*
import timber.log.Timber
import java.util.*

/**
 * Alarm manager to schedule an event based on the time from a Alarm.
 */
class AlarmNotificationScheduler(private val context: Context) {

    /**
     * Schedules all the alarm of the object at once including repeating ones
     *
     * @param passedAlarm alarm to be scheduled
     * @param reschedule whether alarm is repeating
     */
    fun scheduleAlarm(passedAlarm: Alarm, reschedule: Boolean): Boolean {
        Timber.d("Schedule alarm..")
        val alarmIntent = Intent(context, AlarmReceiver::class.java).apply {
            action = ALARM_ACTION
            putExtra(EXTRA_TASK, passedAlarm.alarmId)
        }
        val alarmIntentList: MutableList<PendingIntent> = ArrayList()
        val time: MutableList<Calendar> = ArrayList()
        // If there is no day set, set the alarm on the closest possible date
        if (passedAlarm.repeatDays == "FFFFFFF") {
            val cal = passedAlarm.initCalendar()
            var dayOfTheWeek = getDayOfWeek(cal[Calendar.DAY_OF_WEEK])
            if (cal.timeInMillis > System.currentTimeMillis()) { // set it today
                val sb = StringBuilder("FFFFFFF")
                sb.setCharAt(dayOfTheWeek, 'T')
                passedAlarm.repeatDays = sb.toString()
            } else { // alarm time already passed for the day so set it tomorrow
                val sb = StringBuilder("FFFFFFF")
                if (dayOfTheWeek == SAT) { // if it is saturday
                    dayOfTheWeek = SUN
                } else {
                    dayOfTheWeek++
                }
                sb.setCharAt(dayOfTheWeek, 'T')
                passedAlarm.repeatDays = sb.toString()
            }
        }
        (SUN..SAT).forEach { i ->
            if (passedAlarm.repeatDays[i] == 'T') {
                val daysUntilAlarm: Int
                val cal = passedAlarm.initCalendar()
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
//                val stringId: StringBuilder = StringBuilder().append(i)
//                    .append(passedAlarm.hour).append(passedAlarm.minute)
                val stringId: StringBuilder = StringBuilder().append(passedAlarm.alarmId).append(i)
                    .append(passedAlarm.hour).append(passedAlarm.minute)
                val id = stringId.toString().split("-").joinToString("")
                val intentId = id.toInt()
                Timber.d("intent id: $intentId")
                // check if a previous alarm has been set
                if (PendingIntent.getBroadcast(
                        context, intentId, alarmIntent, PendingIntent.FLAG_NO_CREATE
                    ) != null
                ) {
                    if (!reschedule) {
                        context.showToast(R.string.alarm_duplicate_toast_text)
                    }
                    return false
                }
                val pendingIntent = PendingIntent.getBroadcast(
                    context, intentId, alarmIntent, PendingIntent.FLAG_CANCEL_CURRENT
                )
                alarmIntentList.add(pendingIntent)
                time.add(cal)
            }
        }
        for (i in alarmIntentList.indices) {
            val pendingIntent = alarmIntentList[i]
            val cal = time[i]
            context.setAlarm(cal.timeInMillis, pendingIntent)
            Timber.d("scheduled new alarm")
        }
        return true
    }

    /**
     * Cancels an alarm - Called when an alarm is turned off, deleted, and rescheduled
     *
     * @param alarm alarm to be canceled
     */
    fun cancelAlarm(alarm: Alarm) {
        val receiverIntent = Intent(context, AlarmReceiver::class.java)
        receiverIntent.action = ALARM_ACTION
        for (i in 0..6) { // For each day of the week
            if (alarm.repeatDays[i] == 'T') {
//                val stringId: StringBuilder = StringBuilder().append(i)
//                    .append(alarm.hour).append(alarm.minute)
                val stringId: StringBuilder = StringBuilder().append(alarm.alarmId).append(i)
                val intentId = stringId.toString().toInt()
                val cancelPendingIntent = PendingIntent.getBroadcast(
                    context, intentId, receiverIntent,
                    PendingIntent.FLAG_UPDATE_CURRENT
                )
                context.cancelAlarm(cancelPendingIntent)
                cancelPendingIntent.cancel()
            }
        }
    }

//    companion object : KLogging()
}
