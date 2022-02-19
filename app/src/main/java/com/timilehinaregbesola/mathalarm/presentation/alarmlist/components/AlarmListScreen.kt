package com.timilehinaregbesola.mathalarm.presentation.alarmlist.components

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.timilehinaregbesola.mathalarm.R
import com.timilehinaregbesola.mathalarm.domain.model.Alarm
import com.timilehinaregbesola.mathalarm.presentation.alarmlist.AlarmListEvent
import com.timilehinaregbesola.mathalarm.presentation.alarmlist.AlarmListViewModel
import com.timilehinaregbesola.mathalarm.utils.Navigation
import com.timilehinaregbesola.mathalarm.utils.SAT
import com.timilehinaregbesola.mathalarm.utils.UiEvent
import com.timilehinaregbesola.mathalarm.utils.getDayOfWeek
import kotlinx.coroutines.flow.collect
import java.util.*

@ExperimentalAnimationApi
@ExperimentalFoundationApi
@ExperimentalMaterialApi
@Composable
fun ListDisplayScreen(
    viewModel: AlarmListViewModel = hiltViewModel(),
    onNavigate: (UiEvent.Navigate) -> Unit,
    navController: NavHostController,
    darkTheme: Boolean
) {
    val alarms = viewModel.alarms.collectAsState(null)
    val openDialog = remember { mutableStateOf(false) }
    val shouldOpenSheet = remember { mutableStateOf(true) }
    val scaffoldState = rememberScaffoldState()
    LaunchedEffect(key1 = true) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is UiEvent.ShowSnackbar -> {
                    val result = scaffoldState.snackbarHostState.showSnackbar(
                        message = event.message,
                        actionLabel = event.action
                    )
                    if (result == SnackbarResult.ActionPerformed) {
//                        viewModel.onEvent(AlarmListEvent.OnUndoDeleteClick)
                    }
                }
                is UiEvent.Navigate -> onNavigate(event)
                else -> Unit
            }
        }
    }

    val testScreenResult = navController.currentBackStackEntry?.savedStateHandle?.getLiveData<Long>("testAlarmId")?.observeAsState()
    val currEditAlarm = navController.currentBackStackEntry?.savedStateHandle?.getLiveData<Long>("currentEditAlarm")?.observeAsState()
    val previousRoute = navController.previousBackStackEntry?.destination?.route
    testScreenResult?.value?.let { testId ->
        navController
            .currentBackStackEntry?.savedStateHandle?.remove<Long>("currentEditAlarm")
        navController
            .currentBackStackEntry?.savedStateHandle?.remove<Long>("testAlarmId")
//        navController.currentBackStackEntry?.savedStateHandle?.set("openSheet", false)
        viewModel.onEvent(AlarmListEvent.DeleteTestAlarm(testId))
        if (previousRoute != Navigation.NAV_ALARM_LIST && shouldOpenSheet.value) {
            currEditAlarm?.value?.let {
                println("Prev: $previousRoute")
                shouldOpenSheet.value = false
                viewModel.onEvent(AlarmListEvent.OnEditAlarmClick(it))
            }
        }
    }
    if (alarms.value == null) {
        ListLoadingShimmer(imageHeight = 180.dp)
    }
    alarms.value?.let { alarmList ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
        ) {
            Scaffold(
                scaffoldState = scaffoldState,
                topBar = {
                    ListTopAppBar(
                        openDialog = openDialog,
                        onSettingsClick = {
                            navController.navigate(Navigation.NAV_APP_SETTINGS)
                        }
                    )
                },
                snackbarHost = { state -> AlarmSnack(state) }
            ) {
                if (openDialog.value) {
                    ClearDialog(
                        openDialog
                    ) { viewModel.onEvent(AlarmListEvent.OnClearAlarmsClick) }
                }
                if (alarmList.isEmpty()) {
                    AlarmEmptyScreen(
                        onClickFab = {
                            viewModel.onEvent(AlarmListEvent.OnAddAlarmClick)
                        },
                        darkTheme = darkTheme
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = 16.dp)
                            .background(
                                color = Color.LightGray.copy(alpha = 0.1f)
                            ),
                        contentAlignment = Alignment.TopStart
                    ) {
                        var enabled = false
                        val nearestAlarmMessage = rememberSaveable { mutableStateOf("") }
                        if (alarmList.isNotEmpty()) {
                            enabled = alarmList.any { it.isOn }
                            val now = System.currentTimeMillis()
                            var nearestTime = alarmList[0].let { it1 -> getCal(it1).timeInMillis }
                            var nearestIndex = 0
                            alarmList.forEachIndexed { index, alarm ->
                                val cal = getCal(alarm)
                                val time = cal.timeInMillis
                                if ((time - now) < nearestTime.minus(now)) {
                                    nearestTime = time
                                    nearestIndex = index
                                }
                            }
                            nearestAlarmMessage.value = nearestTime.let { it1 ->
                                alarmList[nearestIndex].getTimeLeft(it1)
                            }
                        }
                        Surface(
                            modifier = Modifier
                                .fillMaxSize()
                        ) {
                            LazyColumn(
                                horizontalAlignment = Alignment.CenterHorizontally,
                            ) {
                                stickyHeader {
                                    ListHeader(enabled, nearestAlarmMessage.value, darkTheme)
                                }
                                items(alarmList) { alarm ->
                                    AlarmItem(
                                        alarm = alarm,
                                        onEditAlarm = {
                                            viewModel.onEvent(AlarmListEvent.OnEditAlarmClick(alarm.alarmId))
                                        },
                                        onUpdateAlarm = viewModel::onUpdate,
                                        scaffoldState = scaffoldState,
                                        onDeleteAlarm = {
                                            viewModel.onEvent(AlarmListEvent.OnDeleteAlarmClick(it))
                                        },
                                        onCancelAlarm = {
                                            viewModel.cancelAlarm(it)
                                        },
                                        onScheduleAlarm = { curAlarm: Alarm, b: Boolean ->
                                            viewModel.scheduleAlarm(curAlarm, b)
                                        },
                                        darkTheme = darkTheme
                                    )
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
                                viewModel.onEvent(AlarmListEvent.OnAddAlarmClick)
                            }
                        )
                    }
                }
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
