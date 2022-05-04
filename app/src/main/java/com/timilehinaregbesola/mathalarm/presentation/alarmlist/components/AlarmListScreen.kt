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
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.navigation.DestinationsNavigator
import com.ramcosta.composedestinations.result.NavResult
import com.ramcosta.composedestinations.result.ResultRecipient
import com.timilehinaregbesola.mathalarm.R
import com.timilehinaregbesola.mathalarm.domain.model.Alarm
import com.timilehinaregbesola.mathalarm.framework.database.AlarmEntity
import com.timilehinaregbesola.mathalarm.framework.database.AlarmMapper
import com.timilehinaregbesola.mathalarm.presentation.alarmlist.AlarmListEvent
import com.timilehinaregbesola.mathalarm.presentation.alarmlist.AlarmListViewModel
import com.timilehinaregbesola.mathalarm.presentation.appsettings.AlarmPreferencesImpl
import com.timilehinaregbesola.mathalarm.presentation.appsettings.shouldUseDarkColors
import com.timilehinaregbesola.mathalarm.presentation.destinations.AlarmBottomSheetDestination
import com.timilehinaregbesola.mathalarm.presentation.destinations.AppSettingsScreenDestination
import com.timilehinaregbesola.mathalarm.presentation.destinations.MathScreenDestination
import com.timilehinaregbesola.mathalarm.presentation.ui.spacing
import com.timilehinaregbesola.mathalarm.utils.SAT
import com.timilehinaregbesola.mathalarm.utils.UiEvent
import com.timilehinaregbesola.mathalarm.utils.getDayOfWeek
import kotlinx.coroutines.InternalCoroutinesApi
import timber.log.Timber
import java.util.*

@InternalCoroutinesApi
@ExperimentalComposeUiApi
@ExperimentalAnimationApi
@ExperimentalFoundationApi
@ExperimentalMaterialApi
@Destination(start = true)
@Composable
fun ListDisplayScreen(
    viewModel: AlarmListViewModel = hiltViewModel(),
    navigator: DestinationsNavigator,
    resultRecipient: ResultRecipient<MathScreenDestination, AlarmEntity>,
    pref: AlarmPreferencesImpl,
) {
    val darkTheme = pref.shouldUseDarkColors()
    val alarms = viewModel.alarms.collectAsState(null)
    val openDialog = remember { mutableStateOf(false) }
    val scaffoldState = rememberScaffoldState()

    resultRecipient.onNavResult { result ->
        when (result) {
            is NavResult.Canceled -> {
                //
                Timber.d("Nav was cancelled")
            }
            is NavResult.Value -> {
                println("result reseived from GoToProfileConfirmationDestination = ${result.value}")
                navigator.navigate(AlarmBottomSheetDestination(alarm = result.value))
            }
        }
    }

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
                is UiEvent.Navigate -> navigator.navigate(AlarmBottomSheetDestination(alarm = AlarmMapper().mapFromDomainModel(event.alarm)))
                else -> Unit
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
                            navigator.navigate(AppSettingsScreenDestination)
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
                            .padding(top = MaterialTheme.spacing.medium)
                            .background(
                                color = Color.LightGray.copy(alpha = 0.1f)
                            ),
                        contentAlignment = Alignment.TopStart
                    ) {
                        var enabled = false
                        var nearestIndex = -1
                        var nearest: Long? = -1L
                        val nearestTime = if (alarmList.isNotEmpty()) {
                            if (nearest == -1L) {
                                nearest = alarmList.firstOrNull { it.isOn }?.let { it1 ->
                                    getCal(it1, viewModel.calender.getCurrentCalendar()).timeInMillis
                                }
                                nearestIndex = alarmList.indexOfFirst { it.isOn }
                            }
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
                                    ListHeader(enabled, nearestAlarmMessage.value ?: "", darkTheme)
                                }
                                items(alarmList) { alarm ->
                                    AlarmItem(
                                        alarm = alarm,
                                        onEditAlarm = {
                                            viewModel.onEvent(AlarmListEvent.OnEditAlarmClick(alarm))
                                        },
                                        onUpdateAlarm = viewModel::onUpdate,
                                        onDeleteAlarm = {
                                            viewModel.onEvent(AlarmListEvent.OnDeleteAlarmClick(it))
                                        },
                                        onCancelAlarm = {
                                            viewModel.cancelAlarm(it)
                                        },
                                        onScheduleAlarm = { curAlarm: Alarm, b: Boolean ->
                                            val calender = viewModel.calender.getCurrentCalendar()
                                            viewModel.scheduleAlarm(
                                                alarm = curAlarm,
                                                reschedule = b,
                                                message = "Alarm set for ${curAlarm.getTimeLeft(
                                                    getCal(curAlarm, calender).timeInMillis, calender
                                                )}"
                                            )
                                        },
                                        darkTheme = darkTheme
                                    )
                                }
                            }
                        }
                        val fabImage = painterResource(id = R.drawable.fabb)
                        AddAlarmFab(
                            modifier = Modifier
                                .padding(
                                    bottom = MaterialTheme.spacing.medium,
                                    end = MaterialTheme.spacing.medium
                                )
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

fun Alarm.getTimeLeft(time: Long, cal: Calendar): String {
    val message: String
    val calender = getCal(alarm = this, cal = cal)
    val today = getDayOfWeek(calender[Calendar.DAY_OF_WEEK])
    var i: Int
    val lastAlarmDay: Int
    val nextAlarmDay: Int
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
    if (i < today || i == today && calender.timeInMillis < System.currentTimeMillis()) {
        val daysUntilAlarm: Int = SAT - today + 1 + i
        calender.add(Calendar.DAY_OF_YEAR, daysUntilAlarm)
    } else {
        val daysUntilAlarm = i - today
        calender.add(Calendar.DAY_OF_YEAR, daysUntilAlarm)
    }
    val alarmTime = calender.timeInMillis
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
