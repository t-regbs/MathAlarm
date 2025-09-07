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
import com.timilehinaregbesola.mathalarm.presentation.alarmlist.components.AlarmListHeader.LIST_HEADER_ALPHA
import com.timilehinaregbesola.mathalarm.presentation.alarmlist.components.AlarmListHeader.LIST_HEADER_ELEVATION
import com.timilehinaregbesola.mathalarm.presentation.alarmlist.components.AlarmListHeader.LIST_HEADER_FONT_SIZE
import com.timilehinaregbesola.mathalarm.presentation.alarmlist.components.AlarmListHeader.ONE_WEEK_IN_MILLISECONDS
import com.timilehinaregbesola.mathalarm.presentation.ui.darkPrimaryLight
import com.timilehinaregbesola.mathalarm.presentation.ui.spacing
import com.timilehinaregbesola.mathalarm.utils.getCalendarFromAlarm
import com.timilehinaregbesola.mathalarm.utils.getTimeLeft
import timber.log.Timber
import java.util.Calendar

@Composable
fun ListHeader(
    modifier: Modifier = Modifier,
    enabled: Boolean,
    calendar: Calendar,
    alarmList: List<Alarm>,
    isDark: Boolean
) {
    val (nearestTime, nearestIndex) = buildNearestTime(
        alarmList = alarmList,
        calendar = calendar
    )
    val nearestAlarmMessage by remember(nearestTime, nearestIndex) {
        derivedStateOf {
            nearestTime?.let { time ->
                alarmList[nearestIndex].getTimeLeft(
                    time,
                    calendar
                )
            }
        }
    }
    Surface(
        modifier = Modifier
            .padding(top = MaterialTheme.spacing.small)
            .fillMaxWidth()
            .background(
                color = if (isDark) darkPrimaryLight else LightGray.copy(alpha = LIST_HEADER_ALPHA),
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

private fun buildNearestTime(
    alarmList: List<Alarm>,
    calendar: Calendar
): Pair<Long?, Int> {
    var nearestTime: Long? = null
    var nearestIndex = -1
    if (alarmList.isNotEmpty()) {
        alarmList
            .filter { it.isOn }
            .forEachIndexed { index, alarm ->
                val cal = getCalendarFromAlarm(alarm, calendar)
                var alarmTime = cal.timeInMillis

                val now = System.currentTimeMillis()
                Timber.d("time = $alarmTime")
                if (alarmTime < now) {
                    alarmTime += ONE_WEEK_IN_MILLISECONDS
                }
                val timeToAlarm = alarmTime - now
                if (nearestTime == null || timeToAlarm < nearestTime!!) {
                    nearestTime = timeToAlarm
                    nearestIndex = index
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
            calendar = Calendar.getInstance(),
            alarmList = emptyList(),
            isDark = true
        )
    }
}

private object AlarmListHeader {
    const val LIST_HEADER_ALPHA = 0.1f
    const val ONE_WEEK_IN_MILLISECONDS = 7 * 24 * 60 * 60 * 1000
    val LIST_HEADER_ELEVATION = 4.dp
    val LIST_HEADER_FONT_SIZE = 16.sp
}
