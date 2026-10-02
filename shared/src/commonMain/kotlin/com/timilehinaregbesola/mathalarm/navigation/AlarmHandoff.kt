package com.timilehinaregbesola.mathalarm.navigation

import com.timilehinaregbesola.mathalarm.framework.database.AlarmEntity
import com.timilehinaregbesola.mathalarm.utils.Destinations.AlarmMath
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * Native delivery identity is separate from authoritative persisted occurrence time.
 * Version 1 alarmId-only/activeAt payloads remain readable in the durable queue.
 */
@Serializable
data class AlarmHandoff(
    val alarmId: Long,
    val activeAt: Long? = null,
    val version: Int = 1,
    val deliveryId: String? = null,
)

private val handoffCodec = Json { ignoreUnknownKeys = true }

fun encodeAlarmHandoff(handoff: AlarmHandoff): String = Json.encodeToString(handoff)

/** Android notification entrypoints provide the full alarm snapshot. */
internal fun decodeAlarmHandoff(payload: String): AlarmHandoff? =
    runCatching { handoffCodec.decodeFromString<AlarmHandoff>(payload) }
        .getOrNull()?.takeIf { it.alarmId > 0 && it.version in 1..2 &&
            (it.version == 1 || !it.deliveryId.isNullOrBlank()) }

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
    val existing = destinations.firstOrNull { destination ->
        val presented = destination.handoff
        // Restoration can use v1 while the native queue holds a v2 delivery for
        // that same durable occurrence. Readiness belongs to the occurrence.
        presented == incoming || (incoming.activeAt != null && presented != null && presented.activeAt == incoming.activeAt &&
            presented.alarmId == incoming.alarmId)
    }
    if (existing == null) {
        // A later delivery waits for resolution of the currently presented real challenge.
        // Preview destinations do not prevent an authoritative delivery from opening.
        return if (acknowledgeWhenReady && destinations.any { !it.fromSheet }) {
            AlarmHandoffAction.WAIT_FOR_READY
        } else AlarmHandoffAction.OPEN
    }
    return if (!acknowledgeWhenReady || existing in readyDestinations) {
        AlarmHandoffAction.ACKNOWLEDGE
    } else {
        AlarmHandoffAction.WAIT_FOR_READY
    }
}
