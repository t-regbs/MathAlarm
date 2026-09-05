package com.timilehinaregbesola.mathalarm.provider

import com.timilehinaregbesola.mathalarm.domain.model.Alarm
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.LocalTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.plus
import kotlinx.datetime.toInstant
import kotlin.time.ExperimentalTime

@OptIn(ExperimentalTime::class)
class AlarmTimeCalculatorImpl(
    private val dateTimeProvider: DateTimeProvider = DateTimeProviderImpl()
) : AlarmTimeCalculator {

    companion object {
        private const val SUN = 0
        private const val SAT = 6
    }

    override fun calculateAlarmTimes(alarm: Alarm): List<Long> {
        val tz = TimeZone.currentSystemDefault()
        val localNow = dateTimeProvider.getCurrentDateTime()
        val nowInstant = localNow.toInstant(tz)
        val todayDate = localNow.date
        val currentDayIndex = todayDate.dayOfWeek.toIndex()

        val times = mutableListOf<Long>()

        // Determine which days to schedule
        val repeatDays = if (alarm.repeatDays == "FFFFFFF") {
            // No repeat days set - determine the closest possible date
            val alarmTimeToday = LocalDateTime(
                date = todayDate,
                time = LocalTime(alarm.hour, alarm.minute, 0)
            )
            val alarmInstantToday = alarmTimeToday.toInstant(tz)

            val dayIndex = if (alarmInstantToday > nowInstant) {
                currentDayIndex // Set for today
            } else {
                // Set for tomorrow
                if (currentDayIndex == SAT) SUN else currentDayIndex + 1
            }

            "FFFFFFF".toCharArray().apply {
                this[dayIndex] = 'T'
            }.concatToString()
        } else {
            alarm.repeatDays
        }

        for (i in SUN..SAT) {
            if (repeatDays.getOrNull(i) == 'T') {
                val targetDate = calculateTargetDate(
                    currentDayIndex = currentDayIndex,
                    targetDayIndex = i,
                    todayDate = todayDate,
                    alarmHour = alarm.hour,
                    alarmMinute = alarm.minute,
                    nowInstant = nowInstant,
                    tz = tz
                )

                val targetDateTime = LocalDateTime(
                    date = targetDate,
                    time = LocalTime(alarm.hour, alarm.minute, 0)
                )

                times.add(targetDateTime.toInstant(tz).toEpochMilliseconds())
            }
        }

        return times
    }

    override fun calculateNextAlarmTime(alarm: Alarm): Long? {
        val times = calculateAlarmTimes(alarm)
        return times.minOrNull()
    }

    @OptIn(ExperimentalTime::class)
    override fun isInFuture(timeInMillis: Long): Boolean {
        val tz = TimeZone.currentSystemDefault()
        val currentTime = dateTimeProvider.getCurrentDateTime().toInstant(tz).toEpochMilliseconds()
        return timeInMillis > currentTime
    }

    private fun calculateTargetDate(
        currentDayIndex: Int,
        targetDayIndex: Int,
        todayDate: kotlinx.datetime.LocalDate,
        alarmHour: Int,
        alarmMinute: Int,
        nowInstant: kotlin.time.Instant,
        tz: TimeZone
    ): kotlinx.datetime.LocalDate {
        val alarmTimeToday = LocalDateTime(
            date = todayDate,
            time = LocalTime(alarmHour, alarmMinute, 0)
        )
        val alarmInstantToday = alarmTimeToday.toInstant(tz)
        val isPastToday = alarmInstantToday <= nowInstant

        val daysUntilAlarm = if (currentDayIndex > targetDayIndex || (currentDayIndex == targetDayIndex && isPastToday)) {
            // Schedule for next week
            SAT - currentDayIndex + 1 + targetDayIndex
        } else {
            // Schedule for this week
            targetDayIndex - currentDayIndex
        }

        return todayDate.plus(DatePeriod(days = daysUntilAlarm))
    }

    private fun DayOfWeek.toIndex(): Int = when (this) {
        DayOfWeek.SUNDAY -> 0
        DayOfWeek.MONDAY -> 1
        DayOfWeek.TUESDAY -> 2
        DayOfWeek.WEDNESDAY -> 3
        DayOfWeek.THURSDAY -> 4
        DayOfWeek.FRIDAY -> 5
        DayOfWeek.SATURDAY -> 6
    }
}
