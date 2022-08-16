package com.timilehinaregbesola.mathalarm.presentation.alarmlist.components

import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
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
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.LifecycleOwner
import androidx.navigation.NavHostController
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.timilehinaregbesola.mathalarm.R
import com.timilehinaregbesola.mathalarm.domain.model.Alarm
import com.timilehinaregbesola.mathalarm.framework.database.AlarmEntity
import com.timilehinaregbesola.mathalarm.framework.database.AlarmMapper
import com.timilehinaregbesola.mathalarm.presentation.alarmlist.AlarmListEvent
import com.timilehinaregbesola.mathalarm.presentation.alarmlist.AlarmListViewModel
import com.timilehinaregbesola.mathalarm.presentation.ui.spacing
import com.timilehinaregbesola.mathalarm.utils.Navigation
import com.timilehinaregbesola.mathalarm.utils.Navigation.NAV_SETTINGS_SHEET_ARGUMENT
import com.timilehinaregbesola.mathalarm.utils.SAT
import com.timilehinaregbesola.mathalarm.utils.UiEvent
import com.timilehinaregbesola.mathalarm.utils.getDayOfWeek
import timber.log.Timber
import java.net.URLEncoder
import java.util.*

@ExperimentalAnimationApi
@ExperimentalFoundationApi
@ExperimentalMaterialApi
@Composable
fun ListDisplayScreen(
    viewModel: AlarmListViewModel = hiltViewModel(),
    navController: NavHostController,
    darkTheme: Boolean
) {
    val alarms = viewModel.alarms.collectAsState(null)
    val alarmPermission = viewModel.permission
    var deleteAllAlarmsDialog by remember { mutableStateOf(false) }
    val scaffoldState = rememberScaffoldState()
    var showPermissionDialog by remember { mutableStateOf(false) }

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
                is UiEvent.Navigate -> {
                    buildArgAndNavigate(AlarmMapper().mapFromDomainModel(event.alarm)) { alarmJson ->
                        navController.navigate(Navigation.NAV_SETTINGS_SHEET.replace("{$NAV_SETTINGS_SHEET_ARGUMENT}", alarmJson))
                    }
                }
                else -> Unit
            }
        }
    }

    DisposableEffect(key1 = Unit) {
        val observer = object : LifecycleEventObserver {
            override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                when (event) {
                    Lifecycle.Event.ON_RESUME -> {
                        val cancelled = navController.currentBackStackEntry?.savedStateHandle?.remove<AlarmEntity>("testAlarm")

                        cancelled?.let {
                            buildArgAndNavigate(it) { alarmJson ->
                                navController.navigate(Navigation.NAV_SETTINGS_SHEET.replace("{$NAV_SETTINGS_SHEET_ARGUMENT}", alarmJson))
                            }
                        }
                    }

                    Lifecycle.Event.ON_DESTROY -> {
                        navController.currentBackStackEntry?.lifecycle?.removeObserver(this)
                    }

                    else -> Unit
                }
            }
        }

        navController.currentBackStackEntry?.lifecycle?.addObserver(observer)

        onDispose {
            navController.currentBackStackEntry?.lifecycle?.removeObserver(observer)
        }
    }

    if (alarms.value == null) {
        ListLoadingShimmer(imageHeight = 180.dp, isDark = darkTheme)
    }
    val context = LocalContext.current
    alarms.value?.let { alarmList ->
        Surface(
            modifier = Modifier
                .fillMaxSize()
        ) {
            Scaffold(
                scaffoldState = scaffoldState,
                topBar = {
                    ListTopAppBar(
                        openDialog = { deleteAllAlarmsDialog = it },
                        onSettingsClick = {
                            navController.navigate(Navigation.NAV_APP_SETTINGS)
                        }
                    )
                },
                snackbarHost = { state -> AlarmSnack(state) }
            ) { padding ->
                AlarmPermissionDialog(
                    context = context,
                    isDialogOpen = showPermissionDialog,
                    onCloseDialog = { showPermissionDialog = false }
                )
                ClearDialog(
                    openDialog = deleteAllAlarmsDialog,
                    onClear = { viewModel.onEvent(AlarmListEvent.OnClearAlarmsClick) },
                    onCloseDialog = { deleteAllAlarmsDialog = false }
                )
                if (alarmList.isEmpty()) {
                    AlarmEmptyScreen(
                        modifier = Modifier.padding(padding),
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
                                            checkPermissionAndPerformAction(
                                                value = alarmPermission.hasExactAlarmPermission(),
                                                action = { viewModel.onEvent(AlarmListEvent.OnEditAlarmClick(alarm)) },
                                                onPermissionAbsent = { showPermissionDialog = true }
                                            )
                                        },
                                        onUpdateAlarm = {
                                            checkPermissionAndPerformAction(
                                                value = alarmPermission.hasExactAlarmPermission(),
                                                action = { viewModel.onUpdate(it) },
                                                onPermissionAbsent = { showPermissionDialog = true }
                                            )
                                        },
                                        onDeleteAlarm = {
                                            viewModel.onEvent(AlarmListEvent.OnDeleteAlarmClick(it))
                                        },
                                        onCancelAlarm = {
                                            viewModel.cancelAlarm(it)
                                        },
                                        onScheduleAlarm = { curAlarm: Alarm, b: Boolean ->
                                            checkPermissionAndPerformAction(
                                                value = alarmPermission.hasExactAlarmPermission(),
                                                action = {
                                                    val calender = viewModel.calender.getCurrentCalendar()
                                                    viewModel.scheduleAlarm(
                                                        alarm = curAlarm,
                                                        reschedule = b,
                                                        message = "Alarm set for ${curAlarm.getTimeLeft(
                                                            getCal(curAlarm, calender).timeInMillis, calender
                                                        )}"
                                                    )
                                                },
                                                onPermissionAbsent = { showPermissionDialog = true }
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
                                checkPermissionAndPerformAction(
                                    value = alarmPermission.hasExactAlarmPermission(),
                                    action = { viewModel.onEvent(AlarmListEvent.OnAddAlarmClick) },
                                    onPermissionAbsent = { showPermissionDialog = true }
                                )
                            }
                        )
                    }
                }
            }
        }
    }
}

fun checkPermissionAndPerformAction(value: Boolean, action: () -> Unit, onPermissionAbsent: () -> Unit) {
    if (value) {
        action()
    } else {
        onPermissionAbsent()
    }
}

private fun buildArgAndNavigate(alarm: AlarmEntity, onNavigate: (String) -> Unit) {
    val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
    val jsonAdapter = moshi.adapter(AlarmEntity::class.java).lenient()
    val json = jsonAdapter.toJson(alarm)
    val alarmJson = URLEncoder.encode(json, "utf-8")
    onNavigate(alarmJson)
}

@Composable
private fun AlarmPermissionDialog(
    context: Context,
    isDialogOpen: Boolean,
    onCloseDialog: () -> Unit,
) {
    val arguments = DialogArguments(
        title = stringResource(id = R.string.task_alarm_permission_dialog_title),
        text = stringResource(id = R.string.task_alarm_permission_dialog_text),
        confirmText = stringResource(id = R.string.task_alarm_permission_dialog_confirm),
        dismissText = stringResource(id = R.string.task_alarm_permission_dialog_cancel),
        onConfirmAction = {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val intent = Intent().apply {
                    flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
                    action = Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM
                }
                context.startActivity(intent)
                onCloseDialog()
            }
        }
    )
    MathAlarmDialog(
        arguments = arguments,
        isDialogOpen = isDialogOpen,
        onDismissRequest = onCloseDialog
    )
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
