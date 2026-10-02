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

        assertEquals(title, decodeAlarmSnapshot(payload)?.title)
        assertEquals(AlarmHandoff(7), decodeAlarmHandoff(payload))
    }

    @Test
    fun rejectsMalformedAndUnidentifiedPayloads() {
        assertNull(decodeAlarmHandoff("{\"title\":\"bad \\\" title\"}"))
        assertNull(decodeAlarmHandoff("{}"))
        assertNull(decodeAlarmHandoff("{\"alarmId\":0}"))
        assertNull(decodeAlarmHandoff("{\"alarmId\":-1}"))
        assertNull(decodeAlarmHandoff("{\"alarmId\":\"not an ID\"}"))
    }

    @Test
    fun identityOnlyHandoffsRoundTripWithOptionalOccurrence() {
        for (handoff in listOf(AlarmHandoff(7), AlarmHandoff(7, 1000))) {
            assertEquals(handoff, decodeAlarmHandoff(encodeAlarmHandoff(handoff)))
        }
        assertEquals("{\"alarmId\":7}", encodeAlarmHandoff(AlarmHandoff(7)))
    }

}
