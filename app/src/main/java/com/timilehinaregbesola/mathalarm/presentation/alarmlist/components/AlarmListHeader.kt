package com.timilehinaregbesola.mathalarm.presentation.alarmlist.components

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.timilehinaregbesola.mathalarm.presentation.ui.darkPrimaryLight

@Composable
fun ListHeader(enabled: Boolean, nearestAlarmMessage: String) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = if (isSystemInDarkTheme()) darkPrimaryLight else Color.LightGray.copy(alpha = 0.1f)
            ),
        elevation = 4.dp
    ) {
        Text(
            text = if (enabled) "Next alarm in $nearestAlarmMessage" else "No upcoming alarms",
            modifier = Modifier
                .padding(start = 24.dp, top = 16.dp, bottom = 8.dp),
            fontSize = 16.sp
        )
    }
}
