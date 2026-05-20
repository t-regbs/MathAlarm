package com.timilehinaregbesola.mathalarm.presentation.alarmsettings.components

import androidx.compose.foundation.background
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
import androidx.compose.ui.graphics.Color.Companion.Black
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
import com.timilehinaregbesola.mathalarm.presentation.alarmsettings.AddEditAlarmEvent.OnDifficultyChange
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
import com.timilehinaregbesola.mathalarm.presentation.alarmsettings.components.AlarmBottomSheet.DIFFICULTY_ICON_END_PADDING
import com.timilehinaregbesola.mathalarm.presentation.alarmsettings.components.AlarmBottomSheet.DIFFICULTY_SECTION_HORIZONTAL_PADDING
import com.timilehinaregbesola.mathalarm.presentation.alarmsettings.components.AlarmBottomSheet.DIFFICULTY_SECTION_TOP_PADDING
import com.timilehinaregbesola.mathalarm.presentation.alarmsettings.components.AlarmBottomSheet.DIVIDER_THICKNESS
import com.timilehinaregbesola.mathalarm.presentation.alarmsettings.components.AlarmBottomSheet.MIDDLE_CONTROL_SECTION_TOP_PADDING
import com.timilehinaregbesola.mathalarm.presentation.alarmsettings.components.AlarmBottomSheet.NO_ELEVATION
import com.timilehinaregbesola.mathalarm.presentation.alarmsettings.components.AlarmBottomSheet.SAVE_BUTTON_FONT_SIZE
import com.timilehinaregbesola.mathalarm.presentation.alarmsettings.components.AlarmBottomSheet.SAVE_BUTTON_TOP_PADDING
import com.timilehinaregbesola.mathalarm.presentation.alarmsettings.components.AlarmBottomSheet.SETTINGS_CONTENT_MAX_WIDTH
import com.timilehinaregbesola.mathalarm.presentation.alarmsettings.components.AlarmBottomSheet.TEST_BUTTON_FONT_SIZE
import com.timilehinaregbesola.mathalarm.presentation.alarmsettings.components.AlarmBottomSheet.TIME_CARD_CORNER_SIZE
import com.timilehinaregbesola.mathalarm.presentation.alarmsettings.components.AlarmBottomSheet.TIME_CARD_HEIGHT
import com.timilehinaregbesola.mathalarm.presentation.alarmsettings.components.AlarmBottomSheet.TIME_TEXT_FONT_SIZE
import com.timilehinaregbesola.mathalarm.presentation.alarmsettings.components.AlarmBottomSheet.TIME_TEXT_PADDING
import com.timilehinaregbesola.mathalarm.presentation.alarmsettings.components.AlarmBottomSheet.TONE_PICKER_MAX_HEIGHT
import com.timilehinaregbesola.mathalarm.presentation.alarmsettings.components.AlarmBottomSheet.TONE_PICKER_ROW_HEIGHT
import com.timilehinaregbesola.mathalarm.presentation.alarmsettings.components.AlarmBottomSheet.TONE_PICKER_DIALOG_ELEVATION
import com.timilehinaregbesola.mathalarm.presentation.alarmsettings.components.AlarmBottomSheet.TONE_PICKER_DIALOG_MAX_WIDTH
import com.timilehinaregbesola.mathalarm.presentation.alarmsettings.components.AlarmBottomSheet.TONE_PICKER_DIALOG_PADDING
import com.timilehinaregbesola.mathalarm.presentation.alarmsettings.components.AlarmBottomSheet.TONE_PICKER_DIVIDER_ALPHA
import com.timilehinaregbesola.mathalarm.presentation.alarmsettings.components.AlarmBottomSheet.TONE_PICKER_DIVIDER_START_PADDING
import com.timilehinaregbesola.mathalarm.presentation.alarmsettings.components.AlarmBottomSheet.TONE_PICKER_HEADER_HEIGHT
import com.timilehinaregbesola.mathalarm.presentation.alarmsettings.components.AlarmBottomSheet.TONE_PICKER_SEPARATOR_THICKNESS
import com.timilehinaregbesola.mathalarm.presentation.ui.MathAlarmTheme
import com.timilehinaregbesola.mathalarm.presentation.ui.darkPrimaryLight
import com.timilehinaregbesola.mathalarm.presentation.ui.icon.Check
import com.timilehinaregbesola.mathalarm.presentation.ui.icon.Close
import com.timilehinaregbesola.mathalarm.presentation.ui.icon.EmojiSymbols
import com.timilehinaregbesola.mathalarm.presentation.ui.icon.Notifications
import com.timilehinaregbesola.mathalarm.presentation.ui.icon.PlayArrow
import com.timilehinaregbesola.mathalarm.presentation.ui.icon.Stop
import com.timilehinaregbesola.mathalarm.presentation.ui.spacing
import com.timilehinaregbesola.mathalarm.presentation.ui.unSelectedDay
import com.timilehinaregbesola.mathalarm.utils.Destinations.AlarmMath
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
) {
    LaunchedEffect(Unit) {
        viewModel.setAlarm(AlarmMapper().mapToDomainModel(alarm))
    }
    val scaffoldState = rememberBottomSheetScaffoldState()
    var showTimePickerDialog by remember { mutableStateOf(false) }
    var showTonePickerDialog by remember { mutableStateOf(false) }
    var showConfirmationDialog by remember { mutableStateOf(false) }
    var showPermRequiredDialog by remember { mutableStateOf(false) }

    val toneText = remember { mutableStateOf<String?>(null) }
    val closeSettings: () -> Unit = {
        if (backstack.size > 1) backstack.removeLastOrNull()
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
            toneText.value = getRingtoneTitle(alert)
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

    LaunchedEffect(true) {
        viewModel.eventFlow.collectLatest { event ->
            when (event) {
                is AlarmSettingsViewModel.UiEvent.ShowSnackbar -> {
                    scaffoldState.snackbarHostState.showSnackbar(
                        message = event.message,
                    )
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
        onCloseClick = closeSettings,
        showDismissButton = showDismissButton,
        topSection = {
            TopSection(
                selectedDays = viewModel.dayChooser.value,
                darkTheme = darkTheme,
                currentTime = viewModel.alarmTime.value.formattedTime,
                onTimeCardClick = { showTimePickerDialog = true },
                onSelectedDaysChanged = {
                    viewModel.onEvent(ToggleDayChooser(it))
                }
            )
        },
        bottomSection = {
            val noPickerText = strings.noRingtonePicker
            val defaultToneText = strings.defaultAlarmTone
            BottomSettingsSection(
                repeatWeekly = viewModel.repeatWeekly.value,
                snoozeEnabled = viewModel.snoozeEnabled.value,
                vibrate = viewModel.vibrate.value,
                difficulty = viewModel.difficulty.value,
                onRepeatToggle = {
                    viewModel.onEvent(ToggleRepeat(it))
                },
                onSnoozeToggle = {
                    viewModel.onEvent(ToggleSnooze(it))
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
                onDifficultyChange = {
                    viewModel.onEvent(OnDifficultyChange(it))
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
                currentTone = when {
                    toneText.value != null -> {
                        toneText.value!!
                    }

                    viewModel.tone.value == "" -> {
                        defaultToneText
                    }

                    else -> {
                        getRingtoneTitle(viewModel.tone.value)
                    }
                }
            )
        },
        onTestClick = {
            viewModel.onEvent(OnTestClick)
        },
        onSaveClick = {
            requestNotificationPermission()
        },
        dialogSection = {
            with(viewModel.alarmTime.value) {
                if (showTimePickerDialog) {
                    TimePickerDialog(
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
                        toneText.value = getRingtoneTitle(selectedTone)
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
    topSection: @Composable () -> Unit,
    bottomSection: @Composable () -> Unit,
    onTestClick: () -> Unit,
    onSaveClick: () -> Unit,
    dialogSection: @Composable () -> Unit
) {
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
                                bottomSection = bottomSection,
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
                            .then(sheetPaddingModifier)
                            .verticalScroll(rememberScrollState()),
                    ) {
                        if (showDismissButton) {
                            SheetHeader(onCloseClick = onCloseClick)
                        }
                        SheetSettingsContent(
                            topSection = topSection,
                            bottomSection = bottomSection,
                        )
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
                modifier = Modifier.size(32.dp),
                imageVector = Close,
                contentDescription = "Dismiss",
            )
        }
        if (showSaveAction && onSaveClick != null) {
            AdaptiveIconButton(onClick = onSaveClick) {
                Icon(
                    modifier = Modifier.size(32.dp),
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
        thickness = DIVIDER_THICKNESS,
        color = unSelectedDay
    )
    bottomSection()
}

@Composable
fun TopSection(
    selectedDays: String,
    currentTime: String,
    darkTheme: Boolean,
    onTimeCardClick: () -> Unit,
    onSelectedDaysChanged: (String) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .height(TIME_CARD_HEIGHT)
            .padding(horizontal = MaterialTheme.spacing.medium),
        colors = CardDefaults.cardColors(
            containerColor = if (darkTheme) darkPrimaryLight else unSelectedDay
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = NO_ELEVATION),
        shape = MaterialTheme.shapes.medium.copy(CornerSize(TIME_CARD_CORNER_SIZE)),
    ) {
        Row(
            modifier = Modifier
                .fillMaxSize()
                .adaptiveClickable(
                    shape = MaterialTheme.shapes.medium.copy(CornerSize(TIME_CARD_CORNER_SIZE)),
                    onClick = { onTimeCardClick() }
                ),
            verticalAlignment = CenterVertically,
            horizontalArrangement = Arrangement.Center,
        ) {
            Text(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(TIME_TEXT_PADDING),
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
    repeatWeekly: Boolean,
    snoozeEnabled: Boolean,
    vibrate: Boolean,
    difficulty: Int,
    onRepeatToggle: (Boolean) -> Unit,
    onSnoozeToggle: (Boolean) -> Unit,
    onVibrateToggle: (Boolean) -> Unit,
    onToneClick: () -> Unit,
    onDifficultyChange: (Int) -> Unit,
    labelTextField: @Composable () -> Unit,
    currentTone: String
) {
    val showVibrateToggle = !isIosPlatform()

    Row(
        modifier = Modifier
            .padding(
                top = MIDDLE_CONTROL_SECTION_TOP_PADDING,
                start = MaterialTheme.spacing.medium,
                end = MaterialTheme.spacing.medium,
            )
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
    Row(
        modifier = Modifier
            .padding(
                top = MaterialTheme.spacing.medium,
                start = MaterialTheme.spacing.medium,
                end = MaterialTheme.spacing.medium,
            )
            .fillMaxWidth(),
    ) {
        TextWithCheckbox(text = strings.snooze, initialState = snoozeEnabled) {
            onSnoozeToggle(it)
        }
    }
    labelTextField()
    TextWithIcon(
        modifier = Modifier.padding(horizontal = MaterialTheme.spacing.medium),
        text = currentTone,
        image = Notifications,
        onClick = {
            onToneClick()
        },
    )
    Row(
        modifier = Modifier
            .padding(
                top = DIFFICULTY_SECTION_TOP_PADDING,
                start = DIFFICULTY_SECTION_HORIZONTAL_PADDING,
                end = DIFFICULTY_SECTION_HORIZONTAL_PADDING,
            )
            .fillMaxWidth(),
    ) {
        Icon(
            modifier = Modifier.padding(end = DIFFICULTY_ICON_END_PADDING),
            imageVector = EmojiSymbols,
            contentDescription = null,
        )
        DifficultyChooser(difficulty) {
            onDifficultyChange(it)
        }
    }
}

@Composable
private fun SheetActionButtons(
    onTestClick: () -> Unit,
    onSaveClick: () -> Unit,
    showSaveButton: Boolean = true,
) {
    AdaptiveButton(
        modifier = Modifier
            .padding(top = MaterialTheme.spacing.large)
            .fillMaxWidth(),
        onClick = onTestClick,
        colors = buttonColors(
            containerColor = unSelectedDay,
            contentColor = Black,
        ),
    ) {
        Text(
            fontSize = TEST_BUTTON_FONT_SIZE,
            text = strings.testAlarm.uppercase(),
        )
    }
    if (showSaveButton) {
        AdaptiveButton(
            modifier = Modifier
                .padding(top = SAVE_BUTTON_TOP_PADDING)
                .fillMaxWidth(),
            onClick = onSaveClick,
            colors = buttonColors(containerColor = MaterialTheme.colorScheme.secondary),
        ) {
            Text(
                fontSize = SAVE_BUTTON_FONT_SIZE,
                text = strings.save.uppercase(),
            )
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
                    Spacer(modifier = Modifier.size(48.dp))
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
                    .size(22.dp),
                imageVector = Check,
                contentDescription = "Selected",
                tint = MaterialTheme.colorScheme.secondary,
            )
        } else {
            Spacer(
                modifier = Modifier
                    .padding(end = MaterialTheme.spacing.small)
                    .size(22.dp),
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
                topSection = {
                    TopSection(
                        selectedDays = "TFFFFFF",
                        currentTime = "12:00",
                        darkTheme = true,
                        onTimeCardClick = {}
                    ) {}
                },
                bottomSection = {
                    BottomSettingsSection(
                        repeatWeekly = true,
                        snoozeEnabled = true,
                        vibrate = true,
                        difficulty = 1,
                        onRepeatToggle = {},
                        onSnoozeToggle = {},
                        onVibrateToggle = {},
                        onToneClick = {},
                        onDifficultyChange = {},
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
    const val FROM_SHEET_KEY = "fromSheet"
    const val URL_ENCODER = "utf-8"
    const val TIME_PATTERN = "hh:mm a"
    val TIME_CARD_HEIGHT = 150.dp
    val NO_ELEVATION = 0.dp
    val TIME_CARD_CORNER_SIZE = 24.dp
    val TIME_TEXT_PADDING = 30.dp
    val TIME_TEXT_FONT_SIZE = 50.sp
    val ALARM_DAYS_TOP_PADDING = 12.dp
    val DIVIDER_THICKNESS = 10.dp
    val MIDDLE_CONTROL_SECTION_TOP_PADDING = 28.dp
    val DIFFICULTY_SECTION_TOP_PADDING = 30.dp
    val DIFFICULTY_SECTION_HORIZONTAL_PADDING = 26.dp
    val DIFFICULTY_ICON_END_PADDING = 14.dp
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
