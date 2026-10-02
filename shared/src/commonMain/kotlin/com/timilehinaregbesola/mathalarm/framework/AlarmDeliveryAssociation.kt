package com.timilehinaregbesola.mathalarm.framework

/** One unresolved challenge per repeating alarm; delivery tokens stay independently native. */
internal suspend fun Usecases.unresolvedOccurrenceForDelivery(alarmId: Long, deliveredAt: Long?): Long? = command {
    val current = findAlarm(alarmId) ?: return@command null
    val activeAt = current.activeAt ?: return@command null
    if (!current.isOn || !current.repeat || (deliveredAt != null && deliveredAt < activeAt)) return@command null
    // A known later alert is folded into this challenge exactly once. Retiring its
    // schedule membership before native acknowledgement prevents legacy replay
    // from creating another challenge after this unresolved occurrence completes.
    if (deliveredAt != null && deliveredAt > activeAt && deliveredAt in current.pendingTimes) {
        updateAlarm(current.copy(pendingTimes = current.pendingTimes.filterNot { it == deliveredAt }))
    }
    activeAt
}

internal suspend fun Usecases.isUnresolvedOccurrence(alarmId: Long, activeAt: Long): Boolean = command {
    findAlarm(alarmId)?.let { it.isOn && it.activeAt == activeAt } == true
}
