package com.timilehinaregbesola.mathalarm.presentation.alarmlist

import androidx.compose.foundation.ExperimentalFoundationApi
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
import com.timilehinaregbesola.mathalarm.presentation.components.ClearDialog
import com.timilehinaregbesola.mathalarm.presentation.components.ListTopAppBar
import com.timilehinaregbesola.mathalarm.presentation.components.TimeLeftSnack
import com.timilehinaregbesola.mathalarm.utils.SAT
import com.timilehinaregbesola.mathalarm.utils.getDayOfWeek
import kotlinx.coroutines.launch
import java.util.*

@ExperimentalFoundationApi
@ExperimentalMaterialApi
@Composable
fun AlarmListScreen(
    navController: NavHostController,
    viewModel: AlarmListViewModel
) {
    Surface(
        Modifier
            .padding(top = 24.dp)
            .fillMaxSize()
    ) {
        viewModel.getAlarms()
        val alarms = viewModel.alarms.observeAsState()
        if (alarms.value?.isEmpty() == true) {
            EmptyScreen(viewModel)
        } else {
            ListDisplayScreen(viewModel)
        }
    }
}

@ExperimentalFoundationApi
@ExperimentalMaterialApi
@Composable
fun ListDisplayScreen(
    viewModel: AlarmListViewModel
) {
    val openDialog = remember { mutableStateOf(false) }
    val scaffoldState = rememberBottomSheetScaffoldState()
    val alarms = viewModel.alarms.observeAsState()
    val fromAdd = viewModel.addClicked.observeAsState()
    viewModel.navigateToAlarmSettings.observeAsState().value.let { id ->
        if (id != null) {
            viewModel.getAlarm(id)
        }
    }
    val alarm = viewModel.alarm.value

    val scope = rememberCoroutineScope()
    Surface(modifier = Modifier.fillMaxSize().padding(bottom = 24.dp)) {
        BottomSheetScaffold(
            sheetContent = {
                fromAdd.value?.let { AlarmBottomSheet(it, alarm, viewModel, scope, scaffoldState) }
            },
            sheetPeekHeight = 0.dp,
            sheetShape = RoundedCornerShape(topStart = 40.dp, topEnd = 40.dp),
            scaffoldState = scaffoldState,
            topBar = {
                ListTopAppBar(openDialog = openDialog)
            },
            snackbarHost = { state -> TimeLeftSnack(state) }
        ) {
            if (openDialog.value) ClearDialog(openDialog)
            Box(
                modifier = Modifier
                    .fillMaxSize()
//                    .padding(top = 16.dp)
                    .background(color = Color.LightGray.copy(alpha = 0.1f)),
                contentAlignment = Alignment.Center
            ) {
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
                        if (alarms.value != null) {
                            items(alarms.value!!) { alarm ->
                                AlarmItem(
                                    alarm = alarm,
                                    onClick = { viewModel.onAlarmClicked(alarm.alarmId) },
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

                val fabImage = painterResource(id = R.drawable.fabb)
                AddAlarmFab(
                    modifier = Modifier
                        .padding(bottom = 16.dp, end = 8.dp)
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
