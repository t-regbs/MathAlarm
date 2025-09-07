package com.timilehinaregbesola.mathalarm.presentation.alarmsettings.components

import android.app.Activity
import android.media.RingtoneManager
import android.net.Uri
import androidx.activity.compose.LocalActivity
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation.Vertical
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Arrangement.SpaceBetween
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.EmojiSymbols
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Button
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
import androidx.compose.material3.rememberTimePickerState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color.Companion.Black
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight.Companion.Bold
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign.Companion.Center
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.NotificationManagerCompat
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation3.runtime.NavBackStack
import cafe.adriel.lyricist.strings
import com.timilehinaregbesola.mathalarm.framework.database.AlarmEntity
import com.timilehinaregbesola.mathalarm.framework.database.AlarmMapper
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
import com.timilehinaregbesola.mathalarm.presentation.alarmsettings.components.AlarmBottomSheet.TEST_BUTTON_FONT_SIZE
import com.timilehinaregbesola.mathalarm.presentation.alarmsettings.components.AlarmBottomSheet.TIME_CARD_CORNER_SIZE
import com.timilehinaregbesola.mathalarm.presentation.alarmsettings.components.AlarmBottomSheet.TIME_CARD_HEIGHT
import com.timilehinaregbesola.mathalarm.presentation.alarmsettings.components.AlarmBottomSheet.TIME_TEXT_FONT_SIZE
import com.timilehinaregbesola.mathalarm.presentation.alarmsettings.components.AlarmBottomSheet.TIME_TEXT_PADDING
import com.timilehinaregbesola.mathalarm.presentation.ui.MathAlarmTheme
import com.timilehinaregbesola.mathalarm.presentation.ui.darkPrimaryLight
import com.timilehinaregbesola.mathalarm.presentation.ui.spacing
import com.timilehinaregbesola.mathalarm.presentation.ui.unSelectedDay
import com.timilehinaregbesola.mathalarm.utils.Destinations.AlarmMath
import com.timilehinaregbesola.mathalarm.utils.PickRingtone
import com.timilehinaregbesola.mathalarm.utils.checkPermissions
import com.timilehinaregbesola.mathalarm.utils.handleNotificationPermission
import com.timilehinaregbesola.mathalarm.utils.openNotificationSettings
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.datetime.LocalTime
import kotlinx.datetime.format
import kotlinx.datetime.format.char
import kotlinx.serialization.json.Json
import timber.log.Timber

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AlarmBottomSheet(
    viewModel: AlarmSettingsViewModel = hiltViewModel(),
    backstack: NavBackStack,
    darkTheme: Boolean,
    alarm: AlarmEntity,
) {
    LaunchedEffect(Unit) {
        viewModel.setAlarm(AlarmMapper().mapToDomainModel(alarm))
    }
    val scaffoldState = rememberBottomSheetScaffoldState()
    var showTimePickerDialog by remember { mutableStateOf(false) }
    var showConfirmationDialog by remember { mutableStateOf(false) }
    var showPermRequiredDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val activity = LocalActivity.current

    val toneText = remember { mutableStateOf<String?>(null) }
    val result = remember { mutableStateOf<Uri?>(null) }
    val pickToneLauncher =
        rememberLauncherForActivityResult(PickRingtone(viewModel.tone.value)) {
            result.value = it
        }
    result.value?.let {
        val alert = it.toString()
        checkPermissions(
            activity = context as Activity,
            tones = listOf(alert),
            unplayableDialogTitle = strings.alert,
            unplayableDialogMessage = strings.permissionsExternalStorageText,
        )
        viewModel.onEvent(OnToneChange(alert))
        toneText.value =
            RingtoneManager.getRingtone(context, alert.toUri()).getTitle(context)
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
                    backstack.removeLastOrNull()
                }
                is AlarmSettingsViewModel.UiEvent.TestAlarm -> {
                    launch(IO) {
                        val alarmEntity = AlarmMapper().mapFromDomainModel(event.alarm)
                        val json = Json.encodeToString(alarmEntity)
                        withContext(Main) {
                            backstack.add(AlarmMath(alarmJson = json, fromSheet = true))
                        }
                    }
                }
            }
        }
    }
    AlarmBottomSheetContent(
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
                vibrate = viewModel.vibrate.value,
                difficulty = viewModel.difficulty.value,
                onRepeatToggle = {
                    viewModel.onEvent(ToggleRepeat(it))
                },
                onVibrateToggle = {
                    viewModel.onEvent(ToggleVibrate(it))
                },
                onToneClick = {
                    try {
                        pickToneLauncher.launch(null)
                    } catch (e: Exception) {
                        Timber.e(e)
                        viewModel.onEvent(
                            OnToneError(message = noPickerText)
                        )
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
                        RingtoneManager.getRingtone(context, viewModel.tone.value.toUri())
                            .getTitle(context)
                    }
                }
            )
        },
        buttonSection = {
            SheetActionButtons(
                onTestClick = {
                    viewModel.onEvent(OnTestClick)
                },
                onSaveClick = {
                    activity?.let {
                        handleNotificationPermission(context = activity) {
                            if (it) {
                                if (NotificationManagerCompat.from(context)
                                        .areNotificationsEnabled()
                                ) {
                                    viewModel.onEvent(OnSaveTodoClick)
                                } else {
                                    showConfirmationDialog = true
                                }
                            } else {
                                showPermRequiredDialog = true
                            }
                        }
                    }
                }
            )
        },
        dialogSection = {
            with(viewModel.alarmTime.value) {
                if (showTimePickerDialog) {
                    TimePickerDialog(
                        timeState = rememberTimePickerState(
                            initialHour = hour,
                            initialMinute = minute
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
                        context.openNotificationSettings()
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
    topSection: @Composable () -> Unit,
    bottomSection: @Composable () -> Unit,
    buttonSection: @Composable () -> Unit,
    dialogSection: @Composable () -> Unit
) {
    with(MaterialTheme) {
        Surface {
            Box {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .padding(spacing.extraMedium)
                        .scrollable(rememberScrollState(), Vertical),
                ) {
                    topSection()
                    HorizontalDivider(
                        modifier = Modifier.padding(
                            top = spacing.medium,
                            start = spacing.medium,
                            end = spacing.medium,
                        ),
                        thickness = DIVIDER_THICKNESS,
                        color = unSelectedDay
                    )
                    bottomSection()
                    buttonSection()
                }
                dialogSection()
            }
        }
    }
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
                .clickable(
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
    vibrate: Boolean,
    difficulty: Int,
    onRepeatToggle: (Boolean) -> Unit,
    onVibrateToggle: (Boolean) -> Unit,
    onToneClick: () -> Unit,
    onDifficultyChange: (Int) -> Unit,
    labelTextField: @Composable () -> Unit,
    currentTone: String
) {
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
        TextWithCheckbox(text = strings.vibrate, initialState = vibrate) {
            onVibrateToggle(it)
        }
    }
    labelTextField()
    TextWithIcon(
        modifier = Modifier.padding(horizontal = MaterialTheme.spacing.medium),
        text = currentTone,
        image = Icons.Outlined.Notifications,
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
            imageVector = Icons.Outlined.EmojiSymbols,
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
    onSaveClick: () -> Unit
) {
    Button(
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
    Button(
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

@Preview
@Composable
private fun BottomSheetPreview() {
    MathAlarmTheme(darkTheme = true) {
        Surface {
            AlarmBottomSheetContent(
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
                        vibrate = true,
                        difficulty = 1,
                        onRepeatToggle = {},
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
                buttonSection = {
                    SheetActionButtons(
                        onTestClick = {},
                        onSaveClick = {}
                    )
                }) {}
        }
    }
}

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
}
