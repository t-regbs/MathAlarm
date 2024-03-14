package com.timilehinaregbesola.mathalarm.presentation.alarmlist.components

import android.annotation.SuppressLint
import android.content.Context
import android.content.Intent
import android.os.Build
import android.provider.Settings
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.material3.SnackbarResult.ActionPerformed
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment.Companion.BottomEnd
import androidx.compose.ui.Alignment.Companion.Center
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Alignment.Companion.TopStart
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color.Companion.LightGray
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
import com.timilehinaregbesola.mathalarm.framework.app.permission.AlarmPermission
import com.timilehinaregbesola.mathalarm.framework.database.AlarmEntity
import com.timilehinaregbesola.mathalarm.framework.database.AlarmMapper
import com.timilehinaregbesola.mathalarm.presentation.alarmlist.AlarmListEvent.OnAddAlarmClick
import com.timilehinaregbesola.mathalarm.presentation.alarmlist.AlarmListEvent.OnClearAlarmsClick
import com.timilehinaregbesola.mathalarm.presentation.alarmlist.AlarmListEvent.OnDeleteAlarmClick
import com.timilehinaregbesola.mathalarm.presentation.alarmlist.AlarmListEvent.OnEditAlarmClick
import com.timilehinaregbesola.mathalarm.presentation.alarmlist.AlarmListEvent.OnUndoDeleteClick
import com.timilehinaregbesola.mathalarm.presentation.alarmlist.AlarmListViewModel
import com.timilehinaregbesola.mathalarm.presentation.alarmlist.components.AlarmListScreen.LIST_ALARM_BACKGROUND_ALPHA
import com.timilehinaregbesola.mathalarm.presentation.alarmlist.components.AlarmListScreen.LOADER_SIZE
import com.timilehinaregbesola.mathalarm.presentation.alarmlist.components.AlarmListScreen.LOADING_SHIMMER_IMAGE_HEIGHT
import com.timilehinaregbesola.mathalarm.presentation.alarmlist.components.AlarmListScreen.TEST_ALARM_KEY
import com.timilehinaregbesola.mathalarm.presentation.ui.spacing
import com.timilehinaregbesola.mathalarm.utils.Navigation.NAV_APP_SETTINGS
import com.timilehinaregbesola.mathalarm.utils.Navigation.NAV_SETTINGS_SHEET
import com.timilehinaregbesola.mathalarm.utils.Navigation.NAV_SETTINGS_SHEET_ARGUMENT
import com.timilehinaregbesola.mathalarm.utils.UiEvent.Navigate
import com.timilehinaregbesola.mathalarm.utils.UiEvent.ShowSnackbar
import com.timilehinaregbesola.mathalarm.utils.getCalendarFromAlarm
import com.timilehinaregbesola.mathalarm.utils.getTimeLeft
import timber.log.Timber
import java.net.URLEncoder
import java.util.*

@SuppressLint("UnrememberedMutableState")
@ExperimentalAnimationApi
@ExperimentalFoundationApi
@ExperimentalMaterial3Api
@Composable
fun ListDisplayScreen(
    viewModel: AlarmListViewModel = hiltViewModel(),
    navController: NavHostController,
    darkTheme: Boolean,
) {
    val alarms by viewModel.alarms.collectAsState(null)
    val alarmPermission = viewModel.permission
    var deleteAllAlarmsDialog by remember { mutableStateOf(false) }
    val snackbarHoststate = remember {
        SnackbarHostState()
    }
    var showPermissionDialog by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }

    LaunchedEffect(key1 = true) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is ShowSnackbar -> {
                    val result = snackbarHoststate.showSnackbar(
                        message = event.message,
                        actionLabel = event.action,
                    )
                    if (result == ActionPerformed) {
                        viewModel.onEvent(OnUndoDeleteClick)
                    }
                }
                is Navigate -> {
                    buildArgAndNavigate(AlarmMapper().mapFromDomainModel(event.alarm)) { alarmJson ->
                        navController.navigate(
                            NAV_SETTINGS_SHEET.replace(
                                "{$NAV_SETTINGS_SHEET_ARGUMENT}",
                                alarmJson,
                            ),
                        )
                    }
                    isLoading = false
                }
                else -> Unit
            }
        }
    }

    DisposableEffect(Unit) {
        val observer = object : LifecycleEventObserver {
            override fun onStateChanged(source: LifecycleOwner, event: Lifecycle.Event) {
                when (event) {
                    Lifecycle.Event.ON_RESUME -> {
                        val cancelled = navController
                            .currentBackStackEntry?.savedStateHandle?.remove<AlarmEntity>(TEST_ALARM_KEY)

                        cancelled?.let {
                            buildArgAndNavigate(it) { alarmJson ->
                                navController.navigate(
                                    NAV_SETTINGS_SHEET.replace(
                                        "{$NAV_SETTINGS_SHEET_ARGUMENT}",
                                        alarmJson,
                                    ),
                                )
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

    if (alarms == null) {
        ListLoadingShimmer(imageHeight = LOADING_SHIMMER_IMAGE_HEIGHT, isDark = darkTheme)
    }
    val context = LocalContext.current
    alarms?.let { alarmList ->
        Surface(
            modifier = Modifier
                .fillMaxSize(),
        ) {
            Scaffold(
                topBar = {
                    ListTopAppBar(
                        openDialog = { deleteAllAlarmsDialog = it },
                        onSettingsClick = {
                            navController.navigate(NAV_APP_SETTINGS)
                        },
                    )
                },
                snackbarHost = { AlarmSnack(snackbarHoststate) },
            ) { padding ->
                AlarmPermissionDialog(
                    context = context,
                    isDialogOpen = showPermissionDialog,
                    onCloseDialog = { showPermissionDialog = false },
                )
                ClearDialog(
                    openDialog = deleteAllAlarmsDialog,
                    onClear = { viewModel.onEvent(OnClearAlarmsClick) },
                    onCloseDialog = { deleteAllAlarmsDialog = false },
                )
                if (alarmList.isEmpty()) {
                    AlarmEmptyScreen(
                        modifier = Modifier.padding(padding),
                        onClickFab = {
                            viewModel.onEvent(OnAddAlarmClick)
                        },
                        darkTheme = darkTheme,
                    )
                } else {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(top = MaterialTheme.spacing.medium)
                            .background(
                                color = LightGray.copy(alpha = LIST_ALARM_BACKGROUND_ALPHA),
                            ),
                        contentAlignment = TopStart,
                    ) {
                        var enabled = false
                        var nearestIndex = -1
                        val nearest: Long = -1L
                        val triple = buildNearestTimeTriple(
                            alarmList = alarmList,
                            nearest = nearest,
                            viewModel = viewModel,
                            nearestIndex = nearestIndex,
                            enabled = enabled,
                        )
                        enabled = triple.second
                        nearestIndex = triple.third
                        val nearestTime = triple.first

                        val nearestAlarmMessage by derivedStateOf {
                            nearestTime?.let { it1 ->
                                alarmList[nearestIndex].getTimeLeft(it1, viewModel.calender.getCurrentCalendar())
                            }
                        }
                        AlarmListContent(
                            viewModel = viewModel,
                            alarmPermission = alarmPermission,
                            alarmList = alarmList,
                            enabled = enabled,
                            nearestAlarmMessage = nearestAlarmMessage,
                            darkTheme = darkTheme,
                            onEditAlarm = { isLoading = true },
                            onPermissionAbsent = { showPermissionDialog = true },
                        )
                        val fabImage = painterResource(id = R.drawable.fab_icon)
                        AddAlarmFab(
                            modifier = Modifier
                                .padding(
                                    bottom = MaterialTheme.spacing.medium,
                                    end = MaterialTheme.spacing.medium,
                                )
                                .align(BottomEnd),
                            fabImage = fabImage,
                            onClick = {
                                isLoading = true
                                checkPermissionAndPerformAction(
                                    value = alarmPermission.hasExactAlarmPermission(),
                                    action = { viewModel.onEvent(OnAddAlarmClick) },
                                    onPermissionAbsent = { showPermissionDialog = true },
                                )
                            },
                        )
                        if (isLoading) {
                            Loader(
                                modifier = Modifier
                                    .size(LOADER_SIZE)
                                    .align(Center),
                            )
                        }
                    }
                }
            }
        }
    }
}

@OptIn(
    ExperimentalFoundationApi::class,
    ExperimentalAnimationApi::class,
    ExperimentalMaterial3Api::class,
)
@Composable
private fun AlarmListContent(
    viewModel: AlarmListViewModel,
    alarmPermission: AlarmPermission,
    alarmList: List<Alarm>,
    enabled: Boolean,
    nearestAlarmMessage: String?,
    darkTheme: Boolean,
    onEditAlarm: () -> Unit,
    onPermissionAbsent: () -> Unit,
) {
    Surface(
        modifier = Modifier
            .fillMaxSize(),
    ) {
        LazyColumn(
            horizontalAlignment = CenterHorizontally,
        ) {
            stickyHeader {
                ListHeader(
                    enabled = enabled,
                    nearestAlarmMessage = nearestAlarmMessage ?: "",
                    isDark = darkTheme,
                )
            }
            items(
                items = alarmList,
                key = { alarm -> alarm.alarmId },
            ) { alarm ->
                AlarmItem(
                    alarm = alarm,
                    onEditAlarm = {
                        onEditAlarm()
                        checkPermissionAndPerformAction(
                            value = alarmPermission.hasExactAlarmPermission(),
                            action = { viewModel.onEvent(OnEditAlarmClick(alarm)) },
                            onPermissionAbsent = onPermissionAbsent,
                        )
                    },
                    onUpdateAlarm = {
                        checkPermissionAndPerformAction(
                            value = alarmPermission.hasExactAlarmPermission(),
                            action = { viewModel.onUpdate(it) },
                            onPermissionAbsent = onPermissionAbsent,
                        )
                    },
                    onDeleteAlarm = {
                        viewModel.onEvent(OnDeleteAlarmClick(it))
                    },
                    onCancelAlarm = viewModel::cancelAlarm,
                    onScheduleAlarm = { curAlarm: Alarm, b: Boolean ->
                        checkPermissionAndPerformAction(
                            value = alarmPermission.hasExactAlarmPermission(),
                            action = {
                                val calender = viewModel.calender.getCurrentCalendar()
                                viewModel.scheduleAlarm(
                                    alarm = curAlarm,
                                    reschedule = b,
                                    message = "Alarm set for ${curAlarm.getTimeLeft(
                                        getCalendarFromAlarm(curAlarm, calender).timeInMillis,
                                        calender,
                                    )}",
                                )
                            },
                            onPermissionAbsent = onPermissionAbsent,
                        )
                    },
                    darkTheme = darkTheme,
                )
            }
        }
    }
}

@Composable
private fun buildNearestTimeTriple(
    alarmList: List<Alarm>,
    nearest: Long?,
    viewModel: AlarmListViewModel,
    nearestIndex: Int,
    enabled: Boolean,
): Triple<Long?, Boolean, Int> {
    var nearest1 = nearest
    var nearestIndex1 = nearestIndex
    var enabled1 = enabled
    val l = if (alarmList.isNotEmpty()) {
        if (nearest1 == -1L) {
            nearest1 = alarmList.firstOrNull { it.isOn }?.let { it1 ->
                getCalendarFromAlarm(it1, viewModel.calender.getCurrentCalendar()).timeInMillis
            }
            nearestIndex1 = alarmList.indexOfFirst { it.isOn }
        }
        enabled1 = alarmList.any { it.isOn }
        val now = System.currentTimeMillis()
        val onAlarms = alarmList.filter { it.isOn }
        onAlarms.forEachIndexed { index, alarm ->
            val cal = getCalendarFromAlarm(alarm, viewModel.calender.getCurrentCalendar())
            val time = cal.timeInMillis
            Timber.d("time = $time")
            val currentEval = time - now
            val nearestEval = nearest1!! - now
            Timber.d("index = $index, currentEval = $currentEval, nearestEval = $nearestEval")
            if (currentEval < nearestEval) {
                nearest1 = time
                nearestIndex1 = index
            }
        }
        nearest1
    } else {
        0
    }
    return Triple(l, enabled1, nearestIndex1)
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
        },
    )
    MathAlarmDialog(
        arguments = arguments,
        isDialogOpen = isDialogOpen,
        onDismissRequest = onCloseDialog,
    )
}

private object AlarmListScreen {
    const val TEST_ALARM_KEY = "testAlarm"
    const val LIST_ALARM_BACKGROUND_ALPHA = 0.1f
    val LOADING_SHIMMER_IMAGE_HEIGHT = 180.dp
    val LOADER_SIZE = 50.dp
}
