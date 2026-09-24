package com.timilehinaregbesola.mathalarm.navigation

import com.timilehinaregbesola.mathalarm.framework.database.AlarmEntity
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull
import kotlinx.serialization.json.Json

class AlarmHandoffTest {
    @Test
    fun acceptsEscapedAlarmTitle() {
        val title = "Wake \"up\" \\ now\n☀️"
        val payload = Json.encodeToString(
            AlarmEntity(
                alarmId = 7,
                hour = 7,
                minute = 0,
                repeat = false,
                repeatDays = "FFFFFFF",
                isOn = true,
                difficulty = 1,
                alarmTone = "alarm_classic",
                vibrate = false,
                snooze = 5,
                title = title,
                isSaved = true,
            )
        )

        assertEquals(title, decodeAlarmHandoff(payload)?.title)
    }

    @Test
    fun rejectsMalformedAndUnidentifiedPayloads() {
        assertNull(decodeAlarmHandoff("{\"title\":\"bad \\\" title\"}"))
        assertNull(decodeAlarmHandoff("{}"))
        assertNull(decodeAlarmHandoff("{\"alarmId\":0}"))
    }
}
