package com.timilehinaregbesola.mathalarm.presentation.alarmsettings.components

import com.timilehinaregbesola.mathalarm.presentation.alarmlist.components.AlarmPermissionDialog

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Arrangement.SpaceBetween
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.ButtonDefaults.buttonColors
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.rememberBottomSheetScaffoldState
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.imePadding
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Alignment.Companion.TopCenter
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight.Companion.Bold
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign.Companion.Center
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import cafe.adriel.lyricist.strings
import co.touchlab.kermit.Logger
import com.mohamedrejeb.calf.ui.button.AdaptiveButton
import com.mohamedrejeb.calf.ui.button.AdaptiveIconButton
import com.mohamedrejeb.calf.ui.gesture.adaptiveClickable
import com.mohamedrejeb.calf.ui.timepicker.rememberAdaptiveTimePickerState
import com.timilehinaregbesola.mathalarm.domain.model.MathChallenge
import com.timilehinaregbesola.mathalarm.framework.database.AlarmEntity
import com.timilehinaregbesola.mathalarm.framework.database.AlarmMapper
import com.timilehinaregbesola.mathalarm.platform.areNotificationsEnabled
import com.timilehinaregbesola.mathalarm.platform.checkRingtonePermissions
import com.timilehinaregbesola.mathalarm.platform.getRingtoneTitle
import com.timilehinaregbesola.mathalarm.platform.isIosPlatform
import com.timilehinaregbesola.mathalarm.platform.openNotificationSettings
import com.timilehinaregbesola.mathalarm.platform.previewAlarmTone
import com.timilehinaregbesola.mathalarm.platform.rememberNotificationPermissionHandler
import com.timilehinaregbesola.mathalarm.platform.rememberRingtonePickerLauncher
import com.timilehinaregbesola.mathalarm.platform.stopAlarmTonePreview
import com.timilehinaregbesola.mathalarm.presentation.alarmlist.components.DialogArguments
import com.timilehinaregbesola.mathalarm.presentation.alarmlist.components.MathAlarmDialog
import com.timilehinaregbesola.mathalarm.presentation.alarmsettings.AddEditAlarmEvent
import com.timilehinaregbesola.mathalarm.presentation.alarmsettings.AddEditAlarmEvent.EnteredTitle
import com.timilehinaregbesola.mathalarm.presentation.alarmsettings.AddEditAlarmEvent.OnSaveTodoClick
import com.timilehinaregbesola.mathalarm.presentation.alarmsettings.AddEditAlarmEvent.OnTestClick
import com.timilehinaregbesola.mathalarm.presentation.alarmsettings.AddEditAlarmEvent.OnToneChange
import com.timilehinaregbesola.mathalarm.presentation.alarmsettings.AddEditAlarmEvent.OnToneError
import com.timilehinaregbesola.mathalarm.presentation.alarmsettings.AddEditAlarmEvent.ToggleDayChooser
import com.timilehinaregbesola.mathalarm.presentation.alarmsettings.AddEditAlarmEvent.ToggleRepeat
import com.timilehinaregbesola.mathalarm.presentation.alarmsettings.AddEditAlarmEvent.ToggleSnooze
import com.timilehinaregbesola.mathalarm.presentation.alarmsettings.AddEditAlarmEvent.ToggleVibrate
import com.timilehinaregbesola.mathalarm.presentation.alarmsettings.AlarmSettingsViewModel
import com.timilehinaregbesola.mathalarm.presentation.alarmsettings.TimeState
import com.timilehinaregbesola.mathalarm.presentation.alarmsettings.components.AlarmBottomSheet.ALARM_DAYS_TOP_PADDING
import com.timilehinaregbesola.mathalarm.presentation.alarmsettings.components.AlarmBottomSheet.MIDDLE_CONTROL_SECTION_TOP_PADDING
import com.timilehinaregbesola.mathalarm.presentation.alarmsettings.components.AlarmBottomSheet.NO_ELEVATION
import com.timilehinaregbesola.mathalarm.presentation.alarmsettings.components.AlarmBottomSheet.SAVE_BUTTON_FONT_SIZE
import com.timilehinaregbesola.mathalarm.presentation.alarmsettings.components.AlarmBottomSheet.SAVE_BUTTON_TOP_PADDING
import com.timilehinaregbesola.mathalarm.presentation.alarmsettings.components.AlarmBottomSheet.SETTINGS_CONTENT_MAX_WIDTH
import com.timilehinaregbesola.mathalarm.presentation.alarmsettings.components.AlarmBottomSheet.TEST_BUTTON_FONT_SIZE
import com.timilehinaregbesola.mathalarm.presentation.alarmsettings.components.AlarmBottomSheet.TIME_CARD_CORNER_SIZE
import com.timilehinaregbesola.mathalarm.presentation.alarmsettings.components.AlarmBottomSheet.TIME_CARD_HEIGHT
import com.timilehinaregbesola.mathalarm.presentation.alarmsettings.components.AlarmBottomSheet.TIME_TEXT_FONT_SIZE
import com.timilehinaregbesola.mathalarm.presentation.alarmsettings.components.AlarmBottomSheet.TONE_PICKER_DIALOG_ELEVATION
import com.timilehinaregbesola.mathalarm.presentation.alarmsettings.components.AlarmBottomSheet.TONE_PICKER_DIALOG_MAX_WIDTH
import com.timilehinaregbesola.mathalarm.presentation.alarmsettings.components.AlarmBottomSheet.TONE_PICKER_DIALOG_PADDING
import com.timilehinaregbesola.mathalarm.presentation.alarmsettings.components.AlarmBottomSheet.TONE_PICKER_DIVIDER_ALPHA
import com.timilehinaregbesola.mathalarm.presentation.alarmsettings.components.AlarmBottomSheet.TONE_PICKER_DIVIDER_START_PADDING
import com.timilehinaregbesola.mathalarm.presentation.alarmsettings.components.AlarmBottomSheet.TONE_PICKER_HEADER_HEIGHT
import com.timilehinaregbesola.mathalarm.presentation.alarmsettings.components.AlarmBottomSheet.TONE_PICKER_MAX_HEIGHT
import com.timilehinaregbesola.mathalarm.presentation.alarmsettings.components.AlarmBottomSheet.TONE_PICKER_ROW_HEIGHT
import com.timilehinaregbesola.mathalarm.presentation.alarmsettings.components.AlarmBottomSheet.TONE_PICKER_SEPARATOR_THICKNESS
import com.timilehinaregbesola.mathalarm.presentation.ui.MathAlarmTheme
import com.timilehinaregbesola.mathalarm.presentation.ui.icon.Check
import com.timilehinaregbesola.mathalarm.presentation.ui.icon.Close
import com.timilehinaregbesola.mathalarm.presentation.ui.icon.Notifications
import com.timilehinaregbesola.mathalarm.presentation.ui.icon.PlayArrow
import com.timilehinaregbesola.mathalarm.presentation.ui.icon.Stop
import com.timilehinaregbesola.mathalarm.presentation.ui.spacing
import com.timilehinaregbesola.mathalarm.utils.Destinations.AlarmMath
import com.timilehinaregbesola.mathalarm.utils.Destinations.SettingsSheet
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalTime
import kotlinx.datetime.format
import kotlinx.datetime.format.char
import kotlinx.serialization.json.Json
import org.koin.compose.viewmodel.koinViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmBottomSheet(
    viewModel: AlarmSettingsViewModel = koinViewModel(),
    backstack: NavBackStack<NavKey>,
    darkTheme: Boolean,
    alarm: AlarmEntity,
    showDismissButton: Boolean,
    isPane: Boolean = false,
    onDraftStateChange: (Boolean) -> Unit = {},
) {
    LaunchedEffect(Unit) {
        viewModel.setAlarm(AlarmMapper().mapToDomainModel(alarm))
    }
    val scaffoldState = rememberBottomSheetScaffoldState()
    var showTimePickerDialog by rememberSaveable { mutableStateOf(false) }
    var showTonePickerDialog by remember { mutableStateOf(false) }
    var showConfirmationDialog by remember { mutableStateOf(false) }
    var showExactAlarmPermissionDialog by remember { mutableStateOf(false) }
    var showPermRequiredDialog by remember { mutableStateOf(false) }

    var editingSubPage by remember { mutableStateOf(false) }
    val hasDraft = viewModel.hasUnsavedChanges || editingSubPage || showTimePickerDialog || showTonePickerDialog
    SideEffect { onDraftStateChange(hasDraft) }

    val toneUri = viewModel.tone.value
    val toneText = remember(toneUri) { mutableStateOf<String?>(null) }
    LaunchedEffect(toneUri) {
        if (toneUri.isNotEmpty()) {
            toneText.value = withContext(Dispatchers.Default) {
                getRingtoneTitle(toneUri)
            }
        }
    }
    val closeSettings: () -> Unit = {
        if (backstack.lastOrNull() is SettingsSheet) backstack.removeLastOrNull()
    }

    // Capture string values for use in non-composable callbacks
    val alertTitle = strings.alert
    val storagePermissionTextFn = strings.permissionsExternalStorageText

    // Use platform-abstracted ringtone picker
    val pickToneLauncher = rememberRingtonePickerLauncher { selectedTone ->
        selectedTone?.let { alert ->
            checkRingtonePermissions(
                tones = listOf(alert),
                unplayableDialogTitle = alertTitle,
                unplayableDialogMessage = storagePermissionTextFn,
            )
            viewModel.onEvent(OnToneChange(alert))
        }
    }

    // Use platform-abstracted notification permission handler
    val requestNotificationPermission = rememberNotificationPermissionHandler { granted ->
        if (granted) {
            if (areNotificationsEnabled()) {
                viewModel.onEvent(OnSaveTodoClick)
            } else {
                showConfirmationDialog = true
            }
        } else {
            showPermRequiredDialog = true
        }
    }

    val errorStrings = strings
    LaunchedEffect(errorStrings) {
        viewModel.eventFlow.collectLatest { event ->
            when (event) {
                AlarmSettingsViewModel.UiEvent.RequestExactAlarmPermission -> showExactAlarmPermissionDialog = true
                is AlarmSettingsViewModel.UiEvent.ShowError -> {
                    scaffoldState.snackbarHostState.showSnackbar(message = event.error.resolve(errorStrings))
                }
                is AlarmSettingsViewModel.UiEvent.SaveAlarm -> {
                    closeSettings()
                }
                is AlarmSettingsViewModel.UiEvent.TestAlarm -> {
                    launch(Dispatchers.Default) {
                        val alarmEntity = AlarmMapper().mapFromDomainModel(event.alarm)
                        val json = Json.encodeToString(alarmEntity)
                        withContext(Dispatchers.Main) {
                            backstack.add(AlarmMath(alarmJson = json, fromSheet = true))
                        }
                    }
                }
            }
        }
    }
    AlarmBottomSheetContent(
        challenge = viewModel.challenge.value,
        snoozeEnabled = viewModel.snoozeEnabled.value,
        snoozeMinutes = viewModel.snoozeMinutes,
        maxSnoozes = viewModel.maxSnoozes.value,
        onSnoozeChange = { enabled, minutes, maximum ->
            viewModel.onEvent(ToggleSnooze(enabled))
            viewModel.onEvent(AddEditAlarmEvent.ChangeSnoozeDuration(minutes))
            viewModel.onEvent(AddEditAlarmEvent.ChangeMaxSnoozes(maximum))
        },
        onChallengeChange = {
            viewModel.onEvent(AddEditAlarmEvent.OnChallengeChange(it))
        },
        onCloseClick = closeSettings,
        showDismissButton = showDismissButton,
        isPane = isPane,
        onSubEditorChanged = { editingSubPage = it },
        topSection = {
            TopSection(
                selectedDays = viewModel.dayChooser.value,
                currentTime = viewModel.alarmTime.value.formattedTime,
                onTimeCardClick = { showTimePickerDialog = true },
                onSelectedDaysChanged = {
                    viewModel.onEvent(ToggleDayChooser(it))
                }
            )
        },
        bottomSection = { onEditChallenge, onEditSnooze ->
            val noPickerText = strings.noRingtonePicker
            val defaultToneText = strings.defaultAlarmTone
            BottomSettingsSection(
                onEditChallenge = onEditChallenge,
                onEditSnooze = onEditSnooze,
                repeatWeekly = viewModel.repeatWeekly.value,
                snoozeEnabled = viewModel.snoozeEnabled.value,
                onSnoozeToggle = { viewModel.onEvent(ToggleSnooze(it)) },
                maxSnoozes = viewModel.maxSnoozes.value,
                snoozeMinutes = viewModel.snoozeMinutes,
                vibrate = viewModel.vibrate.value,
                challenge = viewModel.challenge.value,
                onRepeatToggle = {
                    viewModel.onEvent(ToggleRepeat(it))
                },
                onVibrateToggle = {
                    viewModel.onEvent(ToggleVibrate(it))
                },
                onToneClick = {
                    if (isIosPlatform()) {
                        showTonePickerDialog = true
                    } else {
                        try {
                            pickToneLauncher.launch(viewModel.tone.value.ifEmpty { null })
                        } catch (e: Exception) {
                            Logger.e("error launching tone picker", e)
                            viewModel.onEvent(
                                OnToneError(message = noPickerText)
                            )
                        }
                    }
                },
                labelTextField = {
                    LabelTextField(
                        text = viewModel.alarmTitle.value,
                        onValueChange = { newValue ->
                            viewModel.onEvent(EnteredTitle(newValue))
                        },
                        label = { Text(strings.alarmTitle) },
                        placeholder = { Text(strings.goodDay) },
                    )
                },
                currentTone = toneText.value?.takeIf { it.isNotBlank() } ?: defaultToneText
            )
        },
        onTestClick = {
            viewModel.onEvent(OnTestClick)
        },
        onSaveClick = {
            requestNotificationPermission()
        },
        dialogSection = {
            AlarmPermissionDialog(
                isDialogOpen = showExactAlarmPermissionDialog,
                onCloseDialog = { showExactAlarmPermissionDialog = false },
            )
            with(viewModel.alarmTime.value) {
                if (showTimePickerDialog) {
                    TimePickerDialog(
                        embedded = isPane,
                        timeState = rememberAdaptiveTimePickerState(
                            initialHour = hour,
                            initialMinute = minute,
                            is24Hour = false,
                        ),
                        darkTheme = darkTheme,
                        onCancel = {
                            showTimePickerDialog = false
                        },
                        onConfirm = { newTime ->
                            val tf = LocalTime.Format {
                                amPmHour()
                                char(':')
                                minute()
                                char(' ')
                                amPmMarker("AM", "PM")
                            }
                            viewModel.onEvent(
                                AddEditAlarmEvent.ChangeTime(
                                    TimeState(
                                        hour = newTime.hour,
                                        minute = newTime.minute,
                                        formattedTime = newTime.format(tf)
                                    ),
                                ),
                            )
                            showTimePickerDialog = false
                        }
                    )
                }
            }
            if (showTonePickerDialog) {
                AlarmTonePickerDialog(
                    currentTone = viewModel.tone.value,
                    onDismissRequest = {
                        stopAlarmTonePreview()
                        showTonePickerDialog = false
                    },
                    onToneSelected = { selectedTone ->
                        stopAlarmTonePreview()
                        checkRingtonePermissions(
                            tones = listOf(selectedTone),
                            unplayableDialogTitle = alertTitle,
                            unplayableDialogMessage = storagePermissionTextFn,
                        )
                        viewModel.onEvent(OnToneChange(selectedTone))
                        showTonePickerDialog = false
                    }
                )
            }
            MathAlarmDialog(
                arguments = DialogArguments(
                    title = strings.alert,
                    text = strings.disabledNotificationMessageExtended,
                    confirmText = strings.ok,
                    dismissText = null,
                    onConfirmAction = {
                        viewModel.onEvent(OnSaveTodoClick)
                        showConfirmationDialog = false
                    }
                ),
                isDialogOpen = showConfirmationDialog,
                onDismissRequest = { showConfirmationDialog = false }
            )
            MathAlarmDialog(
                arguments = DialogArguments(
                    title = strings.alert,
                    text = strings.notificationPermissionDialogMessage,
                    confirmText = strings.grantPermission,
                    dismissText = strings.cancel,
                    onConfirmAction = {
                        openNotificationSettings()
                        showPermRequiredDialog = false
                    }
                ),
                isDialogOpen = showPermRequiredDialog,
                onDismissRequest = { showPermRequiredDialog = false }
            )
        }
    )
}

@Composable
private fun AlarmBottomSheetContent(
    onCloseClick: () -> Unit,
    showDismissButton: Boolean,
    isPane: Boolean = false,
    onSubEditorChanged: (Boolean) -> Unit = {},
    topSection: @Composable () -> Unit,
    bottomSection: @Composable (onEditChallenge: () -> Unit, onEditSnooze: () -> Unit) -> Unit,
    onTestClick: () -> Unit,
    onSaveClick: () -> Unit,
    challenge: MathChallenge = MathChallenge(),
    snoozeEnabled: Boolean = true,
    snoozeMinutes: Int = 5,
    maxSnoozes: Int = 3,
    onChallengeChange: (MathChallenge) -> Unit = {},
    onSnoozeChange: (Boolean, Int, Int) -> Unit = { _, _, _ -> },
    dialogSection: @Composable () -> Unit
) {
    AlarmSettingsSheetHost(
        modifier = if (isPane) Modifier.safeDrawingPadding().imePadding() else Modifier,
        challenge = challenge,
        snoozeEnabled = snoozeEnabled,
        snoozeMinutes = snoozeMinutes,
        maxSnoozes = maxSnoozes,
        onSubEditorChanged = onSubEditorChanged,
        onChallengeApply = onChallengeChange,
        onSnoozeApply = onSnoozeChange,
    ) { onEditChallenge, onEditSnooze ->
        with(MaterialTheme) {
            val useFullHeightSheetLayout = isIosPlatform()
            Surface(
                modifier = if (useFullHeightSheetLayout) {
                    Modifier.fillMaxSize()
                } else {
                    Modifier
                }
            ) {
                BoxWithConstraints(
                    modifier = if (useFullHeightSheetLayout) {
                        Modifier.fillMaxSize()
                    } else {
                        Modifier.fillMaxWidth()
                    },
                    contentAlignment = TopCenter,
                ) {
                    val contentWidthModifier = if (maxWidth > SETTINGS_CONTENT_MAX_WIDTH) {
                        Modifier.width(SETTINGS_CONTENT_MAX_WIDTH)
                    } else {
                        Modifier.fillMaxWidth()
                    }
                    val sheetPaddingModifier = Modifier.padding(
                        start = spacing.extraMedium,
                        end = spacing.extraMedium,
                        bottom = spacing.extraMedium,
                        top = spacing.medium
                    )
                    if (useFullHeightSheetLayout) {
                        Column(
                            contentWidthModifier
                                .fillMaxSize()
                                .then(sheetPaddingModifier),
                        ) {
                            SheetHeader(
                                onCloseClick = onCloseClick,
                                onSaveClick = onSaveClick,
                                showSaveAction = true,
                            )
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .verticalScroll(rememberScrollState()),
                            ) {
                                SheetSettingsContent(
                                    topSection = topSection,
                                    bottomSection = {
                                        bottomSection(onEditChallenge, onEditSnooze)
                                    },
                                )
                            }
                            SheetActionButtons(
                                onTestClick = onTestClick,
                                onSaveClick = onSaveClick,
                                showSaveButton = false,
                            )
                        }
                    } else {
                        Column(
                            contentWidthModifier
                                .fillMaxSize()
                                .then(sheetPaddingModifier),
                        ) {
                            if (showDismissButton) {
                                SheetHeader(onCloseClick = onCloseClick)
                            }
                            Column(
                                modifier = Modifier
                                    .weight(1f)
                                    .verticalScroll(rememberScrollState())
                            ) {
                                SheetSettingsContent(
                                    topSection = topSection,
                                    bottomSection = { bottomSection(onEditChallenge, onEditSnooze) },
                                )
                            }
                            SheetActionButtons(
                                onTestClick = onTestClick,
                                onSaveClick = onSaveClick,
                                showSaveButton = true,
                            )
                        }
                    }
                    dialogSection()
                }
            }
        }
    }
}

@Composable
private fun SheetHeader(
    modifier: Modifier = Modifier,
    onCloseClick: () -> Unit,
    onSaveClick: (() -> Unit)? = null,
    showSaveAction: Boolean = false,
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .padding(bottom = MaterialTheme.spacing.small),
        horizontalArrangement = if (showSaveAction) SpaceBetween else Arrangement.End,
        verticalAlignment = CenterVertically,
    ) {
        AdaptiveIconButton(onClick = onCloseClick) {
            Icon(
                modifier = Modifier.size(AlarmBottomSheet.HEADER_ICON_SIZE),
                imageVector = Close,
                contentDescription = "Dismiss",
            )
        }
        if (showSaveAction && onSaveClick != null) {
            AdaptiveIconButton(onClick = onSaveClick) {
                Icon(
                    modifier = Modifier.size(AlarmBottomSheet.HEADER_ICON_SIZE),
                    imageVector = Check,
                    contentDescription = strings.save,
                    tint = MaterialTheme.colorScheme.secondary,
                )
            }
        }
    }
}

@Composable
private fun SheetSettingsContent(
    topSection: @Composable () -> Unit,
    bottomSection: @Composable () -> Unit,
) {
    topSection()
    HorizontalDivider(
        modifier = Modifier.padding(
            top = MaterialTheme.spacing.medium,
            start = MaterialTheme.spacing.medium,
            end = MaterialTheme.spacing.medium,
        ),
        color = MaterialTheme.colorScheme.outlineVariant
    )
    bottomSection()
}

@Composable
fun TopSection(
    selectedDays: String,
    currentTime: String,
    onTimeCardClick: () -> Unit,
    onSelectedDaysChanged: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(TIME_CARD_HEIGHT)
            .padding(horizontal = MaterialTheme.spacing.medium),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHighest,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = NO_ELEVATION),
        shape = MaterialTheme.shapes.medium.copy(CornerSize(TIME_CARD_CORNER_SIZE)),
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .adaptiveClickable(
                    shape = MaterialTheme.shapes.medium.copy(CornerSize(TIME_CARD_CORNER_SIZE)),
                    onClick = { onTimeCardClick() }
                ),
            horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = MaterialTheme.spacing.medium),
                text = currentTime,
                fontSize = TIME_TEXT_FONT_SIZE,
                fontWeight = Bold,
                textAlign = Center,
            )
        }
    }
    Spacer(modifier = Modifier.height(ALARM_DAYS_TOP_PADDING))
    AlarmDays(currentDays = selectedDays) {
        onSelectedDaysChanged(it)
    }
}

@Composable
private fun BottomSettingsSection(
    onEditChallenge: () -> Unit,
    onEditSnooze: () -> Unit,
    repeatWeekly: Boolean,
    snoozeEnabled: Boolean,
    vibrate: Boolean,
    onRepeatToggle: (Boolean) -> Unit,
    onVibrateToggle: (Boolean) -> Unit,
    onToneClick: () -> Unit,
    labelTextField: @Composable () -> Unit,
    currentTone: String,
    challenge: MathChallenge = MathChallenge(),
    maxSnoozes: Int = 3,
    snoozeMinutes: Int = 5,
    onSnoozeToggle: (Boolean) -> Unit = {},
) {
    val showVibrateToggle = !isIosPlatform()

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = MaterialTheme.spacing.medium)
    ) {
        Row(
            modifier = Modifier
                .padding(top = MIDDLE_CONTROL_SECTION_TOP_PADDING)
                .fillMaxWidth(),
            horizontalArrangement = SpaceBetween,
        ) {
            TextWithCheckbox(
                text = strings.repeatWeekly,
                initialState = repeatWeekly,
            ) {
                onRepeatToggle(it)
            }
            if (showVibrateToggle) {
                TextWithCheckbox(
                    text = strings.vibrate,
                    initialState = vibrate,
                ) {
                    onVibrateToggle(it)
                }
            }
        }
        SnoozeSettings(
            enabled = snoozeEnabled,
            onEdit = onEditSnooze,
            maxSnoozes = maxSnoozes,
            snoozeMinutes = snoozeMinutes,
            onEnabledChange = onSnoozeToggle,
        )
        labelTextField()
        TextWithIcon(
            text = currentTone,
            image = Notifications,
            onClick = {
                onToneClick()
            },
        )
        MathChallengeSettings(
            challenge = challenge,
            onEdit = onEditChallenge,
            modifier = Modifier.padding(top = MaterialTheme.spacing.large),
        )
    }
}

@Composable
private fun SheetActionButtons(
    onTestClick: () -> Unit,
    onSaveClick: () -> Unit,
    showSaveButton: Boolean = true,
) {
    SheetFooter(
        secondaryLabel = strings.testAlarm.uppercase(),
        primaryLabel = strings.save.uppercase(),
        onSecondaryClick = onTestClick,
        onPrimaryClick = onSaveClick,
        showPrimary = showSaveButton,
    )
}

/** Shared geometry keeps actions stationary when switching sheet views. */
@Composable
internal fun SheetFooter(
    secondaryLabel: String,
    primaryLabel: String,
    onSecondaryClick: () -> Unit,
    onPrimaryClick: () -> Unit,
    primaryEnabled: Boolean = true,
    showPrimary: Boolean = true,
) {
    AdaptiveButton(
        modifier = Modifier
            .padding(top = MaterialTheme.spacing.medium)
            .fillMaxWidth(),
        onClick = onSecondaryClick,
        colors = buttonColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            contentColor = MaterialTheme.colorScheme.onSurface
        ),
    ) {
        Text(fontSize = TEST_BUTTON_FONT_SIZE, text = secondaryLabel)
    }
    if (showPrimary) {
        AdaptiveButton(
            modifier = Modifier.padding(top = SAVE_BUTTON_TOP_PADDING).fillMaxWidth(),
            onClick = onPrimaryClick,
            enabled = primaryEnabled,
            colors = buttonColors(
                containerColor = MaterialTheme.colorScheme.secondary,
                contentColor = MaterialTheme.colorScheme.onSecondary,
            ),
        ) {
            Text(fontSize = SAVE_BUTTON_FONT_SIZE, text = primaryLabel)
        }
    }
}

@Composable
private fun AlarmTonePickerDialog(
    currentTone: String,
    onDismissRequest: () -> Unit,
    onToneSelected: (String) -> Unit,
) {
    var pendingTone by remember(currentTone) {
        mutableStateOf(currentTone.ifEmpty { IosAlarmToneOptions.first().filename })
    }
    var previewingTone by remember { mutableStateOf<String?>(null) }
    val selectAndPreview: (IosAlarmToneOption) -> Unit = { tone ->
        pendingTone = tone.filename
        previewAlarmTone(tone.filename)
        previewingTone = tone.filename
    }

    DisposableEffect(Unit) {
        onDispose {
            stopAlarmTonePreview()
        }
    }

    Dialog(onDismissRequest = onDismissRequest) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .widthIn(max = TONE_PICKER_DIALOG_MAX_WIDTH),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = TONE_PICKER_DIALOG_ELEVATION,
        ) {
            Column(
                modifier = Modifier.padding(TONE_PICKER_DIALOG_PADDING),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(TONE_PICKER_HEADER_HEIGHT),
                    verticalAlignment = CenterVertically,
                ) {
                    Spacer(modifier = Modifier.size(AlarmBottomSheet.TONE_ACTION_PLACEHOLDER_SIZE))
                    Text(
                        modifier = Modifier.weight(1f),
                        text = "Alarm Sound",
                        fontSize = 17.sp,
                        fontWeight = Bold,
                        textAlign = Center,
                    )
                    AdaptiveIconButton(onClick = { onToneSelected(pendingTone) }) {
                        Icon(
                            imageVector = Check,
                            contentDescription = "Done",
                            tint = MaterialTheme.colorScheme.secondary,
                        )
                    }
                }
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.small))
                TonePickerList(
                    pendingTone = pendingTone,
                    previewingTone = previewingTone,
                    selectAndPreview = selectAndPreview,
                    onPreviewClick = { tone ->
                        if (previewingTone == tone.filename) {
                            stopAlarmTonePreview()
                            previewingTone = null
                        } else {
                            selectAndPreview(tone)
                        }
                    },
                )
            }
        }
    }
}

@Composable
private fun TonePickerList(
    pendingTone: String,
    previewingTone: String?,
    selectAndPreview: (IosAlarmToneOption) -> Unit,
    onPreviewClick: (IosAlarmToneOption) -> Unit,
) {
    LazyColumn(
        modifier = Modifier
            .fillMaxWidth()
            .heightIn(max = TONE_PICKER_MAX_HEIGHT),
    ) {
        itemsIndexed(IosAlarmToneOptions) { index, tone ->
            Column {
                AlarmTonePickerRow(
                    title = tone.displayName,
                    selected = tone.filename == pendingTone,
                    isPreviewing = tone.filename == previewingTone,
                    onRowClick = {
                        selectAndPreview(tone)
                    },
                    onPreviewClick = {
                        onPreviewClick(tone)
                    },
                )
                if (index < IosAlarmToneOptions.lastIndex) {
                    HorizontalDivider(
                        modifier = Modifier.padding(start = TONE_PICKER_DIVIDER_START_PADDING),
                        thickness = TONE_PICKER_SEPARATOR_THICKNESS,
                        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = TONE_PICKER_DIVIDER_ALPHA),
                    )
                }
            }
        }
    }
}

@Composable
private fun AlarmTonePickerRow(
    title: String,
    selected: Boolean,
    isPreviewing: Boolean,
    onRowClick: () -> Unit,
    onPreviewClick: () -> Unit,
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(TONE_PICKER_ROW_HEIGHT)
            .adaptiveClickable(
                onClick = onRowClick
            )
            .padding(
                start = MaterialTheme.spacing.medium,
                end = MaterialTheme.spacing.extraSmall,
            ),
        verticalAlignment = CenterVertically,
    ) {
        if (selected) {
            Icon(
                modifier = Modifier
                    .padding(end = MaterialTheme.spacing.small)
                    .size(AlarmBottomSheet.TONE_SELECTION_ICON_SIZE),
                imageVector = Check,
                contentDescription = "Selected",
                tint = MaterialTheme.colorScheme.secondary,
            )
        } else {
            Spacer(
                modifier = Modifier
                    .padding(end = MaterialTheme.spacing.small)
                    .size(AlarmBottomSheet.TONE_SELECTION_ICON_SIZE),
            )
        }
        Text(
            modifier = Modifier.weight(1f),
            text = title,
            fontSize = 16.sp,
        )
        AdaptiveIconButton(onClick = onPreviewClick) {
            Icon(
                imageVector = if (isPreviewing) Stop else PlayArrow,
                contentDescription = if (isPreviewing) "Stop preview" else "Preview",
            )
        }
    }
}

@Preview
@Composable
private fun BottomSheetPreview() {
    MathAlarmTheme(darkTheme = true) {
        Surface {
            AlarmBottomSheetContent(
                onCloseClick = {},
                showDismissButton = true,
                challenge = MathChallenge(difficulty = 1),
                topSection = {
                    TopSection(
                        selectedDays = "TFFFFFF",
                        currentTime = "12:00",
                        onTimeCardClick = {}
                    ) {}
                },
                bottomSection = { onEditChallenge, onEditSnooze ->
                    BottomSettingsSection(
                        onEditChallenge = onEditChallenge,
                        onEditSnooze = onEditSnooze,
                        repeatWeekly = true,
                        snoozeEnabled = true,
                        vibrate = true,
                        challenge = MathChallenge(difficulty = 1),
                        onRepeatToggle = {},
                        onVibrateToggle = {},
                        onToneClick = {},
                        labelTextField = {
                            LabelTextField(
                                text = TextFieldValue(),
                            ) {}
                        },
                        currentTone = "1000",
                    )
                },
                onTestClick = {},
                onSaveClick = {},
            ) {}
        }
    }
}

private data class IosAlarmToneOption(
    val filename: String,
    val displayName: String,
)

private val IosAlarmToneOptions = listOf(
    IosAlarmToneOption("alarm_classic", "Classic"),
    IosAlarmToneOption("alarm_digital", "Digital"),
    IosAlarmToneOption("alarm_gentle", "Gentle"),
    IosAlarmToneOption("alarm_nature", "Nature"),
    IosAlarmToneOption("alarm_urgent", "Urgent"),
)

private object AlarmBottomSheet {
    val HEADER_ICON_SIZE = 32.dp
    val TONE_ACTION_PLACEHOLDER_SIZE = 48.dp
    val TONE_SELECTION_ICON_SIZE = 22.dp
    val TIME_CARD_HEIGHT = 150.dp
    val NO_ELEVATION = 0.dp
    val TIME_CARD_CORNER_SIZE = 24.dp
    val TIME_TEXT_FONT_SIZE = 50.sp
    val ALARM_DAYS_TOP_PADDING = 12.dp
    val MIDDLE_CONTROL_SECTION_TOP_PADDING = 28.dp
    val TEST_BUTTON_FONT_SIZE = 14.sp
    val SAVE_BUTTON_FONT_SIZE = 14.sp
    val SAVE_BUTTON_TOP_PADDING = 12.dp
    val SETTINGS_CONTENT_MAX_WIDTH = 640.dp
    val TONE_PICKER_DIALOG_MAX_WIDTH = 360.dp
    val TONE_PICKER_DIALOG_PADDING = 12.dp
    val TONE_PICKER_DIALOG_ELEVATION = 6.dp
    val TONE_PICKER_HEADER_HEIGHT = 44.dp
    val TONE_PICKER_MAX_HEIGHT = 360.dp
    val TONE_PICKER_ROW_HEIGHT = 56.dp
    val TONE_PICKER_DIVIDER_START_PADDING = 16.dp
    val TONE_PICKER_SEPARATOR_THICKNESS = 1.dp
    const val TONE_PICKER_DIVIDER_ALPHA = 0.35f
}
