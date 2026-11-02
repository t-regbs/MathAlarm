package com.timilehinaregbesola.mathalarm.utils

import com.timilehinaregbesola.mathalarm.utils.strings.Strings
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.time.Duration.Companion.hours
import kotlin.time.Instant

/** Use elapsed time for the threshold, including across midnight and daylight-saving changes. */
fun nextAlarmMessage(next: Instant?, now: Instant, zone: TimeZone, strings: Strings): String {
    if (next == null || next <= now) return strings.noUpcomingAlarms
    val remaining = next - now
    if (remaining <= 24.hours) {
        val minutes = remaining.inWholeMinutes
        return strings.nextAlarmIn((minutes / 60).toInt(), (minutes % 60).toInt())
    }
    val date = next.toLocalDateTime(zone)
    return "${strings.nextAlarmText} ${formatShortDate(date.date.toString(), strings.dateLocale)} · ${date.time.toString().take(5)}"
}
