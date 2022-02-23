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
import timber.log.Timber
import java.util.*

@ExperimentalAnimationApi
@ExperimentalFoundationApi
@ExperimentalMaterialApi
@Composable
fun ListDisplayScreen(
    viewModel: AlarmListViewModel = hiltViewModel(),
    onNavigate: (UiEvent.Navigate) -> Unit,
    navController: NavHostController,
    darkTheme: Boolean,
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
                        viewModel.onEvent(AlarmListEvent.OnUndoDeleteClick)
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
                        var nearestIndex = 0
                        var nearest: Long?
                        val nearestTime = if (alarmList.isNotEmpty()) {
                            nearest = alarmList.first { it.isOn }.let { it1 -> getCal(it1, viewModel.calender.getCurrentCalendar()).timeInMillis }
                            nearestIndex = alarmList.indexOfFirst { it.isOn }
                            enabled = alarmList.any { it.isOn }
                            val now = System.currentTimeMillis()
                            val onAlarms = alarmList.filter { it.isOn }
                            onAlarms.forEachIndexed { index, alarm ->
                                val cal = getCal(alarm, viewModel.calender.getCurrentCalendar())
                                val time = cal.timeInMillis
                                Timber.d("time = $time")
                                val currentEval = time - now
                                val nearestEval = nearest!! - now
                                Timber.d("index = $index, currentEval = $currentEval, nearestEval = $nearestEval")
                                if (currentEval < nearestEval) {
                                    nearest = time
                                    nearestIndex = index
                                }
                            }
                            nearest
                        } else 0

                        val nearestAlarmMessage = derivedStateOf {
                            nearestTime?.let { it1 ->
                                alarmList[nearestIndex].getTimeLeft(it1, viewModel.calender.getCurrentCalendar())
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
                                    ListHeader(enabled, nearestAlarmMessage?.value ?: "", darkTheme)
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

private fun getCal(alarm: Alarm, cal: Calendar): Calendar {
    cal[Calendar.DAY_OF_WEEK] = alarm.repeatDays.toList().indexOfFirst { it == 'T' } + 1
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

fun Alarm.getTimeLeft(time: Long, cal: Calendar): String {
    val message: String
    val cal = getCal(alarm = this, cal = cal)
    val today = getDayOfWeek(cal[Calendar.DAY_OF_WEEK])
    var i: Int
    var lastAlarmDay: Int
    var nextAlarmDay: Int
    if (System.currentTimeMillis() > time) {
        nextAlarmDay = if (today + 1 == 7) 0 else today + 1
        lastAlarmDay = today
    } else {
        nextAlarmDay = today
        lastAlarmDay = if (today - 1 == -1) 6 else today - 1
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
    val mString = if (minutes == 1) "minute" else "minutes"
    val hString = if (hours == 1) "hour" else "hours"
    val dString = if (days == 1) "day" else "days"
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
