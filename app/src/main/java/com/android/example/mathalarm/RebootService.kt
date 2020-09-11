package com.android.example.mathalarm

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.os.Build
import android.widget.Toast
import androidx.core.app.JobIntentService
import com.android.example.mathalarm.database.Alarm
import com.android.example.mathalarm.database.AlarmDao
import com.android.example.mathalarm.utils.*
import org.koin.android.ext.android.inject
import timber.log.Timber
import java.util.*


class RebootService : JobIntentService() {
    companion object {
        // Service unique ID
        const val SERVICE_JOB_ID = 55


        fun enqueueWork(context: Context, service: Intent) {
            enqueueWork(context, AlarmService::class.java, SERVICE_JOB_ID, service)
        }
    }


    override fun onHandleWork(intent: Intent) {
        onHandleIntent(intent)
    }
    private val dataSource: AlarmDao by inject()
//    private val myHelper: MyHelper by lazy { MyHelper() }
    private fun onHandleIntent(intent: Intent?) {
        if (intent?.extras?.get("service_extra") == "Reboot") {
            val alarms: List<Alarm> = dataSource.getActiveAlarms()
            for (i in alarms.indices) {
                val alarm: Alarm = alarms[i]
                Timber.d("alarm id: ${alarm.alarmId}")
                scheduleAlarm(this, alarm)
                Timber.d("Alarm scheduled")
            }
        }
    }

    private fun scheduleAlarm(context: Context, alarmm: Alarm): Boolean {
        Timber.d("Schedule alarm..")
        val alarm = Intent(context, AlarmService::class.java)
        alarm.putExtra(ALARM_EXTRA, alarmm.alarmId)
        val alarmIntent: MutableList<PendingIntent> = ArrayList()
        val time: MutableList<Calendar> = ArrayList()
        // If there is no day set, set the alarm on the closest possible date
        if (alarmm.repeatDays == "FFFFFFF") {
            val cal = alarmm.initCalendar()
            var dayOfTheWeek = getDayOfWeek(cal[Calendar.DAY_OF_WEEK])
            if (cal.timeInMillis > System.currentTimeMillis()) { //set it today
                val sb = StringBuilder("FFFFFFF")
                sb.setCharAt(dayOfTheWeek, 'T')
                alarmm.repeatDays = sb.toString()
            } else { //alarm time already passed for the day so set it tomorrow
                val sb = StringBuilder("FFFFFFF")
                if (dayOfTheWeek == SAT) { //if it is saturday
                    dayOfTheWeek = SUN
                } else {
                    dayOfTheWeek++
                }
                sb.setCharAt(dayOfTheWeek, 'T')
                alarmm.repeatDays = sb.toString()
            }
        }
        for (i in SUN..SAT) {
            if (alarmm.repeatDays[i] == 'T') {
                var daysUntilAlarm: Int
                val cal = alarmm.initCalendar()
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
                    .append(alarmm.hour).append(alarmm.minute)
                val id = stringId.toString().split("-").joinToString("")
                val intentId = id.toInt()
                Timber.d("intent id: $intentId")
                //check if a previous alarm has been set
                if (PendingIntent.getService(
                        context, intentId, alarm,
                        PendingIntent.FLAG_NO_CREATE) != null) {
                    Toast.makeText(
                        context, context.getString(R.string.alarm_duplicate_toast_text),
                        Toast.LENGTH_SHORT
                    ).show()
                    return false
                }
                val pendingIntent = PendingIntent.getService(
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
            if (alarmm.repeat) {
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
}

