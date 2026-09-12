package com.timilehinaregbesola.mathalarm.presentation.alarmlist.components

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.lyricist.strings
import com.timilehinaregbesola.mathalarm.domain.model.Alarm
import com.timilehinaregbesola.mathalarm.presentation.alarmlist.components.AlarmListHeader.LIST_HEADER_FONT_SIZE
import com.timilehinaregbesola.mathalarm.utils.calculateNextAlarmTime
import com.timilehinaregbesola.mathalarm.utils.getTimeLeft
import kotlinx.datetime.TimeZone
import androidx.compose.ui.tooling.preview.Preview
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
@Composable
fun ListHeader(
    modifier: Modifier = Modifier,
    enabled: Boolean,
    timeZone: TimeZone = TimeZone.currentSystemDefault(),
    alarmList: List<Alarm>,
) {
    val (nearestTime, nearestIndex) = buildNearestTime(
        alarmList = alarmList,
        timeZone = timeZone
    )
    val nearestAlarmMessage by remember(nearestTime, nearestIndex) {
        derivedStateOf {
            nearestTime?.let {
                alarmList.getOrNull(nearestIndex)?.getTimeLeft()
            }
        }
    }
    Surface(
        modifier = modifier.fillMaxWidth().padding(horizontal = 24.dp, vertical = 12.dp),
        shape = androidx.compose.foundation.shape.RoundedCornerShape(16.dp),
        color = MaterialTheme.colorScheme.surfaceContainerLow,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    ) {
        Text(
            text = if (enabled && nearestAlarmMessage != null) {
                "${strings.nextAlarmText} $nearestAlarmMessage"
            } else {
                strings.noUpcomingAlarms
            },
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
            fontSize = LIST_HEADER_FONT_SIZE,
        )
    }
}

@OptIn(ExperimentalTime::class)
private fun buildNearestTime(
    alarmList: List<Alarm>,
    timeZone: TimeZone
): Pair<Instant?, Int> {
    var nearestTime: Instant? = null
    var nearestIndex = -1

    if (alarmList.isNotEmpty()) {
        alarmList
            .forEachIndexed { originalIndex, alarm ->
                if (alarm.isOn) {
                    val alarmInstant = calculateNextAlarmTime(alarm, timeZone)

                    // If a valid future time was found and it's sooner than the current nearest, update
                    if (alarmInstant != null && (nearestTime == null || alarmInstant < nearestTime)) {
                        nearestTime = alarmInstant
                        nearestIndex = originalIndex
                    }
                }
            }
    }
    return Pair(nearestTime, nearestIndex)
}

@Composable
@Preview
private fun ListHeaderPreview() {
    MaterialTheme {
        ListHeader(
            enabled = false,
            alarmList = emptyList(),
        )
    }
}

private object AlarmListHeader {
    val LIST_HEADER_FONT_SIZE = 16.sp
}
