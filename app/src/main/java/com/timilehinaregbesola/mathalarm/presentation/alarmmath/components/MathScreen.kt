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
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.runtime.livedata.observeAsState
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
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
import com.timilehinaregbesola.mathalarm.presentation.alarmmath.ToneState
import com.timilehinaregbesola.mathalarm.presentation.alarmmath.buildQuestionString
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
    val settingsId = 1143682591
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
                    val fromSheet = navController.previousBackStackEntry?.savedStateHandle?.remove<Boolean>("fromSheet")
                    fromSheet?.let {
                        navController.previousBackStackEntry?.savedStateHandle?.set("testAlarm", alarm)
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
        // Vibrate phone
        if (alarm.vibrate) {
            vibrator =
                context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
            val pattern = longArrayOf(0, 1000, 3000)

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
            } else {
                // 0 = Repeat Indefinitely
                vibrator?.vibrate(pattern, 0)
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
        if (navController.previousBackStackEntry?.destination?.id == settingsId) {
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
        Surface(
            Modifier
                .fillMaxSize()
                .padding(vertical = MaterialTheme.spacing.extraMedium),
        ) {
            Column {
                val toneState = viewModel.state.observeAsState()
                val maxChar = 8

                val progress = rememberSaveable { mutableStateOf(0.1f) }
                val animatedProgress = animateFloatAsState(
                    targetValue = progress.value,
                    animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec,
                    label = "ProgressBar",
                ).value

                Spacer(modifier = Modifier.height(MaterialTheme.spacing.extraMedium))
                if (toneState.value is ToneState.Countdown) {
                    LinearProgressIndicator(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(10.dp)
                            .padding(horizontal = MaterialTheme.spacing.extraMedium),
                        color = indicatorColor,
                        progress = animatedProgress,
                    )
                }
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.large))
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                ) {
                    Text(
                        text = question.value ?: "",
                        fontSize = 70.sp,
                        fontWeight = FontWeight.Bold,
                    )
                }
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))
                TextField(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(90.dp)
                        .padding(horizontal = 56.dp),
                    value = viewModel.answerText.value,
                    onValueChange = { newVal ->
                        if (newVal.length <= maxChar) {
                            viewModel.onEvent(
                                MathScreenEvent.EnteredAnswer(newVal.filter { it.isDigit() }),
                            )
                        }
                    },
                    singleLine = true,
                    placeholder = {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.Center,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(text = "=", fontSize = 30.sp)
                        }
                    },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done,
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            viewModel.onEvent(MathScreenEvent.OnEnterClick(problem))
                        },
                    ),
                    textStyle = TextStyle(
                        color = MaterialTheme.colors.onSurface,
                        fontSize = 30.sp,
                        textAlign = TextAlign.Center,
                    ),
                    colors = TextFieldDefaults.textFieldColors(
                        backgroundColor = if (darkTheme) Color.DarkGray else unSelectedDay,
                        focusedIndicatorColor = Color.Transparent,
                        unfocusedIndicatorColor = Color.Transparent,
                        disabledIndicatorColor = Color.Transparent,
                    ),
                    shape = MaterialTheme.shapes.medium.copy(CornerSize(24.dp)),
                )
                Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 56.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                ) {
                    Column(modifier = Modifier.height(120.dp)) {
                        Button(
                            modifier = Modifier
                                .height(55.dp)
                                .width(120.dp),
                            onClick = {
                                viewModel.onEvent(MathScreenEvent.OnClearClick)
                            },
                            colors = ButtonDefaults.buttonColors(
                                backgroundColor = clearButtonColor,
                                contentColor = Color.White,
                            ),
                        ) {
                            Text(text = "CLEAR", fontSize = 19.sp)
                        }
                        Spacer(modifier = Modifier.height(10.dp))
                        Button(
                            modifier = Modifier
                                .height(55.dp)
                                .width(120.dp),
                            enabled = alarm.snooze != 0,
                            onClick = {
                                viewModel.onEvent(MathScreenEvent.OnSnoozeClick(alarm.alarmId))
                                navController.popBackStack()
                            },
                            colors = ButtonDefaults.buttonColors(
                                backgroundColor = snoozeButtonColor,
                                contentColor = Color.White,
                            ),
                        ) {
                            Text(text = "SNOOZE", fontSize = 19.sp)
                        }
                    }
                    Button(
                        modifier = Modifier
                            .height(120.dp)
                            .width(120.dp),
                        onClick = {
                            viewModel.onEvent(MathScreenEvent.OnEnterClick(problem))
                        },
                        colors = ButtonDefaults.buttonColors(
                            backgroundColor = enterButtonColor,
                            contentColor = Color.White,
                        ),
                    ) {
                        Text(text = "ENTER", fontSize = 19.sp)
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
