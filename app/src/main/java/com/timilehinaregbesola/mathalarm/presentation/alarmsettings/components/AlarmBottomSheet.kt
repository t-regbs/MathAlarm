package com.timilehinaregbesola.mathalarm.presentation.alarmsettings.components

import android.app.Activity
import android.media.RingtoneManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.EmojiSymbols
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
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
import com.timilehinaregbesola.mathalarm.presentation.alarmsettings.AlarmSettingsViewModel
import com.timilehinaregbesola.mathalarm.presentation.alarmsettings.TimeState
import com.timilehinaregbesola.mathalarm.presentation.ui.*
import com.timilehinaregbesola.mathalarm.utils.Navigation
import com.timilehinaregbesola.mathalarm.utils.PickRingtone
import com.timilehinaregbesola.mathalarm.utils.checkPermissions
import com.vanpra.composematerialdialogs.MaterialDialog
import com.vanpra.composematerialdialogs.datetime.time.TimePickerDefaults
import com.vanpra.composematerialdialogs.datetime.time.timepicker
import kotlinx.coroutines.flow.collectLatest
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
    alarm: AlarmEntity
) {
    viewModel.setAlarm(AlarmMapper().mapToDomainModel(alarm))
    val scaffoldState = rememberBottomSheetScaffoldState()
    val activity = LocalContext.current as Activity
    var timeCal = LocalTime.now()

    LaunchedEffect(key1 = true) {
        viewModel.eventFlow.collectLatest { event ->
            when (event) {
                is AlarmSettingsViewModel.UiEvent.ShowSnackbar -> {
                    scaffoldState.snackbarHostState.showSnackbar(
                        message = event.message
                    )
                }
                is AlarmSettingsViewModel.UiEvent.SaveAlarm -> {
                    navController.navigateUp()
                }
                is AlarmSettingsViewModel.UiEvent.TestAlarm -> {
                    // Nav to Math Screen
                    val moshi = Moshi.Builder().addLast(KotlinJsonAdapterFactory()).build()
                    val jsonAdapter = moshi.adapter(AlarmEntity::class.java).lenient()
                    val json = jsonAdapter.toJson(AlarmMapper().mapFromDomainModel(event.alarm))
                    val alarmJson = URLEncoder.encode(json, "utf-8")
//                    navigator.navigate(AlarmBottomSheetDestination(alarm = AlarmMapper().mapFromDomainModel(event.alarm)))
                    navController.navigate(Navigation.NAV_ALARM_MATH.replace("{${Navigation.NAV_ALARM_MATH_ARGUMENT}}", alarmJson))
                }
            }
        }
    }

    val alarmTimeText: State<TimeState> = viewModel.alarmTime
    val dialog = remember { MaterialDialog() }
    dialog.build(
        buttons = {
            positiveButton(
                text = "Ok",
                textStyle = TextStyle(color = MaterialTheme.colors.onPrimary)
            )
            negativeButton(
                text = "Cancel",
                textStyle = TextStyle(color = MaterialTheme.colors.onPrimary)
            )
        },
        backgroundColor = if (darkTheme) darkPrimary else Color.LightGray
    ) {
        timeCal = timeCal.withHour(alarmTimeText.value.hour).withMinute(alarmTimeText.value.minute)
        timepicker(
            initialTime = timeCal,
            colors = TimePickerDefaults.colors(
                activeBackgroundColor = if (darkTheme) darkPrimaryLight else Color.White
            )
        ) { time ->
            val dtf = DateTimeFormatter.ofPattern("hh:mm a")
            viewModel.onEvent(
                AddEditAlarmEvent.ChangeTime(
                    TimeState(
                        hour = time.hour,
                        minute = time.minute,
                        formattedTime = time.format(dtf).toString()
                    )
                )
            )
            timeCal = timeCal.withHour(alarmTimeText.value.hour).withMinute(alarmTimeText.value.minute)
        }
    }

    Column(
        Modifier
            .fillMaxWidth()
            .padding(MaterialTheme.spacing.extraMedium)
            .scrollable(rememberScrollState(), Orientation.Vertical)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .padding(horizontal = MaterialTheme.spacing.medium),
            backgroundColor = if (darkTheme) darkPrimaryLight else unSelectedDay,
            elevation = 0.dp,
            shape = MaterialTheme.shapes.medium.copy(CornerSize(24.dp))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        onClick = { dialog.show() },
                    ),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(30.dp),
                    text = alarmTimeText.value.formattedTime,
                    fontSize = 50.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        AlarmDays(currentDays = viewModel.dayChooser.value) {
            viewModel.onEvent(
                AddEditAlarmEvent.ToggleDayChooser(it)
            )
        }
        Divider(
            modifier = Modifier.padding(
                top = MaterialTheme.spacing.medium,
                start = MaterialTheme.spacing.medium,
                end = MaterialTheme.spacing.medium
            ),
            thickness = 10.dp,
            color = unSelectedDay
        )
        Row(
            modifier = Modifier
                .padding(
                    top = 28.dp,
                    start = MaterialTheme.spacing.medium,
                    end = MaterialTheme.spacing.medium
                )
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            TextWithCheckbox(
                text = "Repeat Weekly",
                initialState = viewModel.repeatWeekly.value
            ) { viewModel.onEvent(AddEditAlarmEvent.ToggleRepeat(it)) }
            TextWithCheckbox(text = "Vibrate", initialState = viewModel.vibrate.value) {
                viewModel.onEvent(AddEditAlarmEvent.ToggleVibrate(it))
            }
        }
        LabelTextField(
            text = viewModel.alarmTitle.value,
            onValueChange = { newValue ->
                viewModel.onEvent(AddEditAlarmEvent.EnteredTitle(newValue))
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
            viewModel.onEvent(AddEditAlarmEvent.OnToneChange(alert))
            toneText.value = RingtoneManager.getRingtone(activity, alert.toUri()).getTitle(activity)
        }
        TextWithIcon(
            modifier = Modifier.padding(horizontal = MaterialTheme.spacing.medium),
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
                        AddEditAlarmEvent.OnToneError(
                            activity.getString(R.string.details_no_ringtone_picker)
                        )
                    )
                }
            }
        )
        Row(
            modifier = Modifier
                .padding(top = 30.dp, start = 26.dp, end = 26.dp)
                .fillMaxWidth()
        ) {
            Icon(
                modifier = Modifier.padding(end = 14.dp),
                imageVector = Icons.Outlined.EmojiSymbols,
                contentDescription = null
            )
            DifficultyChooser(viewModel.difficulty.value) {
                viewModel.onEvent(AddEditAlarmEvent.OnDifficultyChange(it))
            }
        }
        Button(
            modifier = Modifier
                .padding(top = MaterialTheme.spacing.large)
                .fillMaxWidth(),
            onClick = {
                viewModel.onEvent(AddEditAlarmEvent.OnTestClick)
//                navController.navigate(Navigation.buildAlarmMathPath(alarmId = id))
            },
            colors = ButtonDefaults.buttonColors(
                backgroundColor = unSelectedDay,
                contentColor = Color.Black
            )
        ) {
            Text(
                fontSize = 14.sp,
                text = "TEST ALARM"
            )
        }
        Button(
            modifier = Modifier
                .padding(top = 12.dp)
                .fillMaxWidth(),
            onClick = {
                viewModel.onEvent(AddEditAlarmEvent.OnSaveTodoClick)
            },
            colors = ButtonDefaults.buttonColors(backgroundColor = MaterialTheme.colors.secondary)
        ) {
            Text(
                fontSize = 14.sp,
                text = "SAVE"
            )
        }
    }
}

@Preview
@Composable
fun TextCheckboxPreview() {
    MathAlarmTheme {
        TextWithCheckbox(
            text = "Repeat Weekly",
            initialState = false
        ) { }
    }
}

@Preview
@Composable
fun LabelTextViewPreview() {
    MathAlarmTheme {
        LabelTextField(text = TextFieldValue(""), {})
    }
}
