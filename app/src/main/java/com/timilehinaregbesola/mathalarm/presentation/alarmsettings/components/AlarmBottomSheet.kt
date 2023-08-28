package com.timilehinaregbesola.mathalarm.presentation.alarmsettings.components

import android.app.Activity
import android.media.RingtoneManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation.Vertical
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Arrangement.SpaceBetween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material.Button
import androidx.compose.material.ButtonDefaults.buttonColors
import androidx.compose.material.Card
import androidx.compose.material.Divider
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.Icon
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.EmojiSymbols
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.rememberBottomSheetScaffoldState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.State
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color.Companion.Black
import androidx.compose.ui.graphics.Color.Companion.LightGray
import androidx.compose.ui.graphics.Color.Companion.White
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight.Companion.Bold
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign.Companion.Center
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import com.timilehinaregbesola.mathalarm.R
import com.timilehinaregbesola.mathalarm.framework.database.AlarmEntity
import com.timilehinaregbesola.mathalarm.framework.database.AlarmMapper
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
import com.timilehinaregbesola.mathalarm.presentation.alarmsettings.components.AlarmBottomSheet.FROM_SHEET_KEY
import com.timilehinaregbesola.mathalarm.presentation.alarmsettings.components.AlarmBottomSheet.MIDDLE_CONTROL_SECTION_TOP_PADDING
import com.timilehinaregbesola.mathalarm.presentation.alarmsettings.components.AlarmBottomSheet.NO_ELEVATION
import com.timilehinaregbesola.mathalarm.presentation.alarmsettings.components.AlarmBottomSheet.SAVE_BUTTON_FONT_SIZE
import com.timilehinaregbesola.mathalarm.presentation.alarmsettings.components.AlarmBottomSheet.SAVE_BUTTON_TOP_PADDING
import com.timilehinaregbesola.mathalarm.presentation.alarmsettings.components.AlarmBottomSheet.TEST_BUTTON_FONT_SIZE
import com.timilehinaregbesola.mathalarm.presentation.alarmsettings.components.AlarmBottomSheet.TIME_CARD_CORNER_SIZE
import com.timilehinaregbesola.mathalarm.presentation.alarmsettings.components.AlarmBottomSheet.TIME_CARD_HEIGHT
import com.timilehinaregbesola.mathalarm.presentation.alarmsettings.components.AlarmBottomSheet.TIME_PATTERN
import com.timilehinaregbesola.mathalarm.presentation.alarmsettings.components.AlarmBottomSheet.TIME_TEXT_FONT_SIZE
import com.timilehinaregbesola.mathalarm.presentation.alarmsettings.components.AlarmBottomSheet.TIME_TEXT_PADDING
import com.timilehinaregbesola.mathalarm.presentation.alarmsettings.components.AlarmBottomSheet.URL_ENCODER
import com.timilehinaregbesola.mathalarm.presentation.ui.MathAlarmTheme
import com.timilehinaregbesola.mathalarm.presentation.ui.darkPrimary
import com.timilehinaregbesola.mathalarm.presentation.ui.darkPrimaryLight
import com.timilehinaregbesola.mathalarm.presentation.ui.spacing
import com.timilehinaregbesola.mathalarm.presentation.ui.unSelectedDay
import com.timilehinaregbesola.mathalarm.utils.Navigation.NAV_ALARM_MATH
import com.timilehinaregbesola.mathalarm.utils.Navigation.NAV_ALARM_MATH_ARGUMENT
import com.timilehinaregbesola.mathalarm.utils.PickRingtone
import com.timilehinaregbesola.mathalarm.utils.checkPermissions
import com.vanpra.composematerialdialogs.MaterialDialog
import com.vanpra.composematerialdialogs.datetime.time.TimePickerDefaults.colors
import com.vanpra.composematerialdialogs.datetime.time.timepicker
import kotlinx.coroutines.Dispatchers.IO
import kotlinx.coroutines.Dispatchers.Main
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import java.net.URLEncoder
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@ExperimentalMaterialApi
@Composable
fun AlarmBottomSheet(
    viewModel: AlarmSettingsViewModel = hiltViewModel(),
    navController: NavHostController,
    darkTheme: Boolean,
    alarm: AlarmEntity,
) {
    viewModel.setAlarm(AlarmMapper().mapToDomainModel(alarm))
    val scaffoldState = rememberBottomSheetScaffoldState()
    val activity = LocalContext.current as Activity
    var timeCal = LocalTime.now()

    LaunchedEffect(true) {
        viewModel.eventFlow.collectLatest { event ->
            when (event) {
                is AlarmSettingsViewModel.UiEvent.ShowSnackbar -> {
                    scaffoldState.snackbarHostState.showSnackbar(
                        message = event.message,
                    )
                }
                is AlarmSettingsViewModel.UiEvent.SaveAlarm -> {
                    navController.navigateUp()
                }
                is AlarmSettingsViewModel.UiEvent.TestAlarm -> {
                    navController
                        .previousBackStackEntry?.savedStateHandle?.set(FROM_SHEET_KEY, true)
                    // Nav to Math Screen
                    launch(IO) {
                        val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
                        val jsonAdapter = moshi.adapter(AlarmEntity::class.java).lenient()
                        val json = jsonAdapter.toJson(AlarmMapper().mapFromDomainModel(event.alarm))
                        val alarmJson = URLEncoder.encode(json, URL_ENCODER)
                        withContext(Main) {
                            navController.navigate(
                                NAV_ALARM_MATH.replace(
                                    "{$NAV_ALARM_MATH_ARGUMENT}",
                                    alarmJson,
                                ),
                            )
                        }
                    }
                }
            }
        }
    }
    with(MaterialTheme) {
        val alarmTimeText: State<TimeState> = viewModel.alarmTime
        val dialog = remember { MaterialDialog() }
        dialog.build(
            buttons = {
                positiveButton(
                    text = "Ok",
                    textStyle = TextStyle(color = colors.onPrimary),
                )
                negativeButton(
                    text = "Cancel",
                    textStyle = TextStyle(color = colors.onPrimary),
                )
            },
            backgroundColor = if (darkTheme) darkPrimary else LightGray,
        ) {
            with(alarmTimeText.value) {
                timeCal = timeCal.withHour(hour).withMinute(minute)
                timepicker(
                    initialTime = timeCal,
                    colors = colors(
                        activeBackgroundColor = if (darkTheme) darkPrimaryLight else White,
                    ),
                ) { time ->
                    val dtf = DateTimeFormatter.ofPattern(TIME_PATTERN)
                    viewModel.onEvent(
                        AddEditAlarmEvent.ChangeTime(
                            TimeState(
                                hour = time.hour,
                                minute = time.minute,
                                formattedTime = time.format(dtf).toString(),
                            ),
                        ),
                    )
                    timeCal = timeCal.withHour(hour).withMinute(minute)
                }
            }
        }
        Column(
            Modifier
                .fillMaxWidth()
                .padding(spacing.extraMedium)
                .scrollable(rememberScrollState(), Vertical),
        ) {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(TIME_CARD_HEIGHT)
                    .padding(horizontal = spacing.medium),
                backgroundColor = if (darkTheme) darkPrimaryLight else unSelectedDay,
                elevation = NO_ELEVATION,
                shape = shapes.medium.copy(CornerSize(TIME_CARD_CORNER_SIZE)),
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxSize()
                        .clickable(
                            onClick = { dialog.show() },
                        ),
                    verticalAlignment = CenterVertically,
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Text(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(TIME_TEXT_PADDING),
                        text = alarmTimeText.value.formattedTime,
                        fontSize = TIME_TEXT_FONT_SIZE,
                        fontWeight = Bold,
                        textAlign = Center,
                    )
                }
            }
            Spacer(modifier = Modifier.height(ALARM_DAYS_TOP_PADDING))
            AlarmDays(currentDays = viewModel.dayChooser.value) {
                viewModel.onEvent(
                    ToggleDayChooser(it),
                )
            }
            Divider(
                modifier = Modifier.padding(
                    top = spacing.medium,
                    start = spacing.medium,
                    end = spacing.medium,
                ),
                thickness = DIVIDER_THICKNESS,
                color = unSelectedDay,
            )
            Row(
                modifier = Modifier
                    .padding(
                        top = MIDDLE_CONTROL_SECTION_TOP_PADDING,
                        start = spacing.medium,
                        end = spacing.medium,
                    )
                    .fillMaxWidth(),
                horizontalArrangement = SpaceBetween,
            ) {
                TextWithCheckbox(
                    text = "Repeat Weekly",
                    initialState = viewModel.repeatWeekly.value,
                ) { viewModel.onEvent(ToggleRepeat(it)) }
                TextWithCheckbox(text = "Vibrate", initialState = viewModel.vibrate.value) {
                    viewModel.onEvent(ToggleVibrate(it))
                }
            }
            LabelTextField(
                text = viewModel.alarmTitle.value,
                onValueChange = { newValue ->
                    viewModel.onEvent(EnteredTitle(newValue))
                },
                label = { Text("Alarm title") },
                placeholder = { Text("Good day") },
            )
            val toneText = remember { mutableStateOf<String?>(null) }
            val result = remember { mutableStateOf<Uri?>(null) }
            val launcher = rememberLauncherForActivityResult(PickRingtone(viewModel.tone.value)) {
                result.value = it
            }
            result.value?.let {
                val alert = it.toString()
                checkPermissions(activity, listOf(alert))
                viewModel.onEvent(OnToneChange(alert))
                toneText.value = RingtoneManager.getRingtone(activity, alert.toUri()).getTitle(activity)
            }
            TextWithIcon(
                modifier = Modifier.padding(horizontal = spacing.medium),
                text = when {
                    toneText.value != null -> {
                        toneText.value!!
                    }
                    viewModel.tone.value == "" -> {
                        activity.getString(R.string.default_alarm_tone)
                    }
                    else -> {
                        RingtoneManager.getRingtone(activity, viewModel.tone.value.toUri()).getTitle(activity)
                    }
                },
                image = Icons.Outlined.Notifications,
                onClick = {
                    try {
                        launcher.launch(null)
                    } catch (e: Exception) {
                        Timber.e(e)
                        viewModel.onEvent(
                            OnToneError(
                                activity.getString(R.string.details_no_ringtone_picker),
                            ),
                        )
                    }
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
                DifficultyChooser(viewModel.difficulty.value) {
                    viewModel.onEvent(OnDifficultyChange(it))
                }
            }
            Button(
                modifier = Modifier
                    .padding(top = spacing.large)
                    .fillMaxWidth(),
                onClick = {
                    viewModel.onEvent(OnTestClick)
                },
                colors = buttonColors(
                    backgroundColor = unSelectedDay,
                    contentColor = Black,
                ),
            ) {
                Text(
                    fontSize = TEST_BUTTON_FONT_SIZE,
                    text = "TEST ALARM",
                )
            }
            Button(
                modifier = Modifier
                    .padding(top = SAVE_BUTTON_TOP_PADDING)
                    .fillMaxWidth(),
                onClick = {
                    viewModel.onEvent(OnSaveTodoClick)
                },
                colors = buttonColors(backgroundColor = colors.secondary),
            ) {
                Text(
                    fontSize = SAVE_BUTTON_FONT_SIZE,
                    text = "SAVE",
                )
            }
        }
    }
}

@Preview
@Composable
private fun TextCheckboxPreview() {
    MathAlarmTheme {
        TextWithCheckbox(
            text = "Repeat Weekly",
            initialState = false,
        ) { }
    }
}

@Preview
@Composable
private fun LabelTextViewPreview() {
    MathAlarmTheme {
        LabelTextField(text = TextFieldValue("")) {}
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
