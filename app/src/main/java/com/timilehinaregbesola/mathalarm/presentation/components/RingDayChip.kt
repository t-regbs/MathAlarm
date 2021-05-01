package com.timilehinaregbesola.mathalarm.presentation.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.Surface
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.timilehinaregbesola.composealarm.ui.teall
import com.timilehinaregbesola.composealarm.ui.unSelectedDay

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
        color = if (selected) {
            teall
        } else {
            unSelectedDay
        }
    ) {
        Box(
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = day,
                fontWeight = FontWeight.Bold,
                fontSize = 15.sp,
                color = if (selected) Color.White else Color.Black,
//                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
            )
        }
    }
}