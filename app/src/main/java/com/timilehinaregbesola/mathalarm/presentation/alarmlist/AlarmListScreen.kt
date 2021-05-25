package com.timilehinaregbesola.mathalarm.presentation.alarmlist

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.timilehinaregbesola.mathalarm.R
import com.timilehinaregbesola.mathalarm.domain.model.Alarm
import com.timilehinaregbesola.mathalarm.presentation.components.AddAlarmFab
import com.timilehinaregbesola.mathalarm.presentation.components.AlarmSnack
import com.timilehinaregbesola.mathalarm.presentation.components.ClearDialog
import com.timilehinaregbesola.mathalarm.presentation.components.ListTopAppBar
import com.timilehinaregbesola.mathalarm.utils.SAT
import com.timilehinaregbesola.mathalarm.utils.getDayOfWeek
import kotlinx.coroutines.launch
import java.util.*

@ExperimentalAnimationApi
@ExperimentalFoundationApi
@ExperimentalMaterialApi
@Composable
fun ListDisplayScreen(
    viewModel: AlarmListViewModel,
    navController: NavHostController
) {
    viewModel.getAlarms()
    val openDialog = remember { mutableStateOf(false) }
    val scaffoldState = rememberBottomSheetScaffoldState()
    val alarms = viewModel.alarms.observeAsState()

    val scope = rememberCoroutineScope()
    val sheetState by viewModel.sheetState.observeAsState(SheetState.Init)
    Surface(
        modifier = Modifier
            .fillMaxSize()
//            .padding(bottom = 24.dp)
    ) {
        BottomSheetScaffold(
            sheetContent = {
                if (sheetState != SheetState.Init) {
                    AlarmBottomSheet(sheetState, viewModel, scope, scaffoldState, navController)
                }
            },
            sheetPeekHeight = 0.dp,
            sheetShape = RoundedCornerShape(topStart = 40.dp, topEnd = 40.dp),
            scaffoldState = scaffoldState,
            topBar = {
                ListTopAppBar(openDialog = openDialog)
            },
            snackbarHost = { state -> AlarmSnack(state) }
        ) {
            if (openDialog.value) ClearDialog(openDialog)
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(top = 16.dp)
                    .background(
                        color = if (alarms.value?.isEmpty() == true) {
                            Color.White
                        } else {
                            Color.LightGray.copy(alpha = 0.1f)
                        }
                    ),
                contentAlignment = if (alarms.value?.isEmpty() == true) {
                    Alignment.Center
                } else {
                    Alignment.TopStart
                }
            ) {
                val emptyImage = painterResource(id = R.drawable.search_icon)
                if (alarms.value?.isEmpty() == true) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .fillMaxHeight()
                            .align(Alignment.TopCenter)
                    ) {
                        Box(
                            modifier = Modifier
                                .padding(top = 143.dp)
                                .align(Alignment.CenterHorizontally),
                            contentAlignment = Alignment.TopEnd
                        ) {
                            Image(
                                painter = emptyImage,
                                contentDescription = "Empty Alarm List",
                                modifier = Modifier
                                    .width(167.dp)
                                    .height(228.dp)
                            )
                            Image(
                                painter = emptyImage,
                                contentDescription = "Empty Alarm List",
                                modifier = Modifier
                                    .padding(top = 24.dp, end = 40.dp)
                                    .width(167.dp)
                                    .height(228.dp)
                            )
                        }

                        Text(
                            modifier = Modifier
                                .padding(top = 29.dp)
                                .align(Alignment.CenterHorizontally),
                            text = "Nothing to see here",
                            fontSize = 16.sp
                        )
                    }
                } else {
                    var enabled = false
                    var nearestAlarmMessage = ""
                    if (alarms.value != null && alarms.value!!.isNotEmpty()) {
                        enabled = alarms.value!!.any { it.isOn }
                        val now = System.currentTimeMillis()
                        var nearest = alarms.value!![0].let { it1 -> getCal(it1).timeInMillis }
                        var nearestIndex = 0
                        alarms.value?.forEachIndexed { index, alarm ->
                            val cal = getCal(alarm)
                            val time = cal.timeInMillis
                            if ((time - now) < nearest.minus(now)) {
                                nearest = time
                                nearestIndex = index
                            }
                        }
                        nearestAlarmMessage = nearest.let { it1 ->
                            alarms.value!![nearestIndex].getTimeLeft(it1)
                        }
                    }
                    Surface(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(bottom = 16.dp),
                    ) {
                        LazyColumn(
                            horizontalAlignment = Alignment.CenterHorizontally,
                        ) {
                            stickyHeader {
                                ListHeader(enabled, nearestAlarmMessage)
                            }
                            if (alarms.value != null) {
                                items(alarms.value!!) { alarm ->
                                    AlarmItem(
                                        alarm = alarm,
                                        onEditAlarm = {
                                            viewModel.onEditAlarmClicked(alarm.alarmId)
                                            scope.launch { scaffoldState.bottomSheetState.expand() }
                                        },
                                        onUpdateAlarm = viewModel::onUpdate,
                                        scaffoldState = scaffoldState,
                                        onDeleteAlarm = {
                                            viewModel.onDelete(alarm)
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
                val fabImage = painterResource(id = R.drawable.fabb)
                AddAlarmFab(
                    modifier = Modifier
                        .padding(bottom = 16.dp, end = 16.dp)
                        .align(Alignment.BottomEnd),
                    fabImage = fabImage,
                    onClick = {
                        viewModel.onFabClicked()
                        scope.launch {
                            scaffoldState.bottomSheetState.expand()
                        }
                    }
                )
            }
        }
    }
}

@Composable
private fun ListHeader(enabled: Boolean, nearestAlarmMessage: String) {
    Surface(
        modifier = Modifier
            .fillMaxWidth()
            .background(color = Color.LightGray.copy(alpha = 0.1f)),
        elevation = 4.dp
    ) {
        Text(
            text = if (enabled) "Next alarm in $nearestAlarmMessage" else "No upcoming alarms",
            modifier = Modifier
                .padding(start = 24.dp, top = 16.dp, bottom = 8.dp),
            fontSize = 16.sp
        )
    }
}

private fun getCal(alarm: Alarm): Calendar {
    val cal = Calendar.getInstance()
    cal[Calendar.HOUR_OF_DAY] = alarm.hour
    cal[Calendar.MINUTE] = alarm.minute
    cal[Calendar.SECOND] = 0
    return cal
}

@Preview(showBackground = true)
@Composable
fun EmptyPreview() {
//    EmptyScreen()
}

fun Alarm.getTimeLeft(time: Long): String {
    val message: String
    val cal = getCal(alarm = this)
    val today = getDayOfWeek(cal[Calendar.DAY_OF_WEEK])
    var i: Int
    var lastAlarmDay: Int
    var nextAlarmDay: Int
    if (System.currentTimeMillis() > time) {
        nextAlarmDay = today + 1
        lastAlarmDay = today
        if (nextAlarmDay == 7) {
            nextAlarmDay = 0
        }
    } else {
        nextAlarmDay = today
        lastAlarmDay = today - 1
        if (lastAlarmDay == -1) {
            lastAlarmDay = 6
        }
    }
    i = nextAlarmDay
    while (i != lastAlarmDay) {
        if (i == 7) {
            i = 0
        }
        if (repeatDays[i] == 'T') {
            break
        }
        i++
    }
    if (i < today || i == today && cal.timeInMillis < System.currentTimeMillis()) {
        val daysUntilAlarm: Int = SAT - today + 1 + i
        cal.add(Calendar.DAY_OF_YEAR, daysUntilAlarm)
    } else {
        val daysUntilAlarm = i - today
        cal.add(Calendar.DAY_OF_YEAR, daysUntilAlarm)
    }
    val alarmTime = cal.timeInMillis
    val remainderTime = alarmTime - System.currentTimeMillis()
    val minutes = (remainderTime / (1000 * 60) % 60).toInt()
    val hours = (remainderTime / (1000 * 60 * 60) % 24).toInt()
    val days = (remainderTime / (1000 * 60 * 60 * 24)).toInt()
    val mString = if (minutes == 1) {
        "minute"
    } else {
        "minutes"
    }
    val hString = if (hours == 1) {
        "hour"
    } else {
        "hours"
    }
    val dString = if (days == 1) {
        "day"
    } else {
        "days"
    }
    message = if (days == 0) {
        if (hours == 0) {
            ("$minutes $mString")
        } else {
            ("$hours $hString $minutes $mString")
        }
    } else {
        (
            " " + days + " " + dString + " " + hours + " " + hString + " " + minutes + " " +
                mString + " "
            )
    }
    return message
}