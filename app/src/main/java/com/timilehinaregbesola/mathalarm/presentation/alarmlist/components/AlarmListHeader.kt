package com.timilehinaregbesola.mathalarm.presentation.alarmlist.components

import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.timilehinaregbesola.mathalarm.domain.model.AppThemeOptions
import com.timilehinaregbesola.mathalarm.presentation.ui.darkPrimaryLight
import com.timilehinaregbesola.mathalarm.presentation.ui.spacing

@Composable
fun ListHeader(enabled: Boolean, nearestAlarmMessage: String, theme: AppThemeOptions) {
    val isDark = when (theme) {
        AppThemeOptions.DARK -> true
        AppThemeOptions.LIGHT -> false
        else -> isSystemInDarkTheme()
    }
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                color = if (isDark) darkPrimaryLight else Color.LightGray.copy(alpha = 0.1f)
            ),
        elevation = 4.dp
    ) {
        Text(
            text = if (enabled) "Next alarm in $nearestAlarmMessage" else "No upcoming alarms",
            modifier = Modifier
                .padding(
                    start = MaterialTheme.spacing.extraMedium,
                    top = MaterialTheme.spacing.medium,
                    bottom = MaterialTheme.spacing.small
                ),
            fontSize = 16.sp
        )
    }
}
