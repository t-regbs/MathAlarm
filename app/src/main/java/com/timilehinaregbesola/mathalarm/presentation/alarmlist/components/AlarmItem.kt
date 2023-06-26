package com.timilehinaregbesola.mathalarm.presentation.alarmlist.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.Alignment.Companion.Bottom
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Color.Companion.Gray
import androidx.compose.ui.graphics.Color.Companion.LightGray
import androidx.compose.ui.text.font.FontWeight.Companion.Bold
import androidx.compose.ui.text.font.FontWeight.Companion.Normal
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.timilehinaregbesola.mathalarm.domain.model.Alarm
import com.timilehinaregbesola.mathalarm.presentation.alarmlist.components.AlarmItem.ActualTimeFontSize
import com.timilehinaregbesola.mathalarm.presentation.alarmlist.components.AlarmItem.AlarmInfoFontSize
import com.timilehinaregbesola.mathalarm.presentation.alarmlist.components.AlarmItem.AlarmItemCornerSize
import com.timilehinaregbesola.mathalarm.presentation.alarmlist.components.AlarmItem.AlarmItemElevation
import com.timilehinaregbesola.mathalarm.presentation.alarmlist.components.AlarmItem.AlarmItemLightBackgroundHex
import com.timilehinaregbesola.mathalarm.presentation.alarmlist.components.AlarmItem.AlarmTitleFontSize
import com.timilehinaregbesola.mathalarm.presentation.alarmlist.components.AlarmItem.DaysSetLimit
import com.timilehinaregbesola.mathalarm.presentation.alarmlist.components.AlarmItem.DividerThickness
import com.timilehinaregbesola.mathalarm.presentation.alarmlist.components.AlarmItem.EqualWeight
import com.timilehinaregbesola.mathalarm.presentation.alarmlist.components.AlarmItem.ExpandedSectionDividerSpacing
import com.timilehinaregbesola.mathalarm.presentation.alarmlist.components.AlarmItem.FiveOclock
import com.timilehinaregbesola.mathalarm.presentation.alarmlist.components.AlarmItem.Noon
import com.timilehinaregbesola.mathalarm.presentation.alarmlist.components.AlarmItem.ThreeQuartersWeight
import com.timilehinaregbesola.mathalarm.presentation.alarmlist.components.AlarmItem.TimeLengthIndex
import com.timilehinaregbesola.mathalarm.presentation.alarmlist.components.AlarmItem.TimeOfDayFontSize
import com.timilehinaregbesola.mathalarm.presentation.alarmlist.components.AlarmItem.TrueChar
import com.timilehinaregbesola.mathalarm.presentation.alarmlist.components.AlarmItem.Two
import com.timilehinaregbesola.mathalarm.presentation.alarmlist.components.AlarmItem.ZeroIndex
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
    darkTheme: Boolean,
) {
    with(MaterialTheme.spacing) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .fillMaxHeight()
                .padding(
                    top = small,
                    bottom = small,
                    start = extraMedium,
                    end = extraMedium,
                ),
            elevation = AlarmItemElevation,
            shape = MaterialTheme.shapes.medium.copy(CornerSize(AlarmItemCornerSize)),
        ) {
            val expandItem = rememberSaveable { mutableStateOf(false) }
            Column(
                Modifier
                    .background(if (darkTheme) darkPrimaryLight else Color(AlarmItemLightBackgroundHex))
                    .clickable(onClick = { expandItem.value = !expandItem.value }),
            ) {
                Column(modifier = Modifier) {
                    Row {
                        val time = alarm.getFormatTime().toString()
                        val actualTime = time.substring(ZeroIndex, TimeLengthIndex)
                        val timeOfDay = time.substring(actualTime.length)
                        Row(
                            modifier = Modifier
                                .padding(
                                    start = extraMedium,
                                    top = small,
                                    bottom = small,
                                )
                                .weight(ThreeQuartersWeight),
                        ) {
                            Text(
                                text = actualTime,
                                fontSize = ActualTimeFontSize,
                                fontWeight = if (alarm.isOn) Bold else Normal,
                            )
                            Text(
                                text = timeOfDay,
                                fontSize = TimeOfDayFontSize,
                                color = if (darkTheme) LightGray else Gray,
                                fontWeight = if (alarm.isOn) Bold else Normal,
                                modifier = Modifier
                                    .align(Bottom)
                                    .padding(bottom = small),
                            )
                        }
                        Switch(
                            modifier = Modifier
                                .weight(EqualWeight)
                                .padding(extraSmall)
                                .align(CenterVertically),
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
                            },
                        )
                    }
                    Text(
                        modifier = Modifier.padding(start = extraMedium),
                        text = alarm.title,
                        fontSize = AlarmTitleFontSize,
                    )
                    Row(
                        modifier = Modifier
                            .padding(
                                bottom = medium,
                                top = medium,
                                start = extraMedium,
                            )
                            .fillMaxWidth(),
                    ) {
                        val sb = StringBuilder()
                        var daysSet = 0
                        fullDays.indices.forEach { day ->
                            if (alarm.repeatDays[day] == TrueChar) {
                                daysSet++
                                if (daysSet < DaysSetLimit) {
                                    sb.append("${fullDays[day]}, ")
                                }
                            }
                        }
                        val alarmInfoText = if (daysSet < DaysSetLimit) {
                            sb.dropLast(Two).toString()
                        } else {
                            "Multiple Days"
                        }

                        val moreInfo = when {
                            alarm.hour < Noon -> {
                                "Good morning"
                            }
                            alarm.hour in Noon..FiveOclock -> {
                                "Afternoon"
                            }
                            else -> {
                                "Good Evening"
                            }
                        }

                        Text(
                            text = "$alarmInfoText | $moreInfo",
                            fontSize = AlarmInfoFontSize,
                            modifier = Modifier.weight(ThreeQuartersWeight),
                        )
                        if (!expandItem.value) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = "Expand",
                                modifier = Modifier
                                    .weight(EqualWeight)
                                    .align(CenterVertically)
                                    .clickable(
                                        onClick = { expandItem.value = true },
                                    ),
                            )
                        }
                    }
                }
                AnimatedVisibility(expandItem.value) {
                    AlarmItemExpandableSection(
                        onEditAlarm = onEditAlarm,
                        onDeleteAlarm = { onDeleteAlarm(alarm) },
                        onExpandClick = { expandItem.value = false },
                    )
                }
            }
        }
    }
}

@Composable
private fun AlarmItemExpandableSection(
    onEditAlarm: () -> Unit,
    onDeleteAlarm: () -> Unit,
    onExpandClick: () -> Unit,
) {
    with(MaterialTheme.spacing) {
        Column {
            Spacer(modifier = Modifier.height(small))
            Divider(
                thickness = DividerThickness,
                modifier = Modifier
                    .padding(
                        start = small,
                        end = small,
                    ),
            )
            Spacer(modifier = Modifier.height(ExpandedSectionDividerSpacing))
            Row(
                modifier = Modifier
                    .padding(
                        start = extraMedium,
                        bottom = medium,
                    ),
            ) {
                Row(modifier = Modifier.weight(ThreeQuartersWeight)) {
                    Row(
                        Modifier
                            .padding(end = extraMedium)
                            .clickable(onClick = onDeleteAlarm),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = "Delete",
                            modifier = Modifier
                                .padding(end = extraSmall)
                                .clickable(onClick = onDeleteAlarm),
                        )
                        Text(
                            text = "Delete",
                            modifier = Modifier
                                .align(CenterVertically),
                        )
                    }
                    Row(
                        Modifier.clickable(onClick = onEditAlarm),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = "Edit",
                            modifier = Modifier
                                .padding(end = extraSmall),
                        )
                        Text(
                            text = "Edit",
                            modifier = Modifier
                                .align(CenterVertically),
                        )
                    }
                }
                Icon(
                    imageVector = Icons.Default.KeyboardArrowUp,
                    contentDescription = "Collapse",
                    modifier = Modifier
                        .weight(EqualWeight)
                        .align(CenterVertically)
                        .clickable(
                            onClick = onExpandClick,
                        ),
                )
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
                darkTheme = true,
            )
        }
    }
}

private object AlarmItem {
    const val Two = 2
    const val DaysSetLimit = 4
    const val TrueChar = 'T'
    const val ZeroIndex = 0
    const val TimeLengthIndex = 5
    const val EqualWeight = 1f
    const val AlarmItemLightBackgroundHex = 0x99FFFFFF
    const val Noon = 12
    const val FiveOclock = 17
    const val ThreeQuartersWeight = 3f
    val AlarmItemElevation = 4.dp
    val AlarmTitleFontSize = 15.sp
    val ActualTimeFontSize = 40.sp
    val TimeOfDayFontSize = 16.sp
    val ExpandedSectionDividerSpacing = 20.dp
    val AlarmInfoFontSize = 14.sp
    val DividerThickness = 3.dp
    val AlarmItemCornerSize = 8.dp
}
