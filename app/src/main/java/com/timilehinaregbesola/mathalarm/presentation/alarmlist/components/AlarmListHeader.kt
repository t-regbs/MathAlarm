package com.timilehinaregbesola.mathalarm.presentation.alarmlist.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color.Companion.LightGray
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.timilehinaregbesola.mathalarm.presentation.alarmlist.components.AlarmListHeader.ListHeaderAlpha
import com.timilehinaregbesola.mathalarm.presentation.alarmlist.components.AlarmListHeader.ListHeaderElevation
import com.timilehinaregbesola.mathalarm.presentation.alarmlist.components.AlarmListHeader.ListHeaderFontSize
import com.timilehinaregbesola.mathalarm.presentation.ui.darkPrimaryLight
import com.timilehinaregbesola.mathalarm.presentation.ui.spacing

@Composable
fun ListHeader(enabled: Boolean, nearestAlarmMessage: String, isDark: Boolean) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = if (isDark) darkPrimaryLight else LightGray.copy(alpha = ListHeaderAlpha),
            ),
        elevation = ListHeaderElevation,
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
                fontSize = ListHeaderFontSize,
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
    const val ListHeaderAlpha = 0.1f
    val ListHeaderElevation = 4.dp
    val ListHeaderFontSize = 16.sp
}
