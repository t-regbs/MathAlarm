package com.timilehinaregbesola.mathalarm.presentation.alarmmath.components

import android.annotation.SuppressLint
import android.content.Context
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.Arrangement.Center
import androidx.compose.foundation.layout.Arrangement.SpaceBetween
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.material.ProgressIndicatorDefaults.ProgressAnimationSpec
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color.Companion.DarkGray
import androidx.compose.ui.graphics.Color.Companion.Transparent
import androidx.compose.ui.graphics.Color.Companion.White
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight.Companion.Bold
import androidx.compose.ui.text.input.ImeAction.Companion.Done
import androidx.compose.ui.text.input.KeyboardType.Companion.Number
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import androidx.navigation.NavHostController
import com.google.accompanist.navigation.animation.rememberAnimatedNavController
import com.timilehinaregbesola.mathalarm.domain.model.Alarm
import com.timilehinaregbesola.mathalarm.framework.database.AlarmEntity
import com.timilehinaregbesola.mathalarm.framework.database.AlarmMapper
import com.timilehinaregbesola.mathalarm.presentation.alarmlist.components.AlarmSnack
import com.timilehinaregbesola.mathalarm.presentation.alarmmath.AlarmMathViewModel
import com.timilehinaregbesola.mathalarm.presentation.alarmmath.MathScreenEvent
import com.timilehinaregbesola.mathalarm.presentation.alarmmath.MathScreenEvent.EnteredAnswer
import com.timilehinaregbesola.mathalarm.presentation.alarmmath.MathScreenEvent.OnClearClick
import com.timilehinaregbesola.mathalarm.presentation.alarmmath.MathScreenEvent.OnEnterClick
import com.timilehinaregbesola.mathalarm.presentation.alarmmath.MathScreenEvent.OnSnoozeClick
import com.timilehinaregbesola.mathalarm.presentation.alarmmath.ToneState.Countdown
import com.timilehinaregbesola.mathalarm.presentation.alarmmath.buildQuestionString
import com.timilehinaregbesola.mathalarm.presentation.alarmmath.components.MathScreen.ANSWER_FIELD_CORNER_SIZE
import com.timilehinaregbesola.mathalarm.presentation.alarmmath.components.MathScreen.ANSWER_FIELD_FONT_SIZE
import com.timilehinaregbesola.mathalarm.presentation.alarmmath.components.MathScreen.ANSWER_FIELD_HEIGHT
import com.timilehinaregbesola.mathalarm.presentation.alarmmath.components.MathScreen.ANSWER_FIELD_HORIZONTAL_PADDING
import com.timilehinaregbesola.mathalarm.presentation.alarmmath.components.MathScreen.BUTTON_SECTION_HEIGHT
import com.timilehinaregbesola.mathalarm.presentation.alarmmath.components.MathScreen.BUTTON_SECTION_HORIZONTAL_PADDING
import com.timilehinaregbesola.mathalarm.presentation.alarmmath.components.MathScreen.CLEAR_BUTTON_BOTTOM_PADDING
import com.timilehinaregbesola.mathalarm.presentation.alarmmath.components.MathScreen.CLEAR_BUTTON_HEIGHT
import com.timilehinaregbesola.mathalarm.presentation.alarmmath.components.MathScreen.CLEAR_BUTTON_WIDTH
import com.timilehinaregbesola.mathalarm.presentation.alarmmath.components.MathScreen.CLEAR_FONT_SIZE
import com.timilehinaregbesola.mathalarm.presentation.alarmmath.components.MathScreen.DEFAULT_VIBRATION_PATTERN
import com.timilehinaregbesola.mathalarm.presentation.alarmmath.components.MathScreen.ENTER_BUTTON_SIZE
import com.timilehinaregbesola.mathalarm.presentation.alarmmath.components.MathScreen.ENTER_FONT_SIZE
import com.timilehinaregbesola.mathalarm.presentation.alarmmath.components.MathScreen.FROM_SHEET_KEY
import com.timilehinaregbesola.mathalarm.presentation.alarmmath.components.MathScreen.INITIAL_INDICATOR_PROGRESS
import com.timilehinaregbesola.mathalarm.presentation.alarmmath.components.MathScreen.MAX_ANSWER_CHARS
import com.timilehinaregbesola.mathalarm.presentation.alarmmath.components.MathScreen.PROGRESS_INDICATOR_HEIGHT
import com.timilehinaregbesola.mathalarm.presentation.alarmmath.components.MathScreen.PROGRESS_LABEL
import com.timilehinaregbesola.mathalarm.presentation.alarmmath.components.MathScreen.QUESTION_FONT_SIZE
import com.timilehinaregbesola.mathalarm.presentation.alarmmath.components.MathScreen.REPEAT_INDEFINITELY
import com.timilehinaregbesola.mathalarm.presentation.alarmmath.components.MathScreen.SETTINGS_ID
import com.timilehinaregbesola.mathalarm.presentation.alarmmath.components.MathScreen.SNOOZE_BUTTON_HEIGHT
import com.timilehinaregbesola.mathalarm.presentation.alarmmath.components.MathScreen.SNOOZE_BUTTON_WIDTH
import com.timilehinaregbesola.mathalarm.presentation.alarmmath.components.MathScreen.SNOOZE_FONT_SIZE
import com.timilehinaregbesola.mathalarm.presentation.alarmmath.components.MathScreen.TEST_ALARM_KEY
import com.timilehinaregbesola.mathalarm.presentation.alarmmath.generateMathProblem
import com.timilehinaregbesola.mathalarm.presentation.ui.*
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.flow.collectLatest
import timber.log.Timber
import java.io.IOException

@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
@ExperimentalMaterialApi
@InternalCoroutinesApi
@ExperimentalComposeUiApi
@Composable
fun MathScreen(
    navController: NavHostController,
    alarm: AlarmEntity,
    viewModel: AlarmMathViewModel = hiltViewModel(),
    darkTheme: Boolean,
) {
    var vibrator: Vibrator? = null
    val context = LocalContext.current
    val problem = remember { generateMathProblem(alarm.difficulty) }
    val question = remember { mutableStateOf(buildQuestionString(problem)) }
    val scaffoldState = rememberScaffoldState()
    val keyboardController = LocalSoftwareKeyboardController.current

    LaunchedEffect(key1 = true) {
        viewModel.eventFlow.collectLatest { event ->
            when (event) {
                is AlarmMathViewModel.UiEvent.ShowSnackbar -> {
                    scaffoldState.snackbarHostState.showSnackbar(
                        message = event.message,
                    )
                }
                is AlarmMathViewModel.UiEvent.CompleteAndClose -> {
                    if (!alarm.repeat) {
                        viewModel.completeAlarm(AlarmMapper().mapToDomainModel(alarm))
                    }
                    val fromSheet = navController
                        .previousBackStackEntry?.savedStateHandle?.remove<Boolean>(FROM_SHEET_KEY)
                    fromSheet?.let {
                        navController.previousBackStackEntry?.savedStateHandle?.set(TEST_ALARM_KEY, alarm)
                    }
                    navController.popBackStack()
                }
                is AlarmMathViewModel.UiEvent.StopVibrateAndHideKeyboard -> {
                    vibrator?.cancel()
                    vibrator = null
                    keyboardController?.hide()
                }
            }
        }
    }

    DisposableEffect(true) {
        if (alarm.vibrate) {
            vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createWaveform(DEFAULT_VIBRATION_PATTERN, REPEAT_INDEFINITELY))
            } else {
                vibrator?.vibrate(DEFAULT_VIBRATION_PATTERN, REPEAT_INDEFINITELY)
            }
        }
        onDispose {
            vibrator?.cancel()
            vibrator = null
        }
    }
    if (alarm.alarmTone.isNotEmpty()) {
        val alarmUri = Uri.parse(alarm.alarmTone)
        println(navController.previousBackStackEntry?.destination?.id)
        if (navController.previousBackStackEntry?.destination?.id == SETTINGS_ID) {
            try {
                viewModel.audioPlayer.apply {
                    stop()
                    init()
                    reset()
                    setDataSource(alarmUri)
                    startAlarmAudio()
                }
                viewModel.startTimer()
            } catch (e: IOException) {
                e.printStackTrace()
            }
        }
    } else {
        Timber.d("Tone not available")
        viewModel.onEvent(MathScreenEvent.OnToneError("Tone not available"))
    }

    Scaffold(
        scaffoldState = scaffoldState,
        snackbarHost = { state -> AlarmSnack(state) },
    ) {
        with(MaterialTheme) {
            Surface(
                Modifier
                    .fillMaxSize()
                    .padding(vertical = spacing.extraMedium),
            ) {
                Column {
                    val toneState = viewModel.state.observeAsState()
                    val progress = rememberSaveable { mutableStateOf(INITIAL_INDICATOR_PROGRESS) }
                    val animatedProgress = animateFloatAsState(
                        targetValue = progress.value,
                        animationSpec = ProgressAnimationSpec,
                        label = PROGRESS_LABEL,
                    ).value

                    Spacer(modifier = Modifier.height(spacing.extraMedium))
                    if (toneState.value is Countdown) {
                        LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(PROGRESS_INDICATOR_HEIGHT)
                                .padding(horizontal = spacing.extraMedium),
                            color = indicatorColor,
                            progress = animatedProgress,
                        )
                    }
                    Spacer(modifier = Modifier.height(spacing.large))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Center,
                    ) {
                        Text(
                            text = question.value,
                            fontSize = QUESTION_FONT_SIZE,
                            fontWeight = Bold,
                        )
                    }
                    Spacer(modifier = Modifier.height(spacing.medium))
                    TextField(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(ANSWER_FIELD_HEIGHT)
                            .padding(horizontal = ANSWER_FIELD_HORIZONTAL_PADDING),
                        value = viewModel.answerText.value,
                        onValueChange = { newVal ->
                            if (newVal.length <= MAX_ANSWER_CHARS) {
                                viewModel.onEvent(
                                    EnteredAnswer(newVal.filter { it.isDigit() }),
                                )
                            }
                        },
                        singleLine = true,
                        placeholder = {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Center,
                                verticalAlignment = CenterVertically,
                            ) {
                                Text(text = "=", fontSize = ANSWER_FIELD_FONT_SIZE)
                            }
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = Number,
                            imeAction = Done,
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                viewModel.onEvent(OnEnterClick(problem))
                            },
                        ),
                        textStyle = TextStyle(
                            color = colors.onSurface,
                            fontSize = ANSWER_FIELD_FONT_SIZE,
                            textAlign = TextAlign.Center,
                        ),
                        colors = TextFieldDefaults.textFieldColors(
                            backgroundColor = if (darkTheme) DarkGray else unSelectedDay,
                            focusedIndicatorColor = Transparent,
                            unfocusedIndicatorColor = Transparent,
                            disabledIndicatorColor = Transparent,
                        ),
                        shape = shapes.medium.copy(CornerSize(ANSWER_FIELD_CORNER_SIZE)),
                    )
                    Spacer(modifier = Modifier.height(spacing.medium))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = BUTTON_SECTION_HORIZONTAL_PADDING),
                        horizontalArrangement = SpaceBetween,
                    ) {
                        Column(modifier = Modifier.height(BUTTON_SECTION_HEIGHT)) {
                            Button(
                                modifier = Modifier
                                    .height(CLEAR_BUTTON_HEIGHT)
                                    .width(CLEAR_BUTTON_WIDTH),
                                onClick = {
                                    viewModel.onEvent(OnClearClick)
                                },
                                colors = ButtonDefaults.buttonColors(
                                    backgroundColor = clearButtonColor,
                                    contentColor = White,
                                ),
                            ) {
                                Text(text = "CLEAR", fontSize = CLEAR_FONT_SIZE)
                            }
                            Spacer(modifier = Modifier.height(CLEAR_BUTTON_BOTTOM_PADDING))
                            Button(
                                modifier = Modifier
                                    .height(SNOOZE_BUTTON_HEIGHT)
                                    .width(SNOOZE_BUTTON_WIDTH),
                                enabled = alarm.snooze != 0,
                                onClick = {
                                    viewModel.onEvent(OnSnoozeClick(alarm.alarmId))
                                    navController.popBackStack()
                                },
                                colors = ButtonDefaults.buttonColors(
                                    backgroundColor = snoozeButtonColor,
                                    contentColor = White,
                                ),
                            ) {
                                Text(text = "SNOOZE", fontSize = SNOOZE_FONT_SIZE)
                            }
                        }
                        Button(
                            modifier = Modifier
                                .size(ENTER_BUTTON_SIZE),
                            onClick = {
                                viewModel.onEvent(OnEnterClick(problem))
                            },
                            colors = ButtonDefaults.buttonColors(
                                backgroundColor = enterButtonColor,
                                contentColor = White,
                            ),
                        ) {
                            Text(text = "ENTER", fontSize = ENTER_FONT_SIZE)
                        }
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@ExperimentalComposeUiApi
@InternalCoroutinesApi
@ExperimentalMaterialApi
@Preview
@Composable
fun MathPreview() {
    MathAlarmTheme(darkTheme = true) {
        MathScreen(
            navController = rememberAnimatedNavController(),
            alarm = AlarmMapper().mapFromDomainModel(Alarm()),
            darkTheme = true,
        )
    }
}

private object MathScreen {
    val DEFAULT_VIBRATION_PATTERN = longArrayOf(0, 1000, 3000)
    const val SETTINGS_ID = 1143682591
    const val TEST_ALARM_KEY = "testAlarm"
    const val FROM_SHEET_KEY = "fromSheet"
    const val INITIAL_INDICATOR_PROGRESS = 0.1f
    const val MAX_ANSWER_CHARS = 8
    const val PROGRESS_LABEL = "ProgressBar"
    const val REPEAT_INDEFINITELY = 0
    val PROGRESS_INDICATOR_HEIGHT = 10.dp
    val QUESTION_FONT_SIZE = 70.sp
    val ANSWER_FIELD_HORIZONTAL_PADDING = 56.dp
    val ANSWER_FIELD_HEIGHT = 90.dp
    val ANSWER_FIELD_CORNER_SIZE = 24.dp
    val ANSWER_FIELD_FONT_SIZE = 30.sp
    val BUTTON_SECTION_HORIZONTAL_PADDING = 56.dp
    val BUTTON_SECTION_HEIGHT = 120.dp
    val SNOOZE_BUTTON_HEIGHT = 55.dp
    val SNOOZE_BUTTON_WIDTH = 120.dp
    val CLEAR_BUTTON_HEIGHT = 55.dp
    val CLEAR_BUTTON_WIDTH = 120.dp
    val CLEAR_BUTTON_BOTTOM_PADDING = 10.dp
    val ENTER_FONT_SIZE = 19.sp
    val SNOOZE_FONT_SIZE = 19.sp
    val CLEAR_FONT_SIZE = 19.sp
    val ENTER_BUTTON_SIZE = 120.dp
}
