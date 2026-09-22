package com.timilehinaregbesola.mathalarm.fake

import com.timilehinaregbesola.mathalarm.domain.model.Alarm
import com.timilehinaregbesola.mathalarm.interactors.AlarmInteractor
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Instant

class AlarmInteractorFake : AlarmInteractor {
    private val snoozes: MutableMap<Long, FakeData> = mutableMapOf()
    private val alarmMap: MutableMap<Long, FakeData> = mutableMapOf()
    
    override suspend fun schedule(alarm: Alarm, timeInMillis: Long) {
        val instant = Instant.fromEpochMilliseconds(timeInMillis)
        val dateTime = instant.toLocalDateTime(TimeZone.currentSystemDefault())
        alarmMap[alarm.alarmId] = FakeData(timeInMillis, dateTime)
    }

    override suspend fun scheduleSnooze(alarm: Alarm, timeInMillis: Long) {
        val dateTime = Instant.fromEpochMilliseconds(timeInMillis).toLocalDateTime(TimeZone.currentSystemDefault())
        snoozes[alarm.alarmId] = FakeData(timeInMillis, dateTime)
    }

    override fun cancelSnooze(alarm: Alarm) { snoozes.remove(alarm.alarmId) }

    override fun cancelRegularOccurrences(alarm: Alarm) { alarmMap.remove(alarm.alarmId) }

    override fun cancel(alarm: Alarm) {
        alarmMap.remove(alarm.alarmId)
        cancelSnooze(alarm)
    }

    override suspend fun update(alarm: Alarm) {
        val existing = alarmMap[alarm.alarmId]
        if (existing != null) {
            alarmMap[alarm.alarmId] = existing.copy(updated = true)
        }
    }

    fun isAlarmScheduled(alarm: Alarm): Boolean = alarmMap.contains(alarm.alarmId) || snoozes.contains(alarm.alarmId)

    fun clear() {
        alarmMap.clear()
        snoozes.clear()
    }

    fun getAlarmTimeMillis(alarmId: Long): Long? = nextOccurrence(alarmId)?.timeInMillis

    fun getAlarmTime(alarmId: Long): LocalDateTime? = nextOccurrence(alarmId)?.dateTime

    fun getScheduledAlarms(): Map<Long, FakeData> =
        (alarmMap.keys + snoozes.keys).associateWith { nextOccurrence(it)!! }

    private fun nextOccurrence(alarmId: Long): FakeData? =
        listOfNotNull(alarmMap[alarmId], snoozes[alarmId]).minByOrNull { it.timeInMillis }
}

data class FakeData(
    val timeInMillis: Long,
    val dateTime: LocalDateTime,
    val updated: Boolean = false
)
