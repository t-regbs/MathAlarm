package com.timilehinaregbesola.mathalarm.navigation

import com.timilehinaregbesola.mathalarm.utils.Destinations.AlarmMath

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
