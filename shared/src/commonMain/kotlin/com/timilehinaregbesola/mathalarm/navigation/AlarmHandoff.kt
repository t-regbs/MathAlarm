package com.timilehinaregbesola.mathalarm.navigation

import com.timilehinaregbesola.mathalarm.framework.database.AlarmEntity
import com.timilehinaregbesola.mathalarm.utils.Destinations.AlarmMath
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Navigation identifies an alarm; saved settings remain authoritative. */
@Serializable
data class AlarmHandoff(val alarmId: Long, val activeAt: Long? = null)

private val handoffCodec = Json { ignoreUnknownKeys = true }

fun encodeAlarmHandoff(handoff: AlarmHandoff): String = Json.encodeToString(handoff)

/** Android notification entrypoints provide the full alarm snapshot. */
internal fun decodeAlarmHandoff(payload: String): AlarmHandoff? =
    runCatching { handoffCodec.decodeFromString<AlarmHandoff>(payload) }
        .getOrNull()?.takeIf { it.alarmId > 0 }

internal fun decodeAlarmSnapshot(payload: String): AlarmEntity? =
    runCatching { Json.decodeFromString<AlarmEntity>(payload) }
        .getOrNull()?.takeIf { it.alarmId > 0 }

internal val AlarmMath.handoff: AlarmHandoff?
    get() = if (fromSheet) null else decodeAlarmHandoff(handoffJson ?: alarmJson)

internal enum class AlarmHandoffAction { OPEN, ACKNOWLEDGE, WAIT_FOR_READY }

internal fun alarmHandoffAction(
    incoming: AlarmHandoff?,
    destinations: List<AlarmMath>,
    readyDestinations: Set<AlarmMath>,
    acknowledgeWhenReady: Boolean,
): AlarmHandoffAction {
    if (incoming == null) return AlarmHandoffAction.ACKNOWLEDGE
    val existing = destinations.firstOrNull { it.handoff == incoming }
        ?: return AlarmHandoffAction.OPEN
    return if (!acknowledgeWhenReady || existing in readyDestinations) {
        AlarmHandoffAction.ACKNOWLEDGE
    } else {
        AlarmHandoffAction.WAIT_FOR_READY
    }
}
