package com.timilehinaregbesola.mathalarm.analytics

import com.timilehinaregbesola.mathalarm.domain.model.Alarm
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

class AnalyticsEventsTest {
    @Test
    fun savedAlarmUsesOnlyNonIdentifyingConfiguration() {
        val event = AnalyticsEvents.alarmSaved(
            Alarm(title = "Private title", alarmTone = "content://private/tone", repeat = true,
                difficulty = 2, questionCount = 4, snooze = 5),
            isNew = true,
        )

        assertEquals("alarm_saved", event.name)
        assertEquals("create", event.labels["mode"])
        assertEquals("true", event.labels["repeat"])
        assertEquals(4L, event.counts["question_count"])
        assertFalse(event.toString().contains("Private title"))
        assertFalse(event.toString().contains("content://private/tone"))
    }
}
