package com.timilehinaregbesola.mathalarm.framework.database

import com.timilehinaregbesola.mathalarm.domain.model.Alarm
import kotlin.test.Test
import kotlin.test.assertEquals

class AlarmMapperTest {
    @Test
    fun `stored alarm equality is stable across reconstruction`() {
        val mapper = AlarmMapper()
        val saved = Alarm(alarmId = 42, hour = 6, minute = 30, isSaved = true,
            title = "Morning", isOn = true, pendingTimes = listOf(1000L, 2000L),
            scheduleInitialized = true, activeAt = 1000L)
        val entity = mapper.mapFromDomainModel(saved)
        val first = mapper.mapToDomainModel(entity)
        val reconstructed = mapper.mapToDomainModel(entity)
        assertEquals(saved, first)
        assertEquals(first, reconstructed)
        assertEquals(first.hashCode(), reconstructed.hashCode())
        assertEquals(entity, mapper.mapFromDomainModel(reconstructed))
    }
}
