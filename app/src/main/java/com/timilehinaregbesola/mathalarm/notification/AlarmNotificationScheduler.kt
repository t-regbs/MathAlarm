package com.timilehinaregbesola.mathalarm.notification

import android.annotation.SuppressLint
import android.app.PendingIntent
import android.app.PendingIntent.FLAG_MUTABLE
import android.app.PendingIntent.FLAG_UPDATE_CURRENT
import android.content.Context
import android.content.Intent
import android.os.Build
import co.touchlab.kermit.Logger
import com.timilehinaregbesola.mathalarm.AlarmReceiver
import com.timilehinaregbesola.mathalarm.AlarmReceiver.Companion.ALARM_ACTION
import com.timilehinaregbesola.mathalarm.AlarmReceiver.Companion.EXTRA_TASK
import com.timilehinaregbesola.mathalarm.domain.model.Alarm
import com.timilehinaregbesola.mathalarm.utils.SAT
import com.timilehinaregbesola.mathalarm.utils.SUN
import com.timilehinaregbesola.mathalarm.utils.cancelAlarm
import com.timilehinaregbesola.mathalarm.utils.fullDays
import com.timilehinaregbesola.mathalarm.utils.initLocalDateTimeInSystemZone
import com.timilehinaregbesola.mathalarm.utils.setExactAlarm
import com.timilehinaregbesola.mathalarm.utils.toIndex
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.LocalDate
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

/**
 * Alarm manager to schedule an event based on the time from a Alarm.
 */
class AlarmNotificationScheduler(private val context: Context, private val logger: Logger) {

    /**
     * Schedules all the alarm of the object at once including repeating ones
     *
     * @param passedAlarm alarm to be scheduled
     * @param reschedule whether alarm is repeating
     */
    @OptIn(ExperimentalTime::class)
    @SuppressLint("UnspecifiedImmutableFlag")
    fun scheduleAlarm(passedAlarm: Alarm, reschedule: Boolean): Boolean {
        logger.d("Schedule alarm for id=${passedAlarm.alarmId}, time=${passedAlarm.hour}:${passedAlarm.minute}, repeat=${passedAlarm.repeat}, repeatDays=${passedAlarm.repeatDays}, reschedule=$reschedule")
        val alarmIntent = Intent(context, AlarmReceiver::class.java).apply {
            action = ALARM_ACTION
            putExtra(EXTRA_TASK, passedAlarm.alarmId)
        }
        val alarmIntentList: MutableList<PendingIntent> = ArrayList()
        val timeInstants: MutableList<Instant> = ArrayList()
        val tz = TimeZone.currentSystemDefault()
        var hasExistingAlarms = false

        // If there is no day set, set the alarm on the closest possible date
        if (passedAlarm.repeatDays == "FFFFFFF") {
            logger.d("No repeat days set, determining closest possible date")
            val dateTime = passedAlarm.initLocalDateTimeInSystemZone()
            val instant = dateTime.toInstant(tz)
            val nowInstant = Clock.System.now()
            logger.d("Alarm datetime: $dateTime, instant: $instant, now: $nowInstant")

            var dayOfTheWeek = dateTime.date.dayOfWeek.toIndex()
            logger.d("Current day of week: $dayOfTheWeek")

            if (instant > nowInstant) { // set it today
                val sb = StringBuilder("FFFFFFF")
                sb.setCharAt(dayOfTheWeek, 'T')
                passedAlarm.repeatDays = sb.toString()
                logger.d("Alarm time is in the future, setting for today. New repeatDays: ${passedAlarm.repeatDays}")
            } else { // alarm time already passed for the day so set it tomorrow
                val sb = StringBuilder("FFFFFFF")
                if (dayOfTheWeek == SAT) { // if it is saturday
                    dayOfTheWeek = SUN
                } else {
                    dayOfTheWeek++
                }
                sb.setCharAt(dayOfTheWeek, 'T')
                passedAlarm.repeatDays = sb.toString()
                logger.d("Alarm time already passed, setting for tomorrow (day $dayOfTheWeek). New repeatDays: ${passedAlarm.repeatDays}")
            }
        }

        for (i in SUN..SAT) {
            if (passedAlarm.repeatDays[i] == 'T') {
                logger.d("Processing day $i (${fullDays[i]}) which is set to true")
                val nowInstant = Clock.System.now()
                val localNow = nowInstant.toLocalDateTime(tz)
                val todayDate = localNow.date

                val currentDay = todayDate.dayOfWeek.toIndex()

                logger.d("Current day: $currentDay (${fullDays[currentDay]})")

                val daysUntilAlarm: Int
                val targetDate: LocalDate

                val alarmTimeToday = passedAlarm.initLocalDateTimeInSystemZone()
                val alarmInstantToday = alarmTimeToday.toInstant(tz)
                logger.d("Alarm time today would be: $alarmTimeToday (${alarmInstantToday})")
                logger.d("Current time is: $localNow (${nowInstant})")

                val isPastToday = alarmInstantToday < nowInstant
                logger.d("Is alarm time past for today? $isPastToday")

                if (currentDay > i || (currentDay == i && isPastToday)) {
                    // days left till end of week(sat) + the day of the week of the alarm
                    // EX: alarm = i = tues = 2; current = wed = 3; end of week = sat = 6
                    // end - current = 6 - 3 = 3 -> 3 days till saturday/end of week
                    // end of week + 1 (to sunday) + day of week alarm is on = 3 + 1 + 2 = 6
                    daysUntilAlarm = SAT - currentDay + 1 + i
                    targetDate = todayDate.plus(DatePeriod(days = daysUntilAlarm))
                    logger.d("Current day ($currentDay) > alarm day ($i) or same day but time passed, scheduling for next week")
                    logger.d("Days until alarm: $daysUntilAlarm, target date: $targetDate")
                } else {
                    daysUntilAlarm = i - currentDay
                    targetDate = todayDate.plus(DatePeriod(days = daysUntilAlarm))
                    logger.d("Current day ($currentDay) <= alarm day ($i) and time not passed, scheduling for this week")
                    logger.d("Days until alarm: $daysUntilAlarm, target date: $targetDate")
                }

                val targetDateTime = LocalDateTime(
                    date = targetDate,
                    time = LocalTime(passedAlarm.hour, passedAlarm.minute, 0)
                )
                val targetInstant = targetDateTime.toInstant(tz)

                val stringId: StringBuilder = StringBuilder().append(passedAlarm.alarmId).append(i)
                    .append(passedAlarm.hour).append(passedAlarm.minute)
                val id = stringId.toString().split("-").joinToString("")
                val intentId = id.toInt()
                logger.d("Generated intent ID: $intentId for alarm ID: ${passedAlarm.alarmId}, day: $i, time: ${passedAlarm.hour}:${passedAlarm.minute}")

                // Check if a previous alarm has been set
                logger.d("Checking if a previous alarm with this ID already exists")
                val isSet = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    PendingIntent.getBroadcast(
                        context,
                        intentId,
                        alarmIntent,
                        PendingIntent.FLAG_NO_CREATE or FLAG_MUTABLE,
                    )
                } else {
                    PendingIntent.getBroadcast(context, intentId, alarmIntent, PendingIntent.FLAG_NO_CREATE)
                }

                if (isSet != null) {
                    logger.d("An alarm with ID $intentId already exists")
                    hasExistingAlarms = true
                    if (!reschedule) {
                        logger.d("Not rescheduling because reschedule flag is false")
                        // context.showToast(R.string.alarm_duplicate_toast_text)
                    } else {
                        // If reschedule is true, cancel the existing alarm and create a new one
                        logger.d("Canceling existing alarm because reschedule flag is true")
                        context.cancelAlarm(isSet)
                        isSet.cancel()
                    }
                }

                // If reschedule is true or no existing alarm was found, create a new one
                if (isSet == null || reschedule) {
                    logger.d("Proceeding to create new alarm")

                    val pendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                        PendingIntent.getBroadcast(
                            context,
                            intentId,
                            alarmIntent,
                            PendingIntent.FLAG_CANCEL_CURRENT or FLAG_MUTABLE,
                        )
                    } else {
                        PendingIntent.getBroadcast(
                            context,
                            intentId,
                            alarmIntent,
                            PendingIntent.FLAG_CANCEL_CURRENT,
                        )
                    }

                    alarmIntentList.add(pendingIntent)
                    timeInstants.add(targetInstant)
                }
            }
        }

        // Return true if we scheduled new alarms OR if there were existing alarms
        if (alarmIntentList.isEmpty() && !hasExistingAlarms) {
            logger.w("No alarms were scheduled and no existing alarms found")
            return false
        }

        logger.d("Scheduling ${alarmIntentList.size} alarms")
        for (i in alarmIntentList.indices) {
            val pendingIntent = alarmIntentList[i]
            val instant = timeInstants[i]
            logger.d("Scheduling alarm #${i+1}/${alarmIntentList.size} for time: ${instant}")
            context.setExactAlarm(instant.toEpochMilliseconds(), pendingIntent)
            logger.d("Alarm #${i+1} scheduled successfully")
        }

        logger.d("All ${alarmIntentList.size} alarms scheduled successfully, returning true")
        return true
    }

    /**
     * Cancels an alarm - Called when an alarm is turned off, deleted, and rescheduled
     *
     * @param alarm alarm to be canceled
     */
    fun cancelAlarm(alarm: Alarm) {
        logger.d("AlarmNotificationScheduler.cancelAlarm called: alarmId=${alarm.alarmId}, time=${alarm.hour}:${alarm.minute}, repeat=${alarm.repeat}, repeatDays=${alarm.repeatDays}")

        val receiverIntent = Intent(context, AlarmReceiver::class.java)
        receiverIntent.action = ALARM_ACTION
        receiverIntent.putExtra(EXTRA_TASK, alarm.alarmId)

        var canceledCount = 0
        for (i in 0..6) { // For each day of the week
            if (alarm.repeatDays.getOrNull(i) == 'T') {
                logger.d("Canceling alarm for day $i (${fullDays[i]})")

                val stringId: StringBuilder = StringBuilder().append(alarm.alarmId).append(i)
                    .append(alarm.hour).append(alarm.minute)
                val id = stringId.toString().split("-").joinToString("")
                val intentId = id.toInt()
                logger.d("Generated intent ID: $intentId for alarm ID: ${alarm.alarmId}, day: $i, time: ${alarm.hour}:${alarm.minute}")

                val cancelPendingIntent = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    PendingIntent.getBroadcast(
                        context,
                        intentId,
                        receiverIntent,
                        FLAG_UPDATE_CURRENT or FLAG_MUTABLE,
                    )
                } else {
                    PendingIntent.getBroadcast(
                        context,
                        intentId,
                        receiverIntent,
                        FLAG_UPDATE_CURRENT,
                    )
                }

                logger.d("Calling context.cancelAlarm for intent ID: $intentId")
                context.cancelAlarm(cancelPendingIntent)
                cancelPendingIntent.cancel()
                logger.d("Alarm canceled for day $i (${fullDays[i]})")
                canceledCount++
            }
        }

        logger.d("AlarmNotificationScheduler.cancelAlarm completed for alarmId=${alarm.alarmId}, canceled $canceledCount alarms")
    }
}
