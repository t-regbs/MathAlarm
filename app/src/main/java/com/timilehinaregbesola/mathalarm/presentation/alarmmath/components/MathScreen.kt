package com.timilehinaregbesola.mathalarm.presentation.alarmmath.components

import android.annotation.SuppressLint
import android.content.Context
import android.os.Build
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement.Center
import androidx.compose.foundation.layout.Arrangement.SpaceBetween
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProgressIndicatorDefaults.ProgressAnimationSpec
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.material3.TextFieldDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
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
import androidx.core.net.toUri
import androidx.navigation3.runtime.NavBackStack
import cafe.adriel.lyricist.strings
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
import com.timilehinaregbesola.mathalarm.presentation.alarmmath.components.MathScreen.CLEAR_FONT_SIZE
import com.timilehinaregbesola.mathalarm.presentation.alarmmath.components.MathScreen.DEFAULT_VIBRATION_PATTERN
import com.timilehinaregbesola.mathalarm.presentation.alarmmath.components.MathScreen.ENTER_FONT_SIZE
import com.timilehinaregbesola.mathalarm.presentation.alarmmath.components.MathScreen.INITIAL_INDICATOR_PROGRESS
import com.timilehinaregbesola.mathalarm.presentation.alarmmath.components.MathScreen.MAX_ANSWER_CHARS
import com.timilehinaregbesola.mathalarm.presentation.alarmmath.components.MathScreen.PROGRESS_INDICATOR_HEIGHT
import com.timilehinaregbesola.mathalarm.presentation.alarmmath.components.MathScreen.PROGRESS_LABEL
import com.timilehinaregbesola.mathalarm.presentation.alarmmath.components.MathScreen.QUESTION_FONT_SIZE
import com.timilehinaregbesola.mathalarm.presentation.alarmmath.components.MathScreen.REPEAT_INDEFINITELY
import com.timilehinaregbesola.mathalarm.presentation.alarmmath.components.MathScreen.SNOOZE_FONT_SIZE
import com.timilehinaregbesola.mathalarm.presentation.alarmmath.generateMathProblem
import com.timilehinaregbesola.mathalarm.presentation.ui.MathAlarmTheme
import com.timilehinaregbesola.mathalarm.presentation.ui.clearButtonColor
import com.timilehinaregbesola.mathalarm.presentation.ui.enterButtonColor
import com.timilehinaregbesola.mathalarm.presentation.ui.indicatorColor
import com.timilehinaregbesola.mathalarm.presentation.ui.shapes
import com.timilehinaregbesola.mathalarm.presentation.ui.snoozeButtonColor
import com.timilehinaregbesola.mathalarm.presentation.ui.spacing
import com.timilehinaregbesola.mathalarm.presentation.ui.unSelectedDay
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.flow.collectLatest
import org.koin.compose.viewmodel.koinViewModel
import timber.log.Timber
import java.io.IOException

@SuppressLint("UnusedMaterialScaffoldPaddingParameter")
@ExperimentalMaterial3Api
@InternalCoroutinesApi
@ExperimentalComposeUiApi
@Composable
fun MathScreen(
    backStack: NavBackStack,
    alarm: AlarmEntity,
    viewModel: AlarmMathViewModel = koinViewModel(),
    darkTheme: Boolean,
    fromSheet: Boolean = false
) {
    var vibrator: Vibrator? = null
    val context = LocalContext.current
    val problem = remember { generateMathProblem(alarm.difficulty) }
    val question = remember { mutableStateOf(buildQuestionString(problem)) }
    val snackbarHostState = remember {
        SnackbarHostState()
    }
    val keyboardController = LocalSoftwareKeyboardController.current
    val toneState by viewModel.state.collectAsState()
    val progress by remember(viewModel.audioPlayer.currentPosition) {
        mutableFloatStateOf(
            if (toneState is Countdown) {
                val state = toneState as Countdown
                state.seconds / state.total.toFloat()
            } else {
                INITIAL_INDICATOR_PROGRESS
            }
        )
    }
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = ProgressAnimationSpec,
        label = PROGRESS_LABEL
    )

    LaunchedEffect(key1 = true) {
        viewModel.eventFlow.collectLatest { event ->
            when (event) {
                is AlarmMathViewModel.UiEvent.ShowSnackbar -> {
                    snackbarHostState.showSnackbar(
                        message = event.message,
                    )
                }
                is AlarmMathViewModel.UiEvent.CompleteAndClose -> {
                    if (!alarm.repeat) {
                        viewModel.completeAlarm(AlarmMapper().mapToDomainModel(alarm))
                    }
                    backStack.removeLastOrNull()
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
        if (alarm.alarmTone.isNotEmpty()) {
            val alarmUri = alarm.alarmTone.toUri()
            if (fromSheet) {
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
        onDispose {
            vibrator?.cancel()
            vibrator = null
        }
    }

    MathScreenContent(
        snackbarHostState = snackbarHostState,
        question = question.value,
        animatedProgress = animatedProgress,
        inputField = {
            MathInputField(
                value = viewModel.answerText.value,
                darkTheme = darkTheme,
                onDonePressed = {
                    viewModel.onEvent(OnEnterClick(problem))
                },
                onValueChange = { newVal ->
                    if (newVal.length <= MAX_ANSWER_CHARS) {
                        viewModel.onEvent(
                            EnteredAnswer(newVal.filter { it.isDigit() }),
                        )
                    }
                }
            )
        },
        buttonSection = {
            ButtonSection(
                alarm = alarm,
                onEnterClick = {
                    viewModel.onEvent(OnEnterClick(problem))
                },
                onClearClick = {
                    viewModel.onEvent(OnClearClick)
                },
                onSnoozeClick = {
                    viewModel.onEvent(OnSnoozeClick(alarm.alarmId))
                    backStack.removeLastOrNull()
                }
            )
        }
    )
}

@ExperimentalMaterial3Api
@Composable
private fun MathScreenContent(
    snackbarHostState: SnackbarHostState,
    question: String,
    animatedProgress: Float,
    inputField: @Composable () -> Unit,
    buttonSection: @Composable () -> Unit
) {
    Scaffold(
        snackbarHost = { AlarmSnack(state = snackbarHostState) },
    ) { padding ->
        with(MaterialTheme) {
            Surface(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(vertical = spacing.extraMedium),
            ) {
                Column {
                    Spacer(modifier = Modifier.height(spacing.extraMedium))
                    LinearProgressIndicator(
                        progress = { animatedProgress },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(PROGRESS_INDICATOR_HEIGHT)
                            .padding(horizontal = spacing.extraMedium),
                        color = indicatorColor,
                    )
                    Spacer(modifier = Modifier.height(spacing.large))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Center,
                    ) {
                        Text(
                            text = question,
                            fontSize = QUESTION_FONT_SIZE,
                            fontWeight = Bold,
                        )
                    }
                    Spacer(modifier = Modifier.height(spacing.medium))
                    inputField()
                    Spacer(modifier = Modifier.height(spacing.medium))
                    buttonSection()
                }
            }
        }
    }
}

@ExperimentalMaterial3Api
@Composable
private fun MathInputField(
    value: String,
    darkTheme: Boolean,
    onDonePressed: () -> Unit,
    onValueChange: (String) -> Unit,
) {
    TextField(
        modifier = Modifier
            .fillMaxWidth()
            .height(ANSWER_FIELD_HEIGHT)
            .padding(horizontal = ANSWER_FIELD_HORIZONTAL_PADDING),
        value = value,
        onValueChange = onValueChange,
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
                onDonePressed()
            },
        ),
        textStyle = TextStyle(
            color = MaterialTheme.colorScheme.onSurface,
            fontSize = ANSWER_FIELD_FONT_SIZE,
            textAlign = TextAlign.Center,
        ),
        colors = TextFieldDefaults.colors(
            unfocusedContainerColor = if (darkTheme) DarkGray else unSelectedDay,
            focusedContainerColor = if (darkTheme) DarkGray else unSelectedDay,
            focusedIndicatorColor = Transparent,
            unfocusedIndicatorColor = Transparent,
            disabledIndicatorColor = Transparent,
            cursorColor = MaterialTheme.colorScheme.onSurface
        ),
        shape = shapes.medium.copy(CornerSize(ANSWER_FIELD_CORNER_SIZE)),
    )
}

@Composable
private fun ButtonSection(
    alarm: AlarmEntity,
    onClearClick: () -> Unit,
    onSnoozeClick: () -> Unit,
    onEnterClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .height(IntrinsicSize.Max)
            .padding(horizontal = BUTTON_SECTION_HORIZONTAL_PADDING),
        horizontalArrangement = SpaceBetween,
    ) {
        Column(
            modifier = Modifier
                .padding(end = 8.dp)
                .weight(1f)
                .height(BUTTON_SECTION_HEIGHT),
            verticalArrangement = SpaceBetween,
        ) {
            Button(
                modifier = Modifier.fillMaxWidth(),
                onClick = {
                    onClearClick()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = clearButtonColor,
                    contentColor = White,
                ),
            ) {
                Text(text = strings.clear.uppercase(), fontSize = CLEAR_FONT_SIZE)
            }
            Button(
                modifier = Modifier.fillMaxWidth(),
                enabled = alarm.snooze != 0,
                onClick = {
                    onSnoozeClick()
                },
                colors = ButtonDefaults.buttonColors(
                    containerColor = snoozeButtonColor,
                    contentColor = White,
                ),
            ) {
                Text(text = strings.snooze.uppercase(), fontSize = SNOOZE_FONT_SIZE)
            }
        }
        Button(
            modifier = Modifier
                .weight(1f)
                .fillMaxHeight(),
            onClick = {
                onEnterClick()
            },
            shape = RoundedCornerShape(8.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = enterButtonColor,
                contentColor = White,
            ),
        ) {
            Text(text = strings.enter.uppercase(), fontSize = ENTER_FONT_SIZE)
        }
    }
}

@ExperimentalComposeUiApi
@InternalCoroutinesApi
@ExperimentalMaterial3Api
@Preview
@Composable
fun MathPreview() {
    MathAlarmTheme(darkTheme = true) {
        MathScreenContent(
            snackbarHostState = SnackbarHostState(),
            question = "1 + 1",
            animatedProgress = INITIAL_INDICATOR_PROGRESS,
            inputField = {
                MathInputField(
                    value = "",
                    darkTheme = true,
                    onDonePressed = { },
                    onValueChange = { }
                )
            },
            buttonSection = {
                ButtonSection(
                    alarm = AlarmMapper().mapFromDomainModel(Alarm()),
                    onClearClick = { },
                    onSnoozeClick = { },
                    onEnterClick = { }
                )
            }
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
    val ENTER_FONT_SIZE = 19.sp
    val SNOOZE_FONT_SIZE = 19.sp
    val CLEAR_FONT_SIZE = 19.sp
}
