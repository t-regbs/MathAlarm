package com.timilehinaregbesola.mathalarm.fake

import com.timilehinaregbesola.mathalarm.domain.model.Alarm
import com.timilehinaregbesola.mathalarm.interactors.AlarmInteractor
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

class AlarmInteractorFake : AlarmInteractor {
    private val alarmMap: MutableMap<Long, FakeData> = mutableMapOf()
    
    override fun schedule(alarm: Alarm, timeInMillis: Long) {
        val instant = Instant.fromEpochMilliseconds(timeInMillis)
        val dateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
        alarmMap[alarm.alarmId] = FakeData(timeInMillis, dateTime)
    }

    override fun cancel(alarm: Alarm) {
        alarmMap.remove(alarm.alarmId)
    }

    override fun update(alarm: Alarm) {
        // For testing, update just marks the alarm as updated
        val existing = alarmMap[alarm.alarmId]
        if (existing != null) {
            alarmMap[alarm.alarmId] = existing.copy(updated = true)
        }
    }

    fun isAlarmScheduled(alarm: Alarm): Boolean = alarmMap.contains(alarm.alarmId)

    fun clear() = alarmMap.clear()

    fun getAlarmTimeMillis(alarmId: Long): Long? = alarmMap[alarmId]?.timeInMillis

    fun getScheduledAlarms(): Map<Long, FakeData> = alarmMap.toMap()
}

data class FakeData(
    val timeInMillis: Long,
    val dateTime: LocalDateTime,
    val updated: Boolean = false
)
