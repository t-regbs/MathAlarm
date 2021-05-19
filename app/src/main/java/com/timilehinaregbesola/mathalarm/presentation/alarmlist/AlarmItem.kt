package com.timilehinaregbesola.mathalarm.presentation.alarmlist

import android.util.Log
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
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.timilehinaregbesola.mathalarm.domain.model.Alarm
import com.timilehinaregbesola.mathalarm.utils.*
import kotlinx.coroutines.launch

@ExperimentalAnimationApi
@ExperimentalMaterialApi
@Composable
fun AlarmItem(
    alarm: Alarm,
    onEditAlarm: () -> Unit,
    onUpdateAlarm: (Alarm) -> Unit,
    onDeleteAlarm: (Alarm) -> Unit,
    scaffoldState: BottomSheetScaffoldState
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
        val expandItem = remember { mutableStateOf(false) }
        Column(
            Modifier
                .background(Color(0x99FFFFFF))
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
                            color = Color.Black,
                            fontWeight = if (alarm.isOn) FontWeight.Bold else FontWeight.Normal
                        )
                        Text(
                            text = timeOfDay,
                            fontSize = 16.sp,
                            color = Color.Gray,
                            fontWeight = if (alarm.isOn) FontWeight.Bold else FontWeight.Normal,
                            modifier = Modifier
                                .align(Alignment.Bottom)
                                .padding(bottom = 8.dp)
                        )
                    }
                    val checkedState = remember { mutableStateOf(alarm.isOn) }
                    Switch(
                        modifier = Modifier
                            .weight(1f)
                            .padding(4.dp)
                            .align(Alignment.CenterVertically),
                        checked = checkedState.value,
                        onCheckedChange = {
                            checkedState.value = it
                            alarm.isOn = it
                            if (alarm.isOn) {
                                if (alarm.scheduleAlarm(context, false)) {
                                    scope.launch {
                                        when (
                                            scaffoldState.snackbarHostState.showSnackbar(
                                                message = alarm.getTimeLeftMessage(context)!!,
                                                duration = SnackbarDuration.Short
                                            )
                                        ) {
                                            SnackbarResult.Dismissed ->
                                                Log.d("Track", "Dismissed")
                                            SnackbarResult.ActionPerformed ->
                                                Log.d("Track", "Action!")
                                        }
                                    }
                                } else {
                                    alarm.isOn = false
                                    checkedState.value = false
                                }
                            } else {
                                alarm.cancelAlarm(context)
                            }
                            onUpdateAlarm(alarm)
                        }
                    )
                }
                Row(
                    modifier = Modifier
                        .padding(bottom = 16.dp, top = 24.dp, start = 24.dp)
                        .fillMaxWidth()
                ) {
                    val sb = StringBuilder()
                    var daysSet = 0
                    for (day in fullDays.indices) {
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
                        color = Color.Black,
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
