package com.timilehinaregbesola.mathalarm.presentation.alarmsettings.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement.SpaceBetween
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment.Companion.Center
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color.Companion.Black
import androidx.compose.ui.graphics.Color.Companion.White
import androidx.compose.ui.text.font.FontWeight.Companion.Bold
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.timilehinaregbesola.mathalarm.presentation.alarmsettings.components.RingDayChip.ACTIVE_ALARM_DAY
import com.timilehinaregbesola.mathalarm.presentation.alarmsettings.components.RingDayChip.INACTIVE_ALARM_DAY
import com.timilehinaregbesola.mathalarm.presentation.alarmsettings.components.RingDayChip.NO_ELEVATION
import com.timilehinaregbesola.mathalarm.presentation.alarmsettings.components.RingDayChip.RING_DAY_CHIP_SIZE
import com.timilehinaregbesola.mathalarm.presentation.alarmsettings.components.RingDayChip.RING_DAY_FONT_SIZE
import com.timilehinaregbesola.mathalarm.presentation.ui.spacing
import com.timilehinaregbesola.mathalarm.presentation.ui.teall
import com.timilehinaregbesola.mathalarm.presentation.ui.unSelectedDay
import com.timilehinaregbesola.mathalarm.utils.days

@Composable
fun AlarmDays(
    currentDays: String,
    onValueChange: (String) -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = MaterialTheme.spacing.medium),
        horizontalArrangement = SpaceBetween,
    ) {
        days.forEachIndexed { index, day ->
            val sb = StringBuilder(currentDays)
            val sel = currentDays[index] == ACTIVE_ALARM_DAY
            val checkedState = mutableStateOf(sel)
            RingDayChip(
                day = day,
                selected = checkedState.value,
                onSelectChange = {
                    checkedState.value = it
                    if (it) {
                        sb.setCharAt(index, ACTIVE_ALARM_DAY)
                        onValueChange(sb.toString())
                    } else {
                        sb.setCharAt(index, INACTIVE_ALARM_DAY)
                        onValueChange(sb.toString())
                    }
                },
            )
        }
    }
}

@Composable
fun RingDayChip(
    day: String,
    selected: Boolean = false,
    onSelectChange: (Boolean) -> Unit,
) {
    Surface(
        modifier = Modifier
            .size(RING_DAY_CHIP_SIZE)
            .toggleable(
                value = selected,
                onValueChange = onSelectChange,
            ),
        elevation = NO_ELEVATION,
        shape = CircleShape,
        color = if (selected) teall else unSelectedDay,
    ) {
        Box(
            contentAlignment = Center,
        ) {
            Text(
                text = day,
                fontWeight = Bold,
                fontSize = RING_DAY_FONT_SIZE,
                color = if (selected) White else Black,
            )
        }
    }
}

@Preview
@Composable
private fun AlarmDaysPreview() {
    MaterialTheme {
        AlarmDays(currentDays = "MTWTFSS", onValueChange = {})
    }
}

private object RingDayChip {
    const val ACTIVE_ALARM_DAY = 'T'
    const val INACTIVE_ALARM_DAY = 'F'
    val RING_DAY_FONT_SIZE = 15.sp
    val RING_DAY_CHIP_SIZE = 36.dp
    val NO_ELEVATION = 0.dp
}
