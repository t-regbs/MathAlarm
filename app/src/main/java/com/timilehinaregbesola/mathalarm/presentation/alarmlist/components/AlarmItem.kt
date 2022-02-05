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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.timilehinaregbesola.mathalarm.domain.model.Alarm
import com.timilehinaregbesola.mathalarm.presentation.ui.MathAlarmTheme
import com.timilehinaregbesola.mathalarm.presentation.ui.darkPrimaryLight
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
    scaffoldState: ScaffoldState,
    darkTheme: Boolean
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .padding(top = 8.dp, bottom = 8.dp, start = 24.dp, end = 24.dp),
        elevation = 4.dp,
        shape = MaterialTheme.shapes.medium.copy(CornerSize(8.dp))
    ) {
        val expandItem = rememberSaveable { mutableStateOf(false) }
        Column(
            Modifier
                .background(if (darkTheme) darkPrimaryLight else Color(0x99FFFFFF))
                .clickable(onClick = { expandItem.value = !expandItem.value })
        ) {
            Column(modifier = Modifier) {
                Row {
                    val time = alarm.getFormatTime().toString()
                    val actualTime = time.substring(0, time.length - 3)
                    val timeOfDay = time.substring(time.length - 2)
                    Row(
                        modifier = Modifier
                            .padding(start = 24.dp, top = 8.dp, bottom = 8.dp)
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
                            color = if (darkTheme) Color.LightGray else Color.Gray,
                            fontWeight = if (alarm.isOn) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier
                                .align(Alignment.Bottom)
                                .padding(bottom = 8.dp)
                        )
                    }
                    Switch(
                        modifier = Modifier
                            .weight(1f)
                            .padding(4.dp)
                            .align(Alignment.CenterVertically),
                        checked = alarm.isOn,
//                        colors = SwitchDefaults.colors(
//                            checkedThumbColor = enterButtonColor
//                        ),
                        onCheckedChange = {
                            alarm.isOn = it
                            if (alarm.isOn) {
//                                if (alarm.scheduleAlarm(context, false)) {
//                                    scope.launch {
//                                        when (
//                                            scaffoldState.snackbarHostState.showSnackbar(
//                                                message = alarm.getTimeLeftMessage(context)!!,
//                                                duration = SnackbarDuration.Short
//                                            )
//                                        ) {
//                                            SnackbarResult.Dismissed ->
//                                                Timber.d("Track: Dismissed")
//                                            SnackbarResult.ActionPerformed ->
//                                                Timber.d("Track: Action!")
//                                        }
//                                    }
//                                } else {
//                                    alarm.isOn = false
//                                    checkedState.value = false
//                                }
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
                    modifier = Modifier.padding(start = 24.dp),
                    text = alarm.title,
                    fontSize = 15.sp,
                )
                Row(
                    modifier = Modifier
                        .padding(bottom = 16.dp, top = 16.dp, start = 24.dp)
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
                    Spacer(modifier = Modifier.height(8.dp))
                    Divider(
                        thickness = 3.dp,
                        modifier = Modifier
                            .padding(start = 8.dp, end = 8.dp)
                    )
                    Spacer(modifier = Modifier.height(20.dp))
                    Row(modifier = Modifier.padding(start = 24.dp, bottom = 16.dp)) {
                        Row(modifier = Modifier.weight(3f)) {
                            Row(
                                Modifier
                                    .padding(end = 24.dp)
                                    .clickable(onClick = { onDeleteAlarm(alarm) })
                            ) {
                                Icon(
                                    imageVector = Icons.Outlined.Delete,
                                    contentDescription = "Delete",
                                    modifier = Modifier
                                        .padding(end = 4.dp)
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
                                        .padding(end = 4.dp)
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

@OptIn(ExperimentalMaterialApi::class, ExperimentalAnimationApi::class)
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
                onScheduleAlarm = { alarm: Alarm, b: Boolean -> },
                scaffoldState = rememberScaffoldState(),
                darkTheme = true
            )
        }
    }
}
