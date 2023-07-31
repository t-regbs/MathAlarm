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
import com.timilehinaregbesola.mathalarm.presentation.alarmmath.components.MathScreen.AnswerFieldCornerSize
import com.timilehinaregbesola.mathalarm.presentation.alarmmath.components.MathScreen.AnswerFieldFontSize
import com.timilehinaregbesola.mathalarm.presentation.alarmmath.components.MathScreen.AnswerFieldHeight
import com.timilehinaregbesola.mathalarm.presentation.alarmmath.components.MathScreen.AnswerFieldHorizontalPadding
import com.timilehinaregbesola.mathalarm.presentation.alarmmath.components.MathScreen.ButtonSectionHeight
import com.timilehinaregbesola.mathalarm.presentation.alarmmath.components.MathScreen.ButtonSectionHorizontalPadding
import com.timilehinaregbesola.mathalarm.presentation.alarmmath.components.MathScreen.ClearButtonBottomPadding
import com.timilehinaregbesola.mathalarm.presentation.alarmmath.components.MathScreen.ClearButtonHeight
import com.timilehinaregbesola.mathalarm.presentation.alarmmath.components.MathScreen.ClearButtonWidth
import com.timilehinaregbesola.mathalarm.presentation.alarmmath.components.MathScreen.ClearFontSize
import com.timilehinaregbesola.mathalarm.presentation.alarmmath.components.MathScreen.DefaultVibrationPattern
import com.timilehinaregbesola.mathalarm.presentation.alarmmath.components.MathScreen.EnterButtonSize
import com.timilehinaregbesola.mathalarm.presentation.alarmmath.components.MathScreen.EnterFontSize
import com.timilehinaregbesola.mathalarm.presentation.alarmmath.components.MathScreen.FromSheetKey
import com.timilehinaregbesola.mathalarm.presentation.alarmmath.components.MathScreen.InitialIndicatorProgress
import com.timilehinaregbesola.mathalarm.presentation.alarmmath.components.MathScreen.MaxAnswerChars
import com.timilehinaregbesola.mathalarm.presentation.alarmmath.components.MathScreen.ProgressIndicatorHeight
import com.timilehinaregbesola.mathalarm.presentation.alarmmath.components.MathScreen.ProgressLabel
import com.timilehinaregbesola.mathalarm.presentation.alarmmath.components.MathScreen.QuestionFontSize
import com.timilehinaregbesola.mathalarm.presentation.alarmmath.components.MathScreen.RepeatIndefinitely
import com.timilehinaregbesola.mathalarm.presentation.alarmmath.components.MathScreen.SettingsId
import com.timilehinaregbesola.mathalarm.presentation.alarmmath.components.MathScreen.SnoozeButtonHeight
import com.timilehinaregbesola.mathalarm.presentation.alarmmath.components.MathScreen.SnoozeButtonWidth
import com.timilehinaregbesola.mathalarm.presentation.alarmmath.components.MathScreen.SnoozeFontSize
import com.timilehinaregbesola.mathalarm.presentation.alarmmath.components.MathScreen.TestAlarmKey
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
                        .previousBackStackEntry?.savedStateHandle?.remove<Boolean>(FromSheetKey)
                    fromSheet?.let {
                        navController.previousBackStackEntry?.savedStateHandle?.set(TestAlarmKey, alarm)
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
                vibrator?.vibrate(VibrationEffect.createWaveform(DefaultVibrationPattern, RepeatIndefinitely))
            } else {
                vibrator?.vibrate(DefaultVibrationPattern, RepeatIndefinitely)
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
        if (navController.previousBackStackEntry?.destination?.id == SettingsId) {
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
                    val progress = rememberSaveable { mutableStateOf(InitialIndicatorProgress) }
                    val animatedProgress = animateFloatAsState(
                        targetValue = progress.value,
                        animationSpec = ProgressAnimationSpec,
                        label = ProgressLabel,
                    ).value

                    Spacer(modifier = Modifier.height(spacing.extraMedium))
                    if (toneState.value is Countdown) {
                        LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(ProgressIndicatorHeight)
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
                            fontSize = QuestionFontSize,
                            fontWeight = Bold,
                        )
                    }
                    Spacer(modifier = Modifier.height(spacing.medium))
                    TextField(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(AnswerFieldHeight)
                            .padding(horizontal = AnswerFieldHorizontalPadding),
                        value = viewModel.answerText.value,
                        onValueChange = { newVal ->
                            if (newVal.length <= MaxAnswerChars) {
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
                                Text(text = "=", fontSize = AnswerFieldFontSize)
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
                            fontSize = AnswerFieldFontSize,
                            textAlign = TextAlign.Center,
                        ),
                        colors = TextFieldDefaults.textFieldColors(
                            backgroundColor = if (darkTheme) DarkGray else unSelectedDay,
                            focusedIndicatorColor = Transparent,
                            unfocusedIndicatorColor = Transparent,
                            disabledIndicatorColor = Transparent,
                        ),
                        shape = shapes.medium.copy(CornerSize(AnswerFieldCornerSize)),
                    )
                    Spacer(modifier = Modifier.height(spacing.medium))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = ButtonSectionHorizontalPadding),
                        horizontalArrangement = SpaceBetween,
                    ) {
                        Column(modifier = Modifier.height(ButtonSectionHeight)) {
                            Button(
                                modifier = Modifier
                                    .height(ClearButtonHeight)
                                    .width(ClearButtonWidth),
                                onClick = {
                                    viewModel.onEvent(OnClearClick)
                                },
                                colors = ButtonDefaults.buttonColors(
                                    backgroundColor = clearButtonColor,
                                    contentColor = White,
                                ),
                            ) {
                                Text(text = "CLEAR", fontSize = ClearFontSize)
                            }
                            Spacer(modifier = Modifier.height(ClearButtonBottomPadding))
                            Button(
                                modifier = Modifier
                                    .height(SnoozeButtonHeight)
                                    .width(SnoozeButtonWidth),
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
                                Text(text = "SNOOZE", fontSize = SnoozeFontSize)
                            }
                        }
                        Button(
                            modifier = Modifier
                                .size(EnterButtonSize),
                            onClick = {
                                viewModel.onEvent(OnEnterClick(problem))
                            },
                            colors = ButtonDefaults.buttonColors(
                                backgroundColor = enterButtonColor,
                                contentColor = White,
                            ),
                        ) {
                            Text(text = "ENTER", fontSize = EnterFontSize)
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
    val DefaultVibrationPattern = longArrayOf(0, 1000, 3000)
    const val SettingsId = 1143682591
    const val TestAlarmKey = "testAlarm"
    const val FromSheetKey = "fromSheet"
    const val InitialIndicatorProgress = 0.1f
    const val MaxAnswerChars = 8
    const val ProgressLabel = "ProgressBar"
    const val RepeatIndefinitely = 0
    val ProgressIndicatorHeight = 10.dp
    val QuestionFontSize = 70.sp
    val AnswerFieldHorizontalPadding = 56.dp
    val AnswerFieldHeight = 90.dp
    val AnswerFieldCornerSize = 24.dp
    val AnswerFieldFontSize = 30.sp
    val ButtonSectionHorizontalPadding = 56.dp
    val ButtonSectionHeight = 120.dp
    val SnoozeButtonHeight = 55.dp
    val SnoozeButtonWidth = 120.dp
    val ClearButtonHeight = 55.dp
    val ClearButtonWidth = 120.dp
    val ClearButtonBottomPadding = 10.dp
    val EnterFontSize = 19.sp
    val SnoozeFontSize = 19.sp
    val ClearFontSize = 19.sp
    val EnterButtonSize = 120.dp
}
