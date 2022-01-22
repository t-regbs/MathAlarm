package com.timilehinaregbesola.mathalarm.presentation.alarmlist

import android.app.Activity
import android.media.RingtoneManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.Orientation
import androidx.compose.foundation.gestures.scrollable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.EmojiSymbols
import androidx.compose.material.icons.outlined.Label
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.net.toUri
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.timilehinaregbesola.mathalarm.R
import com.timilehinaregbesola.mathalarm.domain.model.Alarm
import com.timilehinaregbesola.mathalarm.presentation.alarmsettings.AddEditAlarmEvent
import com.timilehinaregbesola.mathalarm.presentation.alarmsettings.AlarmSettingsViewModel
import com.timilehinaregbesola.mathalarm.presentation.alarmsettings.TimeState
import com.timilehinaregbesola.mathalarm.presentation.components.RingDayChip
import com.timilehinaregbesola.mathalarm.presentation.ui.MathAlarmTheme
import com.timilehinaregbesola.mathalarm.presentation.ui.unSelectedDay
import com.timilehinaregbesola.mathalarm.utils.*
import com.vanpra.composematerialdialogs.MaterialDialog
import com.vanpra.composematerialdialogs.datetime.time.timepicker
import kotlinx.coroutines.flow.collectLatest
import timber.log.Timber
import java.time.LocalTime
import java.time.format.DateTimeFormatter

@ExperimentalMaterialApi
@Composable
fun AlarmBottomSheet(
    viewModel: AlarmSettingsViewModel = hiltViewModel(),
//    scaffoldState: BottomSheetScaffoldState,
    navController: NavHostController,
) {
    val scope = rememberCoroutineScope()
    var alarm: Alarm?
    val alarmTimeText: State<TimeState>
    val activity = LocalContext.current as Activity
    var timeCal = LocalTime.now()

    LaunchedEffect(key1 = true) {
        viewModel.eventFlow.collectLatest { event ->
            when (event) {
//                is AlarmSettingsViewModel.UiEvent.ShowSnackbar -> {
//                    scaffoldState.snackbarHostState.showSnackbar(
//                        message = event.message
//                    )
//                }
                is AlarmSettingsViewModel.UiEvent.SaveAlarm -> {
                    navController.navigateUp()
                }
            }
        }
    }

    val testScreenResult = navController.currentBackStackEntry?.savedStateHandle?.getLiveData<Long>("testAlarmId")?.observeAsState()
    testScreenResult?.value?.let {
//        viewModel.onDeleteWithId(it)
    }

    alarm = viewModel.alarm
    Timber.d("current alarm: $alarm")

    navController.currentBackStackEntry?.savedStateHandle?.getLiveData<String>("tone")?.observeAsState()?.value.let { alar ->
        alar?.let { alarm!!.alarmTone = it }
    }
    navController.currentBackStackEntry?.savedStateHandle?.getLiveData<Int>("hour")?.observeAsState()?.value.let { alar ->
        alar?.let { alarm!!.hour = it }
    }
    navController.currentBackStackEntry?.savedStateHandle?.getLiveData<Int>("minute")?.observeAsState()?.value.let { alar ->
        alar?.let { alarm!!.minute = it }
    }
    navController.currentBackStackEntry?.savedStateHandle?.getLiveData<Boolean>("vibrate")?.observeAsState()?.value.let { alar ->
        alar?.let { alarm!!.vibrate = it }
    }
    navController.currentBackStackEntry?.savedStateHandle?.getLiveData<Boolean>("repeat")?.observeAsState()?.value.let { alar ->
        alar?.let { alarm!!.repeat = it }
    }
    navController.currentBackStackEntry?.savedStateHandle?.getLiveData<String>("repeatDays")?.observeAsState()?.value.let { alar ->
        alar?.let { alarm!!.repeatDays = it }
    }

//    alarmText = remember { mutableStateOf(alarm?.getFormatTime().toString()) }
    alarmTimeText = viewModel.alarmTime
    val dialog = remember { MaterialDialog() }
    dialog.build(
        buttons = {
            positiveButton("Ok")
            negativeButton("Cancel")
        }
    ) {
        timeCal = alarm?.minute?.let { timeCal.withHour(alarm.hour).withMinute(it) }
        timepicker(initialTime = timeCal) { time ->
            alarm?.hour = time.hour
            alarm?.minute = time.minute
//            alarmText.value = alarm?.getFormatTime().toString()
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
            timeCal = alarm?.hour?.let { timeCal.withHour(it).withMinute(alarm.minute) }
        }
    }

    Column(
        Modifier
            .background(color = Color.White)
            .fillMaxWidth()
            .padding(24.dp)
            .scrollable(rememberScrollState(), Orientation.Vertical)
    ) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .height(150.dp)
                .padding(horizontal = 16.dp),
            backgroundColor = unSelectedDay,
            elevation = 0.dp,
            shape = MaterialTheme.shapes.medium.copy(CornerSize(24.dp))
        ) {
            Row(
                modifier = Modifier
                    .fillMaxSize()
                    .clickable(
                        onClick = {
                            dialog.show()
                        },
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
            modifier = Modifier
                .padding(top = 17.dp, start = 16.dp, end = 16.dp),
            thickness = 10.dp,
            color = unSelectedDay
        )
        Row(
            modifier = Modifier
                .padding(top = 28.dp, start = 16.dp, end = 16.dp)
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
            }
        )
        val toneText = remember { mutableStateOf<String?>(null) }
        val result = remember { mutableStateOf<Uri?>(null) }
        val launcher = rememberLauncherForActivityResult(PickRingtone(alarm)) {
            result.value = it
        }
        result.value?.let {
            val alert = it.toString()
            checkPermissions(activity, listOf(alert))
            viewModel.onEvent(AddEditAlarmEvent.OnToneChange(alert))
            toneText.value = RingtoneManager.getRingtone(activity, alert.toUri()).getTitle(activity)
        }
        TextWithIcon(
            modifier = Modifier.padding(horizontal = 16.dp),
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
//                                Toast.makeText(context, requireContext().getString(R.string.details_no_ringtone_picker), Toast.LENGTH_LONG)
//                                    .show()
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
            Difficulty(viewModel.difficulty.value) {
                viewModel.onEvent(AddEditAlarmEvent.OnDifficultyChange(it))
            }
        }
        Button(
            modifier = Modifier
                .padding(top = 32.dp)
                .fillMaxWidth(),
            onClick = {
                updateSavedStateHandle(navController, alarm)
                val testAlarm = Alarm()
                testAlarm.apply {
                    difficulty = alarm!!.difficulty
                    alarmTone = alarm.alarmTone.ifBlank {
                        RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM).toString()
                    }
                    snooze = 0
                    vibrate = alarm.vibrate
                }
//                val id = viewModel.onAddTestAlarm(testAlarm)
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
                navController.currentBackStackEntry?.savedStateHandle?.apply {
                    remove<String>("tone")
                    remove<Int>("hour")
                    remove<Int>("minute")
                    remove<Int>("difficulty")
                    remove<Boolean>("vibrate")
                    remove<Boolean>("repeat")
                    remove<String>("repeatDays")
                }
//                scope.launch {
//                    alarm!!.title = txtFieldText.text
//                    if (state is SheetState.NewAlarm) {
//                        if (alarm!!.alarmTone == "") {
//                            alarm!!.alarmTone =
//                                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM).toString()
//                        }
// //                        alarm!!.isOn = true
//                        val id = viewModel.onAddTestAlarm(alarm!!)
//                        alarm!!.alarmId = id
// //                        alarm!!.scheduleAlarm(activity, alarm!!.repeat)
//                        viewModel.scheduleAlarm(alarm!!, alarm!!.repeat)
//                    } else {
//                        viewModel.onUpdate(alarm!!)
//                        if (alarm!!.isOn) {
//                            alarm!!.cancelAlarm(activity)
//                        }
// //                        alarm!!.isOn = alarm!!.scheduleAlarm(activity, alarm!!.repeat)
//                        viewModel.scheduleAlarm(alarm!!, alarm!!.repeat)
//                        Timber.d("Save alarm with id: ${alarm!!.alarmId}")
//                    }
//                    alarm = null
//                    viewModel.onSheetClose()
// //                    scaffoldState.snackbarHostState.showSnackbar(
// //                        message = alarm!!.getTimeLeftMessage(activity)!!,
// //                        duration = SnackbarDuration.Short
// //                    )
// //                    scaffoldState.bottomSheetState.collapse()
//                }
                viewModel.onEvent(AddEditAlarmEvent.OnSaveTodoClick)
            }
        ) {
            Text(
                fontSize = 14.sp,
                text = "SAVE"
            )
        }
    }
}

private fun updateSavedStateHandle(
    navController: NavHostController,
    alarm: Alarm?
) {
    navController.currentBackStackEntry?.savedStateHandle?.apply {
        set("tone", alarm!!.alarmTone)
        set("hour", alarm!!.hour)
        set("minute", alarm!!.minute)
        set("difficulty", alarm!!.difficulty)
        set("vibrate", alarm!!.vibrate)
        set("repeat", alarm!!.repeat)
        set("repeatDays", alarm!!.repeatDays)
    }
}

@Composable
private fun TextWithIcon(
    image: ImageVector,
    text: String,
    modifier: Modifier = Modifier,
    onClick: (() -> Unit)? = null
) {
    Row(
        modifier = modifier
            .padding(top = 30.dp, start = 10.dp, end = 10.dp)
            .fillMaxWidth()
    ) {
        Icon(
            modifier = Modifier.padding(end = 14.dp),
            imageVector = image,
            contentDescription = null
        )
        Text(
            modifier = Modifier.clickable { onClick?.invoke() },
            text = text,
            fontSize = 16.sp,
            fontWeight = FontWeight.Normal
        )
    }
}

@Composable
private fun TextWithCheckbox(
    modifier: Modifier = Modifier,
    text: String,
    initialState: Boolean,
    onCheckChange: (Boolean) -> Unit
) {
    Row(
        modifier = modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            modifier = Modifier.padding(end = 14.dp),
            checked = initialState,
            onCheckedChange = {
                onCheckChange(it)
            }
        )
        Text(
            text = text,
            fontSize = 16.sp,
            fontWeight = FontWeight.Normal
        )
    }
}

@Composable
private fun LabelTextField(
    text: TextFieldValue,
    onValueChange: (TextFieldValue) -> Unit
) {
    TextField(
        value = text,
        onValueChange = onValueChange,
        leadingIcon = { Icon(imageVector = Icons.Outlined.Label, contentDescription = null) },
        modifier = Modifier
            .padding(horizontal = 16.dp)
            .fillMaxWidth(),
        label = { Text("Alarm title") },
        placeholder = { Text("Good day") },
        singleLine = true,
        colors = TextFieldDefaults.textFieldColors(backgroundColor = Color.Transparent),
    )
}

@Composable
private fun AlarmDays(
    currentDays: String,
    onValueChange: (String) -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        days.forEachIndexed { index, day ->
            val sb = StringBuilder(currentDays)
            val sel = currentDays[index] == 'T'
            val checkedState = mutableStateOf(sel)
            RingDayChip(
                day = day,
                selected = checkedState.value,
                onSelectChange = {
                    checkedState.value = it
                    if (it) {
                        sb.setCharAt(index, 'T')
                        onValueChange(sb.toString())
                    } else {
                        sb.setCharAt(index, 'F')
                        onValueChange(sb.toString())
                    }
                }
            )
        }
    }
}

@Composable
fun Difficulty(initialDiff: Int, onValueChange: (Int) -> Unit) {
    val expanded = remember { mutableStateOf(false) }
    val items = listOf("Easy Math", "Medium Math", "Hard Math")
    Box {
        Text(
            items[initialDiff],
            modifier = Modifier
                .clickable(onClick = { expanded.value = true })
                .background(unSelectedDay)
        )
        DropdownMenu(
            expanded = expanded.value,
            onDismissRequest = { expanded.value = false }
        ) {
            items.forEachIndexed { index, s ->
                DropdownMenuItem(
                    onClick = {
                        expanded.value = false
                        onValueChange(index)
                    }
                ) {
                    Text(text = s)
                }
            }
        }
    }
}

sealed class SheetState(val alarmId: Long = 0L) {
    class NewAlarm() : SheetState()

    object Init : SheetState()

    class EditAlarm(id: Long) : SheetState(id)
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
