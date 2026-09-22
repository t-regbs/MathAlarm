package com.timilehinaregbesola.mathalarm.presentation.alarmmath.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.foundation.layout.Arrangement.Center
import androidx.compose.foundation.layout.Arrangement.spacedBy
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.TextAutoSize
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.TextButton
import androidx.compose.material3.FilledTonalButton
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.text.TextAutoSize.Companion.StepBased
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
import androidx.compose.runtime.getValue
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Alignment.Companion.CenterVertically
import androidx.compose.ui.Alignment.Companion.TopCenter
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color.Companion.Transparent
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight.Companion.Bold
import androidx.compose.ui.text.input.ImeAction.Companion.Done
import androidx.compose.ui.text.input.KeyboardType.Companion.Number
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import cafe.adriel.lyricist.strings
import co.touchlab.kermit.Logger
import com.mohamedrejeb.calf.ui.button.AdaptiveButton as Button
import com.timilehinaregbesola.mathalarm.domain.model.Alarm
import com.timilehinaregbesola.mathalarm.framework.database.AlarmEntity
import com.timilehinaregbesola.mathalarm.framework.database.AlarmMapper
import com.timilehinaregbesola.mathalarm.platform.PlatformVibrator
import com.timilehinaregbesola.mathalarm.platform.getDefaultAlarmTone
import com.timilehinaregbesola.mathalarm.platform.shouldStartMathScreenAlarmAudio
import com.timilehinaregbesola.mathalarm.presentation.alarmlist.components.AlarmSnack
import com.timilehinaregbesola.mathalarm.presentation.alarmmath.AlarmMathViewModel
import com.timilehinaregbesola.mathalarm.presentation.alarmmath.MathScreenEvent
import com.timilehinaregbesola.mathalarm.presentation.alarmmath.MathScreenEvent.EnteredAnswer
import com.timilehinaregbesola.mathalarm.presentation.alarmmath.MathScreenEvent.OnClearClick
import com.timilehinaregbesola.mathalarm.presentation.alarmmath.MathScreenEvent.OnEnterClick
import com.timilehinaregbesola.mathalarm.presentation.alarmmath.MathScreenEvent.OnSnoozeClick
import com.timilehinaregbesola.mathalarm.platform.ChallengeBackHandler
import com.timilehinaregbesola.mathalarm.presentation.alarmmath.buildQuestionString
import com.timilehinaregbesola.mathalarm.presentation.alarmmath.components.MathScreen.ANSWER_FIELD_CORNER_SIZE
import com.timilehinaregbesola.mathalarm.presentation.alarmmath.components.MathScreen.ANSWER_FIELD_FONT_SIZE
import com.timilehinaregbesola.mathalarm.presentation.alarmmath.components.MathScreen.ANSWER_FIELD_HEIGHT
import com.timilehinaregbesola.mathalarm.presentation.alarmmath.components.MathScreen.ANSWER_FIELD_HORIZONTAL_PADDING
import com.timilehinaregbesola.mathalarm.presentation.alarmmath.components.MathScreen.BUTTON_SECTION_HORIZONTAL_PADDING
import com.timilehinaregbesola.mathalarm.presentation.alarmmath.components.MathScreen.DEFAULT_VIBRATION_PATTERN
import com.timilehinaregbesola.mathalarm.presentation.alarmmath.components.MathScreen.INITIAL_INDICATOR_PROGRESS
import com.timilehinaregbesola.mathalarm.presentation.alarmmath.components.MathScreen.MATH_CONTENT_MAX_WIDTH
import com.timilehinaregbesola.mathalarm.presentation.alarmmath.components.MathScreen.MAX_ANSWER_CHARS
import com.timilehinaregbesola.mathalarm.presentation.alarmmath.components.MathScreen.PROGRESS_INDICATOR_HEIGHT
import com.timilehinaregbesola.mathalarm.presentation.alarmmath.components.MathScreen.PROGRESS_LABEL
import com.timilehinaregbesola.mathalarm.presentation.alarmmath.components.MathScreen.QUESTION_FONT_SIZE
import com.timilehinaregbesola.mathalarm.presentation.alarmmath.components.MathScreen.REPEAT_INDEFINITELY
import com.timilehinaregbesola.mathalarm.presentation.ui.MathAlarmTheme
import com.timilehinaregbesola.mathalarm.presentation.ui.shapes
import com.timilehinaregbesola.mathalarm.presentation.ui.spacing
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.flow.collectLatest
import androidx.compose.ui.tooling.preview.Preview
import org.koin.compose.viewmodel.koinViewModel

@ExperimentalMaterial3Api
@InternalCoroutinesApi
@ExperimentalComposeUiApi
@Composable
fun MathScreen(
    backStack: NavBackStack<NavKey>,
    alarm: AlarmEntity,
    viewModel: AlarmMathViewModel = koinViewModel(),
    fromSheet: Boolean = false
) {
    // Consume Back before Navigation 3 can animate a predictive pop. Rejecting only
    // its completion callback is too late to keep the challenge visibly in place.
    ChallengeBackHandler(enabled = true) {
        if (fromSheet && backStack.size > 1) backStack.removeLastOrNull()
    }
    val vibrator = remember(alarm.alarmId, alarm.vibrate) { if (alarm.vibrate) PlatformVibrator() else null }
    LaunchedEffect(alarm.alarmId, alarm.activeAt, fromSheet) {
        viewModel.initializeChallenge(
            alarm = AlarmMapper().mapToDomainModel(alarm),
            preview = fromSheet
        )
    }
    val snackbarHostState = remember {
        SnackbarHostState()
    }
    val keyboardController = LocalSoftwareKeyboardController.current
    val progress = viewModel.questionIndex.value.toFloat() / viewModel.questionCount
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = ProgressAnimationSpec,
        label = PROGRESS_LABEL
    )
    val errorStrings = strings
    LaunchedEffect(errorStrings) {
        viewModel.eventFlow.collectLatest { event ->
            when (event) {
                is AlarmMathViewModel.UiEvent.ShowError -> {
                    snackbarHostState.showSnackbar(message = event.error.resolve(errorStrings))
                }
                is AlarmMathViewModel.UiEvent.CompleteAndClose -> {
                    viewModel.completeAlarm(AlarmMapper().mapToDomainModel(alarm), preview = fromSheet)
                }
                is AlarmMathViewModel.UiEvent.Close -> {
                    if (backStack.size > 1) backStack.removeLastOrNull()
                }
                is AlarmMathViewModel.UiEvent.StopVibrateAndHideKeyboard -> {
                    vibrator?.cancel()
                    keyboardController?.hide()
                }
            }
        }
    }

    DisposableEffect(true) {
        if (alarm.vibrate) {
            vibrator?.startWaveform(DEFAULT_VIBRATION_PATTERN, REPEAT_INDEFINITELY)
        }
        val alarmTone = alarm.alarmTone.ifEmpty { getDefaultAlarmTone() }
        if (alarmTone.isNotEmpty() && shouldStartMathScreenAlarmAudio(fromSheet)) {
            try {
                viewModel.startAlarmWith(alarmTone)
            } catch (_: Throwable) {
            }
        } else if (alarmTone.isEmpty()) {
            Logger.d("Tone not available")
            viewModel.onEvent(MathScreenEvent.OnToneError("Tone not available"))
        }
        onDispose {
            vibrator?.cancel()
            if (fromSheet) viewModel.stopPreview()
        }
    }

    val problem = viewModel.currentProblem ?: return
    MathScreenContent(
        snackbarHostState = snackbarHostState,
        onClosePreview = if (fromSheet) ({ backStack.removeLastOrNull() }) else null,
        question = buildQuestionString(problem),
        questionProgress = if (viewModel.questionCount > 1) {
            strings.questionProgress(viewModel.questionIndex.value + 1, viewModel.questionCount)
        } else {
            null
        },
        animatedProgress = animatedProgress,
        inputField = {
            MathInputField(
                value = viewModel.answerText.value,
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
                alarm = viewModel.currentAlarm?.let { AlarmMapper().mapFromDomainModel(it) } ?: alarm,
                onEnterClick = {
                    viewModel.onEvent(OnEnterClick(problem))
                },
                onClearClick = {
                    viewModel.onEvent(OnClearClick)
                },
                onSnoozeClick = {
                    viewModel.onEvent(OnSnoozeClick(alarm.alarmId, preview = fromSheet))
                }
            )
        }
    )
}

@ExperimentalMaterial3Api
@Composable
private fun MathScreenContent(
    snackbarHostState: SnackbarHostState,
    onClosePreview: (() -> Unit)? = null,
    question: String,
    questionProgress: String? = null,
    animatedProgress: Float,
    inputField: @Composable () -> Unit,
    buttonSection: @Composable () -> Unit
) {
    Scaffold(
        topBar = {
            if (onClosePreview != null) {
                Row(
                    Modifier.fillMaxWidth().statusBarsPadding(),
                    horizontalArrangement = androidx.compose.foundation.layout.Arrangement.End,
                ) {
                    TextButton(onClick = onClosePreview) { Text(strings.cancel) }
                }
            }
        },
        snackbarHost = { AlarmSnack(state = snackbarHostState) },
    ) { padding ->
        with(MaterialTheme) {
            Surface(
                Modifier
                    .fillMaxSize()
                    .padding(padding)
                    .padding(vertical = spacing.extraMedium),
            ) {
                BoxWithConstraints(
                    modifier = Modifier.fillMaxSize(),
                ) {
                    val centerContent = minOf(maxWidth, maxHeight) >= MathScreen.CENTERED_CONTENT_MIN_SIZE
                    val contentWidthModifier = if (maxWidth > MATH_CONTENT_MAX_WIDTH) {
                        Modifier.width(MATH_CONTENT_MAX_WIDTH)
                    } else {
                        Modifier.fillMaxWidth()
                    }
                    Column(
                        modifier = contentWidthModifier
                            .align(if (centerContent) Alignment.Center else TopCenter)
                            .verticalScroll(rememberScrollState()),
                    ) {
                        Spacer(modifier = Modifier.height(spacing.extraMedium))
                        if (questionProgress != null) {
                            LinearProgressIndicator(
                                progress = { animatedProgress },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(PROGRESS_INDICATOR_HEIGHT)
                                    .padding(horizontal = spacing.extraMedium),
                                color = colorScheme.primary,
                                trackColor = colorScheme.surfaceVariant,
                                drawStopIndicator = {},
                            )
                            Spacer(modifier = Modifier.height(spacing.large))
                            Text(
                                questionProgress,
                                modifier = Modifier.fillMaxWidth(),
                                textAlign = TextAlign.Center,
                                style = typography.titleMedium,
                            )
                            Spacer(modifier = Modifier.height(spacing.medium))
                        } else if (!centerContent) {
                            Spacer(modifier = Modifier.height(MathScreen.SINGLE_QUESTION_TOP_SPACING))
                        }
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Center,
                        ) {
                            BasicText(
                                text = question,
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = spacing.extraMedium),
                                maxLines = 1,
                                style = typography.headlineLarge.copy(
                                    color = colorScheme.onSurface,
                                    fontWeight = Bold,
                                    textAlign = TextAlign.Center,
                                ),
                                autoSize = StepBased(
                                    minFontSize = 24.sp,
                                    maxFontSize = QUESTION_FONT_SIZE
                                ),
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
}

@ExperimentalMaterial3Api
@Composable
private fun MathInputField(
    value: String,
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
            unfocusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
            focusedContainerColor = MaterialTheme.colorScheme.surfaceContainerHigh,
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
    onEnterClick: () -> Unit,
) {
    val snoozePolicy = AlarmMapper().mapToDomainModel(alarm)
    val snoozeEnabled = snoozePolicy.canSnooze

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = BUTTON_SECTION_HORIZONTAL_PADDING),
        verticalArrangement = spacedBy(MathScreen.ACTION_SPACING),
    ) {
        Button(
            onClick = onEnterClick,
            modifier = Modifier
                .fillMaxWidth()
                .heightIn(min = MathScreen.PRIMARY_ACTION_MIN_HEIGHT),
            colors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.colorScheme.primary,
                contentColor = MaterialTheme.colorScheme.onPrimary,
            ),
        ) {
            Text(strings.checkAnswer, style = MaterialTheme.typography.titleMedium)
        }
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = spacedBy(MathScreen.ACTION_SPACING)
        ) {
            TextButton(
                onClick = onClearClick,
                modifier = Modifier
                    .weight(1f)
                    .heightIn(min = MathScreen.SECONDARY_ACTION_MIN_HEIGHT),
            ) {
                Text(
                    text = strings.clear,
                    style = MaterialTheme.typography.titleMedium
                )
            }
            if (snoozeEnabled) {
                FilledTonalButton(
                    onClick = onSnoozeClick,
                    modifier = Modifier.weight(1f).heightIn(min = MathScreen.SECONDARY_ACTION_MIN_HEIGHT),
                ) { Text(strings.snooze, style = MaterialTheme.typography.titleMedium) }
            }
        }
        if (alarm.snooze > 0) {
            snoozePolicy.snoozesRemaining?.let { remaining ->
                Text(
                    text = if (remaining == 0) strings.noSnoozesLeft else strings.snoozesLeft(remaining),
                    modifier = Modifier.fillMaxWidth(),
                    textAlign = TextAlign.Center,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
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
    val CENTERED_CONTENT_MIN_SIZE = 600.dp
    val SINGLE_QUESTION_TOP_SPACING = 80.dp
    val ACTION_SPACING = 12.dp
    val PRIMARY_ACTION_MIN_HEIGHT = 56.dp
    val SECONDARY_ACTION_MIN_HEIGHT = 48.dp
    val DEFAULT_VIBRATION_PATTERN = longArrayOf(0, 1000, 3000)
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
    val MATH_CONTENT_MAX_WIDTH = 520.dp
}
