package com.timilehinaregbesola.mathalarm.presentation.alarmsettings.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.timilehinaregbesola.mathalarm.presentation.ui.spacing
import com.timilehinaregbesola.mathalarm.presentation.ui.teall
import com.timilehinaregbesola.mathalarm.presentation.ui.unSelectedDay
import com.timilehinaregbesola.mathalarm.utils.days

@Composable
fun AlarmDays(
    currentDays: String,
    onValueChange: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = MaterialTheme.spacing.medium),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        days.forEachIndexed { index, day ->
            val sb = StringBuilder(currentDays)
            val sel = currentDays[index] == 'T'
            val checkedState = mutableStateOf(sel)
            RingDayChip(
                day = day,
                selected = checkedState.value,
                onSelectChange = {
                    checkedState.value = it
                    if (it) {
                        sb.setCharAt(index, 'T')
                        onValueChange(sb.toString())
                    } else {
                        sb.setCharAt(index, 'F')
                        onValueChange(sb.toString())
                    }
                }
            )
        }
    }
}

@Composable
fun RingDayChip(
    day: String,
    selected: Boolean = false,
    onSelectChange: (Boolean) -> Unit
) {
    Surface(
        modifier = Modifier
            .size(36.dp)
            .toggleable(
                value = selected,
                onValueChange = onSelectChange
            ),
        elevation = 0.dp,
        shape = CircleShape,
        color = if (selected) teall else unSelectedDay
    ) {
        Box(
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = day,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = if (selected) Color.White else Color.Black,
            )
        }
    }
}
