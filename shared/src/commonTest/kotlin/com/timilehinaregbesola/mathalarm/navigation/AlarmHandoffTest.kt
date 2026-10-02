package com.timilehinaregbesola.mathalarm.navigation

import com.timilehinaregbesola.mathalarm.framework.database.AlarmEntity
import com.timilehinaregbesola.mathalarm.utils.Destinations.AlarmMath
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


    @Test
    fun duplicateInitializedChallengeIsAcknowledgedButLaterDeliveryWaits() {
        val a = destination(1)
        val b = destination(2)
        assertEquals(AlarmHandoffAction.ACKNOWLEDGE,
            alarmHandoffAction(AlarmHandoff(1), listOf(a, b), setOf(a, b), true))
        // A different delivery must wait for resolution; it cannot cover the active alarm.
        assertEquals(AlarmHandoffAction.WAIT_FOR_READY,
            alarmHandoffAction(AlarmHandoff(3), listOf(a, b), setOf(a, b), true))
    }

    @Test
    fun duplicateWaitsUntilInitializationSucceeds() {
        val a = destination(1)
        assertEquals(AlarmHandoffAction.WAIT_FOR_READY,
            alarmHandoffAction(AlarmHandoff(1), listOf(a), emptySet(), true))
        assertEquals(AlarmHandoffAction.ACKNOWLEDGE,
            alarmHandoffAction(AlarmHandoff(1), listOf(a), setOf(a), true))
    }

    @Test
    fun previewDoesNotSuppressDeliveryAndRealOccurrencePreservesDeliveryOrder() {
        val oldOccurrence = destination(1, 1000)
        val preview = oldOccurrence.copy(fromSheet = true)
        assertEquals(AlarmHandoffAction.WAIT_FOR_READY,
            alarmHandoffAction(AlarmHandoff(1, 2000), listOf(oldOccurrence), setOf(oldOccurrence), true))
        assertEquals(AlarmHandoffAction.OPEN,
            alarmHandoffAction(AlarmHandoff(1, 1000), listOf(preview), setOf(preview), true))
    }

    @Test
    fun androidDuplicatesRetainImmediateConsumption() {
        val a = destination(1)
        assertEquals(AlarmHandoffAction.ACKNOWLEDGE,
            alarmHandoffAction(AlarmHandoff(1), listOf(a), emptySet(), false))
    }

    @Test
    fun restoredOccurrenceAcknowledgesItsVersionedQueuedDeliveryOnlyAfterReadiness() {
        val restored = destination(7, 1000)
        val original = AlarmHandoff(7, activeAt = 1000, version = 2, deliveryId = "original")
        assertEquals(AlarmHandoffAction.WAIT_FOR_READY,
            alarmHandoffAction(original, listOf(restored), emptySet(), true))
        assertEquals(AlarmHandoffAction.ACKNOWLEDGE,
            alarmHandoffAction(original, listOf(restored), setOf(restored), true))
        assertEquals(AlarmHandoffAction.WAIT_FOR_READY,
            alarmHandoffAction(original.copy(activeAt = 2000, deliveryId = "next"), listOf(restored), setOf(restored), true))
        assertEquals(AlarmHandoffAction.WAIT_FOR_READY,
            alarmHandoffAction(original.copy(activeAt = null, deliveryId = "unknown"), listOf(restored), setOf(restored), true))
    }

    @Test
    fun versionedNativeDeliveryTokensDistinguishRecurringFiringsAndKeepLegacyPayloads() {
        val first = AlarmHandoff(7, version = 2, deliveryId = "registration/first")
        val next = AlarmHandoff(7, version = 2, deliveryId = "registration/next")
        assertEquals(first, decodeAlarmHandoff(encodeAlarmHandoff(first)))
        assertEquals(next, decodeAlarmHandoff(encodeAlarmHandoff(next)))
        assertEquals(AlarmHandoff(7), decodeAlarmHandoff("{\"alarmId\":7}"))
        val active = AlarmMath("saved settings", handoffJson = encodeAlarmHandoff(first))
        assertEquals(AlarmHandoffAction.ACKNOWLEDGE,
            alarmHandoffAction(first, listOf(active), setOf(active), true))
        assertEquals(AlarmHandoffAction.WAIT_FOR_READY,
            alarmHandoffAction(next, listOf(active), setOf(active), true))
        assertEquals(AlarmHandoffAction.OPEN,
            alarmHandoffAction(next, emptyList(), emptySet(), true))
        assertNull(decodeAlarmHandoff("{\"alarmId\":7,\"version\":2}"))
        assertNull(decodeAlarmHandoff("{\"alarmId\":7,\"version\":3,\"deliveryId\":\"future\"}"))
    }

    private fun destination(id: Long, activeAt: Long? = null) =
        AlarmMath("unused saved settings", handoffJson = encodeAlarmHandoff(AlarmHandoff(id, activeAt)))
}
