package com.timilehinaregbesola.mathalarm.presentation.alarmlist.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color.Companion.LightGray
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.timilehinaregbesola.mathalarm.presentation.alarmlist.components.AlarmListHeader.LIST_HEADER_ALPHA
import com.timilehinaregbesola.mathalarm.presentation.alarmlist.components.AlarmListHeader.LIST_HEADER_ELEVATION
import com.timilehinaregbesola.mathalarm.presentation.alarmlist.components.AlarmListHeader.LIST_HEADER_FONT_SIZE
import com.timilehinaregbesola.mathalarm.presentation.ui.darkPrimaryLight
import com.timilehinaregbesola.mathalarm.presentation.ui.spacing

@Composable
fun ListHeader(enabled: Boolean, nearestAlarmMessage: String, isDark: Boolean) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = if (isDark) darkPrimaryLight else LightGray.copy(alpha = LIST_HEADER_ALPHA),
            ),
        tonalElevation = LIST_HEADER_ELEVATION,
    ) {
        with(MaterialTheme.spacing) {
            Text(
                text = if (enabled) "Next alarm in $nearestAlarmMessage" else "No upcoming alarms",
                modifier = Modifier
                    .padding(
                        start = extraMedium,
                        top = medium,
                        bottom = small,
                    ),
                fontSize = LIST_HEADER_FONT_SIZE,
            )
        }
    }
}

@Composable
@Preview
private fun ListHeaderPreview() {
    MaterialTheme {
        ListHeader(enabled = false, nearestAlarmMessage = "", isDark = true)
    }
}

private object AlarmListHeader {
    const val LIST_HEADER_ALPHA = 0.1f
    val LIST_HEADER_ELEVATION = 4.dp
    val LIST_HEADER_FONT_SIZE = 16.sp
}
