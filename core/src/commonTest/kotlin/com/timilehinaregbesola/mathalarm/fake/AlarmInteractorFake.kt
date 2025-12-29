package com.timilehinaregbesola.mathalarm.fake

import com.timilehinaregbesola.mathalarm.domain.model.Alarm
import com.timilehinaregbesola.mathalarm.interactors.AlarmInteractor
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Clock
import kotlin.time.ExperimentalTime

class AlarmInteractorFake : AlarmInteractor {
    private val alarmMap: MutableMap<Long, FakeData> = mutableMapOf()
    
    override fun schedule(alarm: Alarm, reschedule: Boolean): Boolean {
        alarmMap[alarm.alarmId] = FakeData(reschedule, getAlarmDateTime(alarm))
        return true
    }

    override fun cancel(alarm: Alarm) {
        alarmMap.remove(alarm.alarmId)
    }

    fun isAlarmScheduled(alarm: Alarm): Boolean = alarmMap.contains(alarm.alarmId)

    fun clear() = alarmMap.clear()

    fun getAlarmTime(alarmId: Long): LocalDateTime? =
        alarmMap[alarmId]?.time
        
    @OptIn(ExperimentalTime::class)
    private fun getAlarmDateTime(alarm: Alarm): LocalDateTime {
        val nowInstant = Clock.System.now()
        val tz = TimeZone.currentSystemDefault()
        val today = nowInstant.toLocalDateTime(tz).date
        return LocalDateTime(
            date = today,
            time = kotlinx.datetime.LocalTime(alarm.hour, alarm.minute, 0)
        )
    }
}

data class FakeData(val reschedule: Boolean, val time: LocalDateTime)
