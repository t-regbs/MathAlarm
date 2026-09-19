package com.timilehinaregbesola.mathalarm.presentation.alarmlist.components

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.hazeSource
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.SnackbarResult.ActionPerformed
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment.Companion.BottomEnd
import androidx.compose.ui.Alignment.Companion.Center
import androidx.compose.ui.Alignment.Companion.CenterHorizontally
import androidx.compose.ui.Alignment.Companion.TopCenter
import androidx.compose.ui.Alignment.Companion.TopStart
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import cafe.adriel.lyricist.strings
import com.timilehinaregbesola.mathalarm.domain.model.Alarm
import kotlinx.datetime.TimeZone
import com.timilehinaregbesola.mathalarm.utils.formatShortDate
import com.timilehinaregbesola.mathalarm.provider.skippedTime
import com.timilehinaregbesola.mathalarm.framework.database.AlarmEntity
import com.timilehinaregbesola.mathalarm.framework.database.AlarmMapper
import com.timilehinaregbesola.mathalarm.platform.requestExactAlarmPermission
import com.timilehinaregbesola.mathalarm.presentation.alarmlist.AlarmListEvent.OnAddAlarmClick
import com.timilehinaregbesola.mathalarm.presentation.alarmlist.AlarmListEvent.OnClearAlarmsClick
import com.timilehinaregbesola.mathalarm.presentation.alarmlist.AlarmListEvent.OnClearEmptyAlarmsClick
import com.timilehinaregbesola.mathalarm.presentation.alarmlist.AlarmListEvent.OnDeleteAlarmClick
import com.timilehinaregbesola.mathalarm.presentation.alarmlist.AlarmListEvent.OnEditAlarmClick
import com.timilehinaregbesola.mathalarm.presentation.alarmlist.AlarmListEvent.OnSkipNextClick
import com.timilehinaregbesola.mathalarm.presentation.alarmlist.AlarmListEvent.OnUndoDeleteClick
import com.timilehinaregbesola.mathalarm.presentation.alarmlist.AlarmListEvent.OnUndoSkipClick
import com.timilehinaregbesola.mathalarm.presentation.alarmlist.AlarmListViewModel
import com.timilehinaregbesola.mathalarm.presentation.alarmlist.components.AlarmListScreen.LIST_CONTENT_MAX_WIDTH
import com.timilehinaregbesola.mathalarm.presentation.alarmlist.components.AlarmListScreen.LOADER_SIZE
import com.timilehinaregbesola.mathalarm.presentation.alarmlist.components.AlarmListScreen.LOADING_SHIMMER_IMAGE_HEIGHT
import com.timilehinaregbesola.mathalarm.presentation.ui.MathAlarmTheme
import com.timilehinaregbesola.mathalarm.utils.Destinations.AppSettings
import com.timilehinaregbesola.mathalarm.utils.Destinations.SettingsSheet
import com.timilehinaregbesola.mathalarm.utils.UiEvent.Navigate
import com.timilehinaregbesola.mathalarm.utils.UiEvent.ShowError
import com.timilehinaregbesola.mathalarm.utils.UiEvent.ShowSnackbar
import com.timilehinaregbesola.mathalarm.utils.UiEvent.SnackbarAction
import com.timilehinaregbesola.mathalarm.utils.getTimeLeft
import kotlinx.serialization.json.Json
import mathalarm.app.generated.resources.Res
import mathalarm.app.generated.resources.fab_icon
import org.jetbrains.compose.resources.painterResource
import org.koin.compose.viewmodel.koinViewModel

@ExperimentalAnimationApi
@ExperimentalFoundationApi
@ExperimentalMaterial3Api
@Composable
fun ListDisplayScreen(
    viewModel: AlarmListViewModel = koinViewModel(),
    backstack: NavBackStack<NavKey>,
    darkTheme: Boolean,
) {
    val alarms by viewModel.alarms.collectAsState()
    val alarmPermission = viewModel.permission
    var deleteAllAlarmsDialog by remember { mutableStateOf(false) }
    val snackbarHoststate = remember {
        SnackbarHostState()
    }
    var showPermissionDialog by remember { mutableStateOf(false) }
    var isLoading by remember { mutableStateOf(false) }
    var fabClearance by remember { mutableStateOf(120.dp) }
    val density = LocalDensity.current

    val now by rememberAlarmNow()
    val expiredSkips = alarms.orEmpty().filter { alarm ->
        alarm.skippedDate != null && (alarm.skippedTime(TimeZone.currentSystemDefault())
            ?.let { it <= now.toEpochMilliseconds() } != false)
    }.map { it.alarmId }
    LaunchedEffect(expiredSkips) { if (expiredSkips.isNotEmpty()) viewModel.expireSkips() }

    val errorStrings = strings
    LaunchedEffect(errorStrings) {
        viewModel.uiEvent.collect { event ->
            when (event) {
                is ShowError -> {
                    snackbarHoststate.showSnackbar(message = event.error.resolve(errorStrings))
                }
                is ShowSnackbar -> {
                    val result = snackbarHoststate.showSnackbar(
                        message = event.skippedDate?.let {
                            errorStrings.skippedAlarmOn(formatShortDate(it, errorStrings.dateLocale))
                        } ?: event.message,
                        actionLabel = if (event.actionType == SnackbarAction.UNDO_SKIP) errorStrings.undoSkip else event.action,
                        withDismissAction = true,
                        duration = SnackbarDuration.Short
                    )
                    if (result == ActionPerformed) {
                        when (event.actionType) {
                            SnackbarAction.UNDO_DELETE -> viewModel.onEvent(OnUndoDeleteClick)
                            SnackbarAction.UNDO_SKIP -> event.relatedAlarmId?.let {
                                viewModel.onEvent(OnUndoSkipClick(it, event.skippedDate))
                            }
                            null -> Unit
                        }
                    }
                }

                is Navigate -> {
                    val alarmJson = Json.encodeToString(AlarmMapper().mapFromDomainModel(event.alarm))
                    backstack.removeAll { it is SettingsSheet }
                    backstack.add(SettingsSheet(alarmJson))
                    isLoading = false
                }

                else -> Unit
            }
        }
    }

    if (alarms == null) {
        ListLoadingShimmer(imageHeight = LOADING_SHIMMER_IMAGE_HEIGHT, isDark = darkTheme)
    }
    alarms?.let { alarmList ->
        Scaffold(
            topBar = {
                ListTopAppBar(
                    openDialog = {
                        if (alarmList.isNotEmpty()) {
                            deleteAllAlarmsDialog = true
                        } else {
                            viewModel.onEvent(OnClearEmptyAlarmsClick)
                        }
                    },
                    onSettingsClick = {
                        backstack.add(AppSettings)
                    },
                )
            },
            snackbarHost = { AlarmSnack(
                modifier = Modifier.padding(bottom = if (alarmList.isEmpty()) 0.dp else fabClearance),
                state = snackbarHoststate,
            ) },
        ) { padding ->
            AlarmPermissionDialog(
                isDialogOpen = showPermissionDialog,
                onCloseDialog = { showPermissionDialog = false },
            )
            ClearDialog(
                openDialog = deleteAllAlarmsDialog,
                onClear = { viewModel.onEvent(OnClearAlarmsClick) },
                onCloseDialog = { deleteAllAlarmsDialog = false },
            )
            Box(
                modifier = Modifier
                    .padding(padding)
                    .fillMaxSize(),
                contentAlignment = TopStart,
            ) {
                val alarmSetText = strings.alarmSet
                BoxWithConstraints(
                    modifier = Modifier
                        .align(TopCenter)
                        .fillMaxSize(),
                ) {
                    val listWidthModifier = if (maxWidth > LIST_CONTENT_MAX_WIDTH + 32.dp) {
                        Modifier.width(LIST_CONTENT_MAX_WIDTH + 32.dp)
                    } else {
                        Modifier.fillMaxWidth()
                    }

                    Box(
                        modifier = listWidthModifier
                            .fillMaxHeight()
                            .align(TopCenter),
                    ) {
                        if (alarmList.isEmpty()) {
                            AlarmEmptyScreen(
                                modifier = Modifier.fillMaxSize().padding(horizontal = 16.dp),
                                onClickFab = {
                                    viewModel.onEvent(OnAddAlarmClick)
                                },
                                darkTheme = darkTheme,
                            )
                        } else {
                            AlarmListContent(
                                modifier = Modifier.fillMaxSize(),
                                bottomClearance = fabClearance,
                                alarmList = alarmList,
                                darkTheme = darkTheme,
                                onEditAlarm = {
                                    isLoading = true
                                    checkPermissionAndPerformAction(
                                        value = alarmPermission.hasExactAlarmPermission(),
                                        action = { viewModel.onEvent(OnEditAlarmClick(it)) },
                                        onPermissionAbsent = { showPermissionDialog = true },
                                    )
                                },
                                onDeleteAlarm = {
                                    backstack.removeSettingsSheetFor(it)
                                    viewModel.onEvent(OnDeleteAlarmClick(it))
                                },
                                onCancelAlarm = viewModel::cancelAlarm,
                                onSkipNext = { viewModel.onEvent(OnSkipNextClick(it.alarmId)) },
                                onUndoSkip = { viewModel.onEvent(OnUndoSkipClick(it.alarmId, it.skippedDate)) },
                                onScheduleAlarm = { curAlarm: Alarm, b: Boolean ->
                                    checkPermissionAndPerformAction(
                                        value = alarmPermission.hasExactAlarmPermission(),
                                        action = {
                                            viewModel.scheduleAlarm(
                                                alarm = curAlarm,
                                                reschedule = b,
                                                message = "$alarmSetText ${curAlarm.copy(skippedDate = null, scheduleInitialized = false, snoozedUntil = null).getTimeLeft()}",
                                            )
                                        },
                                        onPermissionAbsent = { showPermissionDialog = true },
                                    )
                                }
                            )
                            val fabImage = painterResource(Res.drawable.fab_icon)
                            AddAlarmFab(
                                modifier = Modifier
                                    .onSizeChanged { fabClearance = with(density) { it.height.toDp() } + 16.dp }
                                    .align(BottomEnd)
                                    .padding(
                                        end = 24.dp,
                                        bottom = 16.dp,
                                    ),
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
                        }
                    }
                }
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

@OptIn(
    ExperimentalAnimationApi::class,
    ExperimentalMaterial3Api::class,
)
@Composable
private fun AlarmListContent(
    modifier: Modifier = Modifier,
    bottomClearance: androidx.compose.ui.unit.Dp = 120.dp,
    alarmList: List<Alarm>,
    darkTheme: Boolean,
    onEditAlarm: (Alarm) -> Unit,
    onDeleteAlarm: (Alarm) -> Unit,
    onCancelAlarm: (Alarm) -> Unit,
    onSkipNext: (Alarm) -> Unit,
    onUndoSkip: (Alarm) -> Unit,
    onScheduleAlarm: (Alarm, Boolean) -> Unit,
) {
    val hazeState = remember { HazeState() }
    Surface(
        modifier = modifier,
    ) {
        LazyColumn(
            contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = bottomClearance),
            horizontalAlignment = CenterHorizontally,
        ) {
            stickyHeader(
                key = "sticky_header"
            ) {
                ListHeader(
                    modifier = Modifier.padding(horizontal = 16.dp),
                    hazeState = hazeState,
                    enabled = alarmList.any { it.isOn },
                    alarmList = alarmList,
                )
            }
            items(
                items = alarmList,
                key = { alarm -> alarm.alarmId },
            ) { alarm ->
                // Include the existing side gutters in the capture layer so card shadows
                // can fade beyond the rounded outline instead of clipping to card width.
                Box(
                    Modifier.fillMaxWidth()
                        .hazeSource(state = hazeState, key = alarm.alarmId)
                        .padding(horizontal = 16.dp)
                ) {
                    AlarmItem(
                        alarm = alarm,
                        onEditAlarm = {
                            onEditAlarm(alarm)
                        },
                        onDeleteAlarm = onDeleteAlarm,
                        onCancelAlarm = onCancelAlarm,
                        onSkipNext = onSkipNext,
                        onUndoSkip = onUndoSkip,
                        onScheduleAlarm = onScheduleAlarm,
                        darkTheme = darkTheme,
                    )
                }
            }
        }
    }
}

private fun NavBackStack<NavKey>.removeSettingsSheetFor(alarm: Alarm) {
    removeAll { destination ->
        val settingsSheet = destination as? SettingsSheet ?: return@removeAll false
        val settingsAlarm = Json.decodeFromString<AlarmEntity>(settingsSheet.settingsAlarm)
        settingsAlarm.alarmId == alarm.alarmId
    }
}

private fun checkPermissionAndPerformAction(
    value: Boolean,
    action: () -> Unit,
    onPermissionAbsent: () -> Unit
) {
    if (value) {
        action()
    } else {
        onPermissionAbsent()
    }
}

@Composable
internal fun AlarmPermissionDialog(
    isDialogOpen: Boolean,
    onCloseDialog: () -> Unit,
) {
    val arguments = DialogArguments(
        title = strings.alarms,
        text = strings.taskAlarmPermissionDialogText,
        confirmText = strings.taskAlarmPermissionDialogConfirm,
        dismissText = strings.taskAlarmPermissionDialogCancel,
        onConfirmAction = {
            requestExactAlarmPermission()
            onCloseDialog()
        },
    )
    MathAlarmDialog(
        arguments = arguments,
        isDialogOpen = isDialogOpen,
        onDismissRequest = onCloseDialog,
    )
}

@Preview
@Composable
private fun AlarmListScreenPreview() {
    MathAlarmTheme {
        AlarmListContent(
            alarmList = listOf(Alarm(), Alarm(alarmId = 1L)),
            darkTheme = false,
            onEditAlarm = {},
            onDeleteAlarm = {},
            onCancelAlarm = {},
            onSkipNext = {},
            onUndoSkip = {},
        ) { _, _ -> }
    }
}

private object AlarmListScreen {
    val LOADING_SHIMMER_IMAGE_HEIGHT = 180.dp
    val LOADER_SIZE = 50.dp
    val LIST_CONTENT_MAX_WIDTH = 720.dp
}
