package com.timilehinaregbesola.mathalarm.presentation.alarmlist.components

import androidx.compose.ui.draw.clip
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import com.timilehinaregbesola.mathalarm.presentation.ui.spacing
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import cafe.adriel.lyricist.strings
import com.timilehinaregbesola.mathalarm.domain.model.Alarm
import com.timilehinaregbesola.mathalarm.presentation.alarmlist.components.AlarmListHeader.LIST_HEADER_FONT_SIZE
import com.timilehinaregbesola.mathalarm.utils.calculateNextAlarmTime
import kotlinx.datetime.TimeZone
import com.timilehinaregbesola.mathalarm.utils.nextAlarmMessage

@Composable
fun ListHeader(
    modifier: Modifier = Modifier,
    enabled: Boolean,
    hazeState: HazeState? = null,
    timeZone: TimeZone = TimeZone.currentSystemDefault(),
    alarmList: List<Alarm>,
) {
    val now by rememberAlarmNow()
    val clock = object : kotlin.time.Clock { override fun now() = now }
    val nearestTime = alarmList.filter { it.isOn }
        .mapNotNull { calculateNextAlarmTime(it, timeZone, clock) }.minOrNull()
    val nearestAlarmMessage = nextAlarmMessage(
        next = nearestTime.takeIf { enabled }, now = now, zone = timeZone, strings = strings,
    )
    val shape = RoundedCornerShape(AlarmListHeader.CORNER_RADIUS)
    val colors = MaterialTheme.colorScheme
    Surface(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = MaterialTheme.spacing.extraMedium, vertical = AlarmListHeader.VERTICAL_PADDING)
            .clip(shape)
            .then(if (hazeState != null) Modifier.hazeEffect(
                state = hazeState,
                style = HazeStyle(
                    backgroundColor = colors.surface,
                    tint = HazeTint(colors.surfaceContainerLow.copy(alpha = AlarmListHeader.TINT_ALPHA)),
                    blurRadius = AlarmListHeader.BLUR_RADIUS,
                    noiseFactor = AlarmListHeader.NOISE_FACTOR,
                    fallbackTint = HazeTint(colors.surfaceContainerLow.copy(alpha = AlarmListHeader.FALLBACK_TINT_ALPHA)),
                ),
            ) else Modifier),
        shape = shape,
        color = if (hazeState != null) Color.Transparent else colors.surfaceContainerLow,
        contentColor = MaterialTheme.colorScheme.onSurfaceVariant,
    ) {
        Text(
            modifier = Modifier.padding(horizontal = MaterialTheme.spacing.medium, vertical = AlarmListHeader.VERTICAL_PADDING),
            text = nearestAlarmMessage,
            fontSize = LIST_HEADER_FONT_SIZE,
        )
    }
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
    val BLUR_RADIUS = 24.dp
    const val TINT_ALPHA = 0.65f
    const val NOISE_FACTOR = 0.04f
    const val FALLBACK_TINT_ALPHA = 0.95f
    val CORNER_RADIUS = 16.dp
    val VERTICAL_PADDING = 12.dp
    val LIST_HEADER_FONT_SIZE = 16.sp
}
