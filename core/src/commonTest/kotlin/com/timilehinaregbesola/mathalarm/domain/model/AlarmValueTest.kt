package com.timilehinaregbesola.mathalarm.domain.model

import kotlinx.datetime.LocalDateTime
import kotlin.test.Test
import kotlin.test.assertEquals

class AlarmValueTest {
    @Test
    fun `creation date does not participate in alarm equality`() {
        val first = Alarm.newAlarm(LocalDateTime(2030, 1, 6, 7, 30))
        val later = Alarm.newAlarm(LocalDateTime(2040, 6, 1, 7, 30))
        assertEquals(first, later)
        assertEquals(first.hashCode(), later.hashCode())
        assertEquals(Alarm(hour = 7, minute = 30), first)
    }

    @Test
    fun `new draft takes hour and minute from one supplied instant`() {
        for (now in listOf(LocalDateTime(2030, 12, 31, 23, 59), LocalDateTime(2031, 1, 1, 0, 0))) {
            val draft = Alarm.newAlarm(now)
            assertEquals(now.hour, draft.hour)
            assertEquals(now.minute, draft.minute)
            assertEquals(0L, draft.alarmId)
            assertEquals(false, draft.isSaved)
        }
    }
}
