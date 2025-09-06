package com.timilehinaregbesola.mathalarm.presentation.alarmlist.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
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
import cafe.adriel.lyricist.strings
import com.timilehinaregbesola.mathalarm.domain.model.Alarm
import com.timilehinaregbesola.mathalarm.presentation.alarmlist.components.AlarmItem.ACTUAL_TIME_FONT_SIZE
import com.timilehinaregbesola.mathalarm.presentation.alarmlist.components.AlarmItem.ALARM_INFO_FONT_SIZE
import com.timilehinaregbesola.mathalarm.presentation.alarmlist.components.AlarmItem.ALARM_ITEM_CORNER_SIZE
import com.timilehinaregbesola.mathalarm.presentation.alarmlist.components.AlarmItem.ALARM_ITEM_ELEVATION
import com.timilehinaregbesola.mathalarm.presentation.alarmlist.components.AlarmItem.ALARM_ITEM_LIGHT_BACKGROUND_HEX
import com.timilehinaregbesola.mathalarm.presentation.alarmlist.components.AlarmItem.ALARM_TITLE_FONT_SIZE
import com.timilehinaregbesola.mathalarm.presentation.alarmlist.components.AlarmItem.DAYS_SET_LIMIT
import com.timilehinaregbesola.mathalarm.presentation.alarmlist.components.AlarmItem.DIVIDER_THICKNESS
import com.timilehinaregbesola.mathalarm.presentation.alarmlist.components.AlarmItem.EQUAL_WEIGHT
import com.timilehinaregbesola.mathalarm.presentation.alarmlist.components.AlarmItem.EXPANDED_SECTION_DIVIDER_SPACING
import com.timilehinaregbesola.mathalarm.presentation.alarmlist.components.AlarmItem.THREE_QUARTERS_WEIGHT
import com.timilehinaregbesola.mathalarm.presentation.alarmlist.components.AlarmItem.TIME_LENGTH_INDEX
import com.timilehinaregbesola.mathalarm.presentation.alarmlist.components.AlarmItem.TIME_OF_DAY_FONT_SIZE
import com.timilehinaregbesola.mathalarm.presentation.alarmlist.components.AlarmItem.TRUE_CHAR
import com.timilehinaregbesola.mathalarm.presentation.alarmlist.components.AlarmItem.TWO
import com.timilehinaregbesola.mathalarm.presentation.alarmlist.components.AlarmItem.ZERO_INDEX
import com.timilehinaregbesola.mathalarm.presentation.ui.MathAlarmTheme
import com.timilehinaregbesola.mathalarm.presentation.ui.darkPrimaryLight
import com.timilehinaregbesola.mathalarm.presentation.ui.spacing
import com.timilehinaregbesola.mathalarm.utils.fullDays
import com.timilehinaregbesola.mathalarm.utils.getFormatTime

@ExperimentalAnimationApi
@ExperimentalMaterial3Api
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
                ),
            elevation = CardDefaults.cardElevation(defaultElevation = ALARM_ITEM_ELEVATION),
            shape = MaterialTheme.shapes.medium.copy(CornerSize(ALARM_ITEM_CORNER_SIZE)),
        ) {
            var expandItem by rememberSaveable { mutableStateOf(false) }
            Column(
                Modifier
                    .background(
                        if (darkTheme) darkPrimaryLight else Color(
                            ALARM_ITEM_LIGHT_BACKGROUND_HEX
                        )
                    )
                    .clickable(onClick = { expandItem = !expandItem }),
            ) {
                Column(modifier = Modifier) {
                    Row {
                        val time = alarm.getFormatTime().toString()
                        val actualTime = time.substring(ZERO_INDEX, TIME_LENGTH_INDEX)
                        val timeOfDay = time.substring(actualTime.length)
                        Row(
                            modifier = Modifier
                                .padding(
                                    start = extraMedium,
                                    top = small,
                                    bottom = small,
                                )
                                .weight(THREE_QUARTERS_WEIGHT),
                        ) {
                            Text(
                                text = actualTime,
                                fontSize = ACTUAL_TIME_FONT_SIZE,
                                fontWeight = if (alarm.isOn) Bold else Normal,
                            )
                            Text(
                                text = timeOfDay,
                                fontSize = TIME_OF_DAY_FONT_SIZE,
                                color = if (darkTheme) LightGray else Gray,
                                fontWeight = if (alarm.isOn) Bold else Normal,
                                modifier = Modifier
                                    .align(Bottom)
                                    .padding(bottom = small),
                            )
                        }
                        Switch(
                            modifier = Modifier
                                .weight(EQUAL_WEIGHT)
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
                        fontSize = ALARM_TITLE_FONT_SIZE,
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
                            if (alarm.repeatDays[day] == TRUE_CHAR) {
                                daysSet++
                                if (daysSet < DAYS_SET_LIMIT) {
                                    sb.append("${fullDays[day]}, ")
                                }
                            }
                        }
                        val alarmInfoText = if (daysSet < DAYS_SET_LIMIT) {
                            sb.dropLast(TWO).toString()
                        } else {
                            strings.multipleDays
                        }

                        val moreInfo = strings.greeting(alarm.hour)

                        Text(
                            text = "$alarmInfoText | $moreInfo",
                            fontSize = ALARM_INFO_FONT_SIZE,
                            modifier = Modifier.weight(THREE_QUARTERS_WEIGHT),
                        )
                        if (!expandItem) {
                            Icon(
                                imageVector = Icons.Default.KeyboardArrowDown,
                                contentDescription = strings.expand,
                                modifier = Modifier
                                    .weight(EQUAL_WEIGHT)
                                    .align(CenterVertically)
                                    .clickable(
                                        onClick = { expandItem = true },
                                    ),
                            )
                        }
                    }
                }
                AnimatedVisibility(expandItem) {
                    AlarmItemExpandableSection(
                        onEditAlarm = onEditAlarm,
                        onDeleteAlarm = { onDeleteAlarm(alarm) },
                        onExpandClick = { expandItem = false },
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
            HorizontalDivider(
                thickness = DIVIDER_THICKNESS,
                modifier = Modifier
                    .padding(
                        start = small,
                        end = small,
                    ),
            )
            Spacer(modifier = Modifier.height(EXPANDED_SECTION_DIVIDER_SPACING))
            Row(
                modifier = Modifier
                    .padding(
                        start = extraMedium,
                        bottom = medium,
                    ),
            ) {
                Row(modifier = Modifier.weight(THREE_QUARTERS_WEIGHT)) {
                    Row(
                        Modifier
                            .padding(end = extraMedium)
                            .clickable(onClick = onDeleteAlarm),
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = strings.delete,
                            modifier = Modifier
                                .padding(end = extraSmall)
                                .clickable(onClick = onDeleteAlarm),
                        )
                        Text(
                            text = strings.delete,
                            modifier = Modifier
                                .align(CenterVertically),
                        )
                    }
                    Row(
                        Modifier.clickable(onClick = onEditAlarm),
                    ) {
                        Icon(
                            imageVector = Icons.Default.Edit,
                            contentDescription = strings.edit,
                            modifier = Modifier
                                .padding(end = extraSmall),
                        )
                        Text(
                            text = strings.edit,
                            modifier = Modifier
                                .align(CenterVertically),
                        )
                    }
                }
                Icon(
                    imageVector = Icons.Default.KeyboardArrowUp,
                    contentDescription = strings.collapse,
                    modifier = Modifier
                        .weight(EQUAL_WEIGHT)
                        .align(CenterVertically)
                        .clickable(
                            onClick = onExpandClick,
                        ),
                )
            }
        }
    }
}

@ExperimentalMaterial3Api
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
    const val TWO = 2
    const val DAYS_SET_LIMIT = 4
    const val TRUE_CHAR = 'T'
    const val ZERO_INDEX = 0
    const val TIME_LENGTH_INDEX = 5
    const val EQUAL_WEIGHT = 1f
    const val ALARM_ITEM_LIGHT_BACKGROUND_HEX = 0x99FFFFFF
    const val THREE_QUARTERS_WEIGHT = 3f
    val ALARM_ITEM_ELEVATION = 4.dp
    val ALARM_TITLE_FONT_SIZE = 15.sp
    val ACTUAL_TIME_FONT_SIZE = 40.sp
    val TIME_OF_DAY_FONT_SIZE = 16.sp
    val EXPANDED_SECTION_DIVIDER_SPACING = 20.dp
    val ALARM_INFO_FONT_SIZE = 14.sp
    val DIVIDER_THICKNESS = 3.dp
    val ALARM_ITEM_CORNER_SIZE = 8.dp
}
