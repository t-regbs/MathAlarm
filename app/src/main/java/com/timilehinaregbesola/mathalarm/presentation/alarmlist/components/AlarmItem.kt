package com.timilehinaregbesola.mathalarm.presentation.alarmlist.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.runtime.Composable
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.timilehinaregbesola.mathalarm.domain.model.Alarm
import com.timilehinaregbesola.mathalarm.domain.model.AppThemeOptions
import com.timilehinaregbesola.mathalarm.presentation.ui.MathAlarmTheme
import com.timilehinaregbesola.mathalarm.presentation.ui.darkPrimaryLight
import com.timilehinaregbesola.mathalarm.presentation.ui.spacing
import com.timilehinaregbesola.mathalarm.utils.*

@ExperimentalAnimationApi
@ExperimentalMaterialApi
@Composable
fun AlarmItem(
    alarm: Alarm,
    onEditAlarm: () -> Unit,
    onUpdateAlarm: (Alarm) -> Unit,
    onDeleteAlarm: (Alarm) -> Unit,
    onCancelAlarm: (Alarm) -> Unit,
    onScheduleAlarm: (Alarm, Boolean) -> Unit,
    darkTheme: AppThemeOptions
) {
    val isDarkTheme = when (darkTheme) {
        AppThemeOptions.DARK -> true
        AppThemeOptions.LIGHT -> false
        else -> isSystemInDarkTheme()
    }
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .padding(
                top = MaterialTheme.spacing.small,
                bottom = MaterialTheme.spacing.small,
                start = MaterialTheme.spacing.extraMedium,
                end = MaterialTheme.spacing.extraMedium
            ),
        elevation = 4.dp,
        shape = MaterialTheme.shapes.medium.copy(CornerSize(8.dp))
    ) {
        val expandItem = rememberSaveable { mutableStateOf(false) }
        Column(
            Modifier
                .background(if (isDarkTheme) darkPrimaryLight else Color(0x99FFFFFF))
                .clickable(onClick = { expandItem.value = !expandItem.value })
        ) {
            Column(modifier = Modifier) {
                Row {
                    val time = alarm.getFormatTime().toString()
                    val actualTime = time.substring(0, time.length - 3)
                    val timeOfDay = time.substring(time.length - 2)
                    Row(
                        modifier = Modifier
                            .padding(
                                start = MaterialTheme.spacing.extraMedium,
                                top = MaterialTheme.spacing.small,
                                bottom = MaterialTheme.spacing.small
                            )
                            .weight(3f),
                    ) {
                        Text(
                            text = actualTime,
                            fontSize = 40.sp,
                            fontWeight = if (alarm.isOn) FontWeight.Bold else FontWeight.Normal
                        )
                        Text(
                            text = timeOfDay,
                            fontSize = 16.sp,
                            color = if (isDarkTheme) Color.LightGray else Color.Gray,
                            fontWeight = if (alarm.isOn) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier
                                .align(Alignment.Bottom)
                                .padding(bottom = MaterialTheme.spacing.small)
                        )
                    }
                    Switch(
                        modifier = Modifier
                            .weight(1f)
                            .padding(MaterialTheme.spacing.extraSmall)
                            .align(Alignment.CenterVertically),
                        checked = alarm.isOn,
                        onCheckedChange = {
                            alarm.isOn = it
                            if (alarm.isOn) {
                                onUpdateAlarm(alarm)
                                onScheduleAlarm(alarm, false)
                            } else {
                                onUpdateAlarm(alarm)
                                onCancelAlarm(alarm)
                            }
                        }
                    )
                }
                Text(
                    modifier = Modifier.padding(start = MaterialTheme.spacing.extraMedium),
                    text = alarm.title,
                    fontSize = 15.sp,
                )
                Row(
                    modifier = Modifier
                        .padding(
                            bottom = MaterialTheme.spacing.medium,
                            top = MaterialTheme.spacing.medium,
                            start = MaterialTheme.spacing.extraMedium
                        )
                        .fillMaxWidth()
                ) {
                    val sb = StringBuilder()
                    var daysSet = 0
                    fullDays.indices.forEach { day ->
                        if (alarm.repeatDays[day] == 'T') {
                            daysSet++
                            if (daysSet < 4) {
                                sb.append("${fullDays[day]}, ")
                            }
                        }
                    }
                    val alarmInfoText = if (daysSet < 4) {
                        sb.dropLast(2).toString()
                    } else {
                        "Multiple Days"
                    }

                    val moreInfo = when {
                        alarm.hour < 12 -> {
                            "Good morning"
                        }
                        alarm.hour in 12..17 -> {
                            "Afternoon"
                        }
                        else -> {
                            "Good Evening"
                        }
                    }

                    Text(
                        text = "$alarmInfoText | $moreInfo",
                        fontSize = 14.sp,
                        modifier = Modifier.weight(3f)
                    )
                    if (!expandItem.value) {
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowDown,
                            contentDescription = "Expand",
                            modifier = Modifier
                                .weight(1f)
                                .align(Alignment.CenterVertically)
                                .clickable(onClick = { expandItem.value = true })
                        )
                    }
                }
            }
            AnimatedVisibility(expandItem.value) {
                Column {
                    Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))
                    Divider(
                        thickness = 3.dp,
                        modifier = Modifier
                            .padding(
                                start = MaterialTheme.spacing.small,
                                end = MaterialTheme.spacing.small
                            )
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Row(
                        modifier = Modifier
                            .padding(
                                start = MaterialTheme.spacing.extraMedium,
                                bottom = MaterialTheme.spacing.medium
                            )
                    ) {
                        Row(modifier = Modifier.weight(3f)) {
                            Row(
                                Modifier
                                    .padding(end = MaterialTheme.spacing.extraMedium)
                                    .clickable(onClick = { onDeleteAlarm(alarm) })
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Delete,
                                    contentDescription = "Delete",
                                    modifier = Modifier
                                        .padding(end = MaterialTheme.spacing.extraSmall)
                                        .clickable(onClick = { onDeleteAlarm(alarm) })
                                )
                                Text(
                                    text = "Delete",
                                    modifier = Modifier
                                        .align(Alignment.CenterVertically)
                                )
                            }
                            Row(
                                Modifier.clickable(onClick = onEditAlarm)
                            ) {
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Edit",
                                    modifier = Modifier
                                        .padding(end = MaterialTheme.spacing.extraSmall)
                                )
                                Text(
                                    text = "Edit",
                                    modifier = Modifier
                                        .align(Alignment.CenterVertically)
                                )
                            }
                        }
                        Icon(
                            imageVector = Icons.Default.KeyboardArrowUp,
                            contentDescription = "Collapse",
                            modifier = Modifier
                                .weight(1f)
                                .align(Alignment.CenterVertically)
                                .clickable(onClick = { expandItem.value = false })
                        )
                    }
                }
            }
        }
    }
}

@ExperimentalMaterialApi
@ExperimentalAnimationApi
@Preview
@Composable
fun ItemPreview() {
    MathAlarmTheme(darkTheme = true) {
        Column(modifier = Modifier.height(200.dp)) {
            AlarmItem(
                alarm = Alarm(title = "Testing testing"),
                onEditAlarm = {},
                onUpdateAlarm = {},
                onDeleteAlarm = {},
                onCancelAlarm = {},
                onScheduleAlarm = { _: Alarm, _: Boolean -> },
                darkTheme = AppThemeOptions.DARK
            )
        }
    }
}
