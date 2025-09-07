package com.timilehinaregbesola.mathalarm.presentation.alarmlist.components

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color.Companion.LightGray
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.lyricist.strings
import com.timilehinaregbesola.mathalarm.domain.model.Alarm
import com.timilehinaregbesola.mathalarm.presentation.alarmlist.components.AlarmListHeader.LIST_HEADER_ELEVATION
import com.timilehinaregbesola.mathalarm.presentation.alarmlist.components.AlarmListHeader.LIST_HEADER_FONT_SIZE
import com.timilehinaregbesola.mathalarm.presentation.alarmlist.components.AlarmListHeader.ListHeaderAlpha
import com.timilehinaregbesola.mathalarm.presentation.ui.darkPrimaryLight
import com.timilehinaregbesola.mathalarm.presentation.ui.spacing
import com.timilehinaregbesola.mathalarm.utils.calculateNextAlarmTime
import com.timilehinaregbesola.mathalarm.utils.getTimeLeft
import kotlinx.datetime.TimeZone
import kotlin.time.ExperimentalTime
import kotlin.time.Instant

@OptIn(ExperimentalTime::class)
@Composable
fun ListHeader(
    modifier: Modifier = Modifier,
    enabled: Boolean,
    timeZone: TimeZone = TimeZone.currentSystemDefault(),
    alarmList: List<Alarm>,
    isDark: Boolean
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
        modifier = Modifier
            .padding(top = MaterialTheme.spacing.small)
            .fillMaxWidth()
            .background(
                color = if (isDark) darkPrimaryLight else LightGray.copy(alpha = ListHeaderAlpha),
            )
            .then(modifier),
        tonalElevation = LIST_HEADER_ELEVATION,
    ) {
        with(MaterialTheme.spacing) {
            Text(
                text = if (enabled && nearestAlarmMessage != null) {
                    "${strings.nextAlarmText} $nearestAlarmMessage"
                } else {
                    strings.noUpcomingAlarms
                },
                modifier = Modifier
                    .padding(
                        start = extraMedium,
                        top = extraLarge,
                        bottom = small,
                    ),
                fontSize = LIST_HEADER_FONT_SIZE,
            )
        }
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
            isDark = true
        )
    }
}

private object AlarmListHeader {
    const val ListHeaderAlpha = 0.1f
    const val ONE_WEEK_IN_MILLISECONDS = 7 * 24 * 60 * 60 * 1000
    val LIST_HEADER_ELEVATION = 4.dp
    val LIST_HEADER_FONT_SIZE = 16.sp
}
