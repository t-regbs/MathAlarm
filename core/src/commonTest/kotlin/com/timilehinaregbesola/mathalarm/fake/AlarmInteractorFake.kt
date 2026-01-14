package com.timilehinaregbesola.mathalarm.fake

import com.timilehinaregbesola.mathalarm.domain.model.Alarm
import com.timilehinaregbesola.mathalarm.interactors.AlarmInteractor
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

class AlarmInteractorFake : AlarmInteractor {
    private val alarmMap: MutableMap<Long, MutableList<FakeData>> = mutableMapOf()
    
    override fun schedule(alarm: Alarm, timeInMillis: Long) {
        val instant = Instant.fromEpochMilliseconds(timeInMillis)
        val dateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
        
        val existingTimes = alarmMap.getOrPut(alarm.alarmId) { mutableListOf() }
        existingTimes.add(FakeData(timeInMillis, dateTime))
    }

    override fun cancel(alarm: Alarm) {
        alarmMap.remove(alarm.alarmId)
    }

    override fun update(alarm: Alarm) {
        // For testing, update just marks all scheduled times as updated
        val existing = alarmMap[alarm.alarmId]
        if (existing != null) {
            alarmMap[alarm.alarmId] = existing.map { it.copy(updated = true) }.toMutableList()
        }
    }

    fun isAlarmScheduled(alarm: Alarm): Boolean = 
        alarmMap[alarm.alarmId]?.isNotEmpty() ?: false

    fun clear() = alarmMap.clear()

    fun getAlarmTimeMillis(alarmId: Long): Long? = 
        alarmMap[alarmId]?.minByOrNull { it.timeInMillis }?.timeInMillis

    fun getScheduledAlarms(): Map<Long, List<FakeData>> = 
        alarmMap.mapValues { it.value.toList() }
}

data class FakeData(
    val timeInMillis: Long,
    val dateTime: LocalDateTime,
    val updated: Boolean = false
)
