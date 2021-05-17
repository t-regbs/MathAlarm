package com.timilehinaregbesola.mathalarm.presentation.alarmlist

import android.app.Activity
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.material.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.EmojiSymbols
import androidx.compose.material.icons.outlined.Label
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.app.ActivityCompat.startActivityForResult
import androidx.core.net.toUri
import com.timilehinaregbesola.mathalarm.R
import com.timilehinaregbesola.mathalarm.domain.model.Alarm
import com.timilehinaregbesola.mathalarm.presentation.components.RingDayChip
import com.timilehinaregbesola.mathalarm.presentation.ui.unSelectedDay
import com.timilehinaregbesola.mathalarm.utils.*
import com.vanpra.composematerialdialogs.MaterialDialog
import com.vanpra.composematerialdialogs.buttons
import com.vanpra.composematerialdialogs.datetime.timepicker.timepicker
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch
import timber.log.Timber
import java.time.LocalTime
import java.util.*

@ExperimentalMaterialApi
@Composable
fun AlarmBottomSheet(
    state: SheetState,
    viewModel: AlarmListViewModel,
    scope: CoroutineScope,
    scaffoldState: BottomSheetScaffoldState
) {
    var alarm: Alarm?
    val alarmText: MutableState<String>
    val activity = LocalContext.current as Activity
    var timeCal = LocalTime.now()
    when (state) {
        is SheetState.EditAlarm -> {
            alarm = remember { viewModel.retrieveAlarm(state.alarmId) }
            Timber.d("current alarm: $alarm")
            alarmText = remember { mutableStateOf(alarm!!.getFormatTime().toString()) }
        }
        is SheetState.NewAlarm -> {
            alarm = remember { Alarm() }
            val sb = StringBuilder("FFFFFFF")
            val cal = initCalendar(alarm)
            val dayOfTheWeek =
                getDayOfWeek(cal[Calendar.DAY_OF_WEEK])
            sb.setCharAt(dayOfTheWeek, 'T')
            alarm.repeatDays = sb.toString()
            Timber.d("new alarm: $alarm")
            alarmText = remember { mutableStateOf(alarm!!.getFormatTime().toString()) }
        }
        else -> {
            alarm = remember { Alarm() }
            alarmText = remember { mutableStateOf(alarm!!.getFormatTime().toString()) }
            Timber.d("illegal state")
        }
    }
    val dialog = remember { MaterialDialog() }
    dialog.build {
        timeCal = timeCal.withHour(alarm!!.hour).withMinute(alarm!!.minute)
        timepicker(initialTime = timeCal) { time ->
            alarm!!.hour = time.hour
            alarm!!.minute = time.minute
            alarmText.value = alarm!!.getFormatTime().toString()
            timeCal = timeCal.withHour(alarm!!.hour).withMinute(alarm!!.minute)
        }
        buttons {
            positiveButton("Ok")
            negativeButton("Cancel")
        }
    }

    Column(
        Modifier
            .background(color = Color.White)
            .fillMaxWidth()
            .padding(24.dp)
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
                    text = alarmText.value,
                    fontSize = 50.sp,
                    fontWeight = FontWeight.Bold,
                    textAlign = TextAlign.Center
                )
            }
        }
        Spacer(modifier = Modifier.height(12.dp))
        AlarmDays(alarm = alarm)
        Divider(
            modifier = Modifier
                .padding(top = 17.dp, start = 16.dp, end = 16.dp),
            thickness = 10.dp,
            color = unSelectedDay
        )
        Row(
            modifier = Modifier
                .padding(top = 38.dp, start = 16.dp, end = 16.dp)
                .fillMaxWidth()
        ) {
            Row(modifier = Modifier.padding(end = 38.dp)) {
                val repeatWeekCheckboxState = remember { mutableStateOf(true) }
                Checkbox(
                    modifier = Modifier.padding(end = 14.dp),
                    checked = repeatWeekCheckboxState.value,
                    onCheckedChange = { repeatWeekCheckboxState.value = it }
                )
                Text(
                    text = "Repeat Weekly",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Normal
                )
            }
            Row {
                val vibrateCheckboxState = remember { mutableStateOf(true) }
                Checkbox(
                    modifier = Modifier.padding(end = 14.dp),
                    checked = vibrateCheckboxState.value,
                    onCheckedChange = { vibrateCheckboxState.value = it }
                )
                Text(
                    text = "Vibrate",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Normal
                )
            }
        }
        Row(
            modifier = Modifier
                .padding(top = 30.dp, start = 16.dp, end = 16.dp)
                .fillMaxWidth()
        ) {
            Icon(
                modifier = Modifier.padding(end = 14.dp),
                imageVector = Icons.Outlined.Label,
                contentDescription = null
            )
            Text(
                text = "Good Morning",
                fontSize = 16.sp,
                fontWeight = FontWeight.Normal
            )
        }
        Row(
            modifier = Modifier
                .padding(top = 30.dp, start = 16.dp, end = 16.dp)
                .fillMaxWidth()
        ) {
            val toneText = remember { mutableStateOf<String?>(null) }
            val result = remember { mutableStateOf<Uri?>(null) }
            val launcher = rememberLauncherForActivityResult(PickRingtone(alarm)) {
                result.value = it
            }
            result.value?.let {
                val alert = it.toString()
                checkPermissions(activity, listOf(alert))
                alarm!!.alarmTone = alert
                toneText.value = RingtoneManager.getRingtone(activity, alert.toUri()).getTitle(activity)
            }
            Icon(
                modifier = Modifier.padding(end = 14.dp),
                imageVector = Icons.Outlined.Notifications,
                contentDescription = null
            )
            Text(
                modifier = Modifier
                    .clickable(
                        onClick = {
                            try {
                                launcher.launch(null)
                            } catch (e: Exception) {
                                Timber.e(e)
//                                Toast.makeText(context, requireContext().getString(R.string.details_no_ringtone_picker), Toast.LENGTH_LONG)
//                                    .show()
                            }
                        }
                    ),
                text = when {
                    toneText.value != null -> {
                        toneText.value!!
                    }
                    alarm?.alarmTone == "" -> {
                        activity.getString(R.string.default_alarm_tone)
                    }
                    else -> {
                        RingtoneManager.getRingtone(activity, alarm?.alarmTone?.toUri()).getTitle(activity)
                    }
                },
                fontSize = 16.sp,
                fontWeight = FontWeight.Normal
            )
        }
        Row(
            modifier = Modifier
                .padding(top = 30.dp, start = 16.dp, end = 16.dp)
                .fillMaxWidth()
        ) {
            Icon(
                modifier = Modifier.padding(end = 14.dp),
                imageVector = Icons.Outlined.EmojiSymbols,
                contentDescription = null
            )
            alarm?.let { Difficulty(it) }
        }
        Button(
            modifier = Modifier
                .padding(top = 32.dp)
                .fillMaxWidth(),
            onClick = {
                if (alarm!! != null) {
//                    navController.navigate(Navigation.buildAlarmMathPath(alarmId = alarm.alarmId))
                }
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
                viewModel.getAlarms()
                scope.launch {
                    if (state is SheetState.NewAlarm) {
                        if (alarm!!.alarmTone == "") alarm!!.alarmTone = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM).toString()
                        viewModel.onAdd(alarm!!)
                    } else {
                        viewModel.onUpdate(alarm!!)
                        Timber.d("Save alarm with id: ${alarm!!.alarmId}")
                    }
                    // TODO: Schedule Alarm
                    alarm = null
                    viewModel.onSheetClose()
                    scaffoldState.bottomSheetState.collapse()
                }
            }
        ) {
            Text(
                fontSize = 14.sp,
                text = "SAVE"
            )
        }
    }
}

private fun pickRingtone(
    activity: Activity,
    alarm: Alarm?
) {
    startActivityForResult(
        activity,
        Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
            putExtra(RingtoneManager.EXTRA_RINGTONE_EXISTING_URI, alarm!!.alarmTone)

            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
            putExtra(
                RingtoneManager.EXTRA_RINGTONE_DEFAULT_URI,
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            )

            putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true)
            putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
        },
        42, null
    )
}

@Composable
private fun AlarmDays(alarm: Alarm?) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        days.forEachIndexed { index, day ->
            if (alarm != null) {
                val sb = StringBuilder(alarm.repeatDays)
                val sel = alarm.repeatDays[index] == 'T'
                val checkedState = remember { mutableStateOf(sel) }
                RingDayChip(
                    day = day,
                    selected = checkedState.value,
                    onSelectChange = {
                        checkedState.value = it
                        if (it) {
                            sb.setCharAt(index, 'T')
                            alarm.repeatDays = sb.toString()
                        } else {
                            sb.setCharAt(index, 'F')
                            alarm.repeatDays = sb.toString()
                        }
                    }
                )
            }
        }
    }
}

@Composable
fun Difficulty(alarm: Alarm) {
    val expanded = remember { mutableStateOf(false) }
    val items = listOf("Easy Math", "Medium Math", "Hard Math")
    val selectedIndex = remember { mutableStateOf(0) }
    Box {
        Text(
            items[selectedIndex.value],
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
                        selectedIndex.value = index
                        alarm.difficulty = index
                        expanded.value = false
                    }
                ) {
                    Text(text = s)
                }
            }
        }
    }
}

private fun initCalendar(alarm: Alarm): Calendar {
    val cal = Calendar.getInstance()
    cal[Calendar.HOUR_OF_DAY] = alarm.hour
    cal[Calendar.MINUTE] = alarm.minute
    cal[Calendar.SECOND] = 0
    return cal
}

sealed class SheetState(val alarmId: Long = 0L) {
    class NewAlarm() : SheetState()

    object Init : SheetState()

    class EditAlarm(id: Long) : SheetState(id)
}
