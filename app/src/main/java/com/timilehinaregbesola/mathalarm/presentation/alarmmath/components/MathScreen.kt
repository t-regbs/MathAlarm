package com.timilehinaregbesola.mathalarm.presentation.alarmmath.components

import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.Vibrator
import androidx.activity.compose.BackHandler
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
import androidx.compose.ui.platform.SoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.navigation.compose.hiltViewModel
import com.ramcosta.composedestinations.annotation.Destination
import com.ramcosta.composedestinations.result.ResultBackNavigator
import com.timilehinaregbesola.mathalarm.domain.model.Alarm
import com.timilehinaregbesola.mathalarm.framework.database.AlarmEntity
import com.timilehinaregbesola.mathalarm.framework.database.AlarmMapper
import com.timilehinaregbesola.mathalarm.presentation.alarmlist.components.AlarmSnack
import com.timilehinaregbesola.mathalarm.presentation.alarmmath.AlarmMathViewModel
import com.timilehinaregbesola.mathalarm.presentation.alarmmath.ToneState
import com.timilehinaregbesola.mathalarm.presentation.appsettings.AlarmPreferencesImpl
import com.timilehinaregbesola.mathalarm.presentation.appsettings.shouldUseDarkColors
import com.timilehinaregbesola.mathalarm.presentation.ui.*
import com.timilehinaregbesola.mathalarm.utils.EASY
import com.timilehinaregbesola.mathalarm.utils.HARD
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.IOException
import kotlin.random.Random

private const val ADD = 0
private const val SUBTRACT = 1
private const val TIMES = 2
private const val DIVIDE = 3

@ExperimentalMaterialApi
@InternalCoroutinesApi
@ExperimentalComposeUiApi
@Destination
@Composable
fun MathScreen(
    resultNavigator: ResultBackNavigator<AlarmEntity>,
    curAlarm: AlarmEntity,
    viewModel: AlarmMathViewModel = hiltViewModel(),
    pref: AlarmPreferencesImpl,
) {
    val darkTheme = pref.shouldUseDarkColors()
    BackHandler { }
    var vibrator: Vibrator? = null
    val settingsId = 1143682591
//    val alarm = viewModel.retrieveAlarm(alarmId)
    val alarm = AlarmMapper().mapToDomainModel(curAlarm)
    val context = LocalContext.current
    alarm.let {
        if (alarm.alarmTone.isNotEmpty()) {
            val alarmUri = Uri.parse(alarm.alarmTone)
            try {
                viewModel.audioPlayer.apply {
                    reset()
                    setDataSource(context, alarmUri)
                    setAudioAttributes(
                        AudioAttributes.Builder()
                            .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                            .setUsage(AudioAttributes.USAGE_ALARM)
                            .build()
                    )
                    prepare()
                    isLooping = true
                    start()
                    viewModel.startTimer()
                }
            } catch (e: IOException) {
                e.printStackTrace()
            }
        } else {
            Timber.d("Tone not available")
            // Should show tone not available snackbar
        }

        // Vibrate phone
        if (alarm.vibrate) {
//            vibrator =
//                context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
//            val pattern = longArrayOf(0, 1000, 3000)
//
//            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
//                vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
//            } else {
//                // 0 = Repeat Indefinitely
//                vibrator?.vibrate(pattern, 0)
//            }
        }

        // Get difficulty
        val problem = remember { getMathProblem(it.difficulty) }
        val question = remember { mutableStateOf(getMathString(problem)) }
        val scaffoldState = rememberScaffoldState()
        val scope = rememberCoroutineScope()

        Scaffold(
            scaffoldState = scaffoldState,
            snackbarHost = { state -> AlarmSnack(state) }
        ) {
            Surface(
                Modifier
                    .fillMaxSize()
                    .padding(vertical = MaterialTheme.spacing.extraMedium)
            ) {
                Column {
                    val toneState = viewModel.state.observeAsState()
                    val answerText = rememberSaveable { mutableStateOf("") }
                    val keyboardController = LocalSoftwareKeyboardController.current
                    val maxChar = 8

                    val progress = rememberSaveable { mutableStateOf(0.1f) }
                    val animatedProgress = animateFloatAsState(
                        targetValue = progress.value,
                        animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec
                    ).value

                    Spacer(modifier = Modifier.height(MaterialTheme.spacing.extraMedium))
                    if (toneState.value is ToneState.Countdown) {
                        val ts = toneState.value as ToneState.Countdown
                        Timber.d("seconds: ${ts.seconds}")
                        Timber.d("total: ${ts.total}")
                        progress.value = ((viewModel.audioPlayer.currentPosition / 1000).toFloat() / (viewModel.audioPlayer.duration / 1000).toFloat())
                        Timber.d("progrss: ${progress.value}")
                        LinearProgressIndicator(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(10.dp)
                                .padding(horizontal = MaterialTheme.spacing.extraMedium),
                            color = indicatorColor,
                            progress = animatedProgress
                        )
                    }
                    Spacer(modifier = Modifier.height(MaterialTheme.spacing.large))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = question.value ?: "",
                            fontSize = 70.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))
                    TextField(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(90.dp)
                            .padding(horizontal = 56.dp),
                        value = answerText.value,
                        onValueChange = { if (it.length <= maxChar) answerText.value = it },
                        singleLine = true,
                        placeholder = {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.Center,
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = "=", fontSize = 30.sp)
                            }
                        },
                        keyboardOptions = KeyboardOptions(
                            keyboardType = KeyboardType.Number,
                            imeAction = ImeAction.Done
                        ),
                        keyboardActions = KeyboardActions(
                            onDone = {
                                dismissAlarm(
                                    answerText,
                                    problem,
                                    viewModel.audioPlayer,
                                    keyboardController,
                                    resultNavigator,
                                    alarm,
                                    viewModel,
                                    {
                                        vibrator?.cancel()
                                        vibrator = null
                                    }
                                ) {
                                    scope.launch {
                                        scaffoldState.snackbarHostState.showSnackbar("Incorrect!")
                                    }
                                }
                            }
                        ),
                        textStyle = TextStyle(
                            color = MaterialTheme.colors.onSurface,
                            fontSize = 30.sp,
                            textAlign = TextAlign.Center
                        ),
                        colors = TextFieldDefaults.textFieldColors(
                            backgroundColor = if (darkTheme) Color.DarkGray else unSelectedDay,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            disabledIndicatorColor = Color.Transparent
                        ),
                        shape = MaterialTheme.shapes.medium.copy(CornerSize(24.dp))
                    )
                    Spacer(modifier = Modifier.height(MaterialTheme.spacing.medium))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 56.dp),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Column(modifier = Modifier.height(120.dp)) {
                            Button(
                                modifier = Modifier
                                    .height(55.dp)
                                    .width(120.dp),
                                onClick = { answerText.value = "" },
                                colors = ButtonDefaults.buttonColors(
                                    backgroundColor = clearButtonColor,
                                    contentColor = Color.White
                                )
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
                                    stopMusicAndHideKeyboard(
                                        viewModel.audioPlayer,
                                        viewModel,
                                        keyboardController
                                    ) {
                                        vibrator?.cancel()
                                        vibrator = null
                                    }
                                    viewModel.snoozeAlarm(alarm.alarmId)
                                    resultNavigator.navigateBack(curAlarm)
                                },
                                colors = ButtonDefaults.buttonColors(
                                    backgroundColor = snoozeButtonColor,
                                    contentColor = Color.White
                                )
                            ) {
                                Text(text = "SNOOZE", fontSize = 19.sp)
                            }
                        }
                        Button(
                            modifier = Modifier
                                .height(120.dp)
                                .width(120.dp),
                            onClick = {
                                dismissAlarm(
                                    answerText,
                                    problem,
                                    viewModel.audioPlayer,
                                    keyboardController,
                                    resultNavigator,
                                    alarm,
                                    viewModel,
                                    {
                                        vibrator?.cancel()
                                        vibrator = null
                                    }
                                ) {
                                    scope.launch {
                                        scaffoldState.snackbarHostState.showSnackbar("Incorrect!")
                                    }
                                }
                            },
                            colors = ButtonDefaults.buttonColors(
                                backgroundColor = enterButtonColor,
                                contentColor = Color.White
                            )
                        ) {
                            Text(text = "ENTER", fontSize = 19.sp)
                        }
                    }
                }
            }
        }
    }
}

@ExperimentalComposeUiApi
private fun dismissAlarm(
    answerText: MutableState<String>,
    problem: MathProblem,
    mp: MediaPlayer,
    keyboardController: SoftwareKeyboardController?,
    resultNavigator: ResultBackNavigator<AlarmEntity>,
    alarm: Alarm,
    viewModel: AlarmMathViewModel,
    onStopMusic: () -> Unit,
    onWrongAnswer: () -> Unit
) {
    if (validateAnswer(answerText, problem)) {
        stopMusicAndHideKeyboard(mp, viewModel, keyboardController, onStopMusic)
        if (!alarm.repeat) {
            viewModel.completeAlarm(alarm)
        }
        if (!alarm.isSaved) {
//            navController
//                .previousBackStackEntry?.savedStateHandle?.set("testAlarmId", alarm.alarmId)
        }
        resultNavigator.navigateBack(AlarmMapper().mapFromDomainModel(alarm))
    } else {
        onWrongAnswer.invoke()
    }
}

@ExperimentalComposeUiApi
private fun stopMusicAndHideKeyboard(
    mp: MediaPlayer,
    viewModel: AlarmMathViewModel,
    keyboardController: SoftwareKeyboardController?,
    stopVibrate: () -> Unit
) {
    stopVibrate.invoke()
    mp.run {
        if (isPlaying) stop()
//        release()
    }
    viewModel.stopTimer()
    keyboardController?.hide()
}

private fun validateAnswer(
    answerText: MutableState<String>,
    problem: MathProblem
): Boolean {
    if (answerText.value.isNotBlank() && problem.answer == answerText.value.trim().toInt()) {
        answerText.value = ""
        return true
    }
    return false
}

// Creates the math problem based on the user-set difficulty
private fun getMathProblem(difficulty: Int): MathProblem {
    val problem = MathProblem()
    val random = Random
    problem.operator = random.nextInt(4)
    val add1: Int
    val add2: Int
    val mult1: Int
    val mult2: Int
    when (difficulty) {
        EASY -> {
            add1 = 90
            add2 = 10
            mult1 = 10
            mult2 = 3
        }
        HARD -> {
            add1 = 9000
            add2 = 1000
            mult1 = 14
            mult2 = 12
        }
        else -> {
            add1 = 900
            add2 = 100
            mult1 = 13
            mult2 = 3
        }
    }
    when (problem.operator) {
        ADD -> {
            problem.numOne = random.nextInt(add1) + add2
            problem.numTwo = random.nextInt(add1) + add2
            problem.answer = problem.numOne + problem.numTwo
        }
        SUBTRACT -> {
            problem.numOne = random.nextInt(add1) + add2
            problem.numTwo = random.nextInt(add1) + add2
            if (problem.numOne < problem.numTwo) {
                val temp: Int = problem.numOne
                problem.numOne = problem.numTwo
                problem.numTwo = temp
            }
            problem.answer = problem.numOne - problem.numTwo
        }
        TIMES -> {
            problem.numOne = random.nextInt(mult1) + mult2
            problem.numTwo = random.nextInt(mult1) + mult2
            problem.answer = problem.numOne * problem.numTwo
        }
        DIVIDE -> {
            problem.numOne = random.nextInt(mult1) + mult2
            problem.numTwo = random.nextInt(mult1) + mult2
            problem.answer = problem.numOne * problem.numTwo
            val tmp: Int = problem.answer
            problem.answer = problem.numOne
            problem.numOne = tmp
        }
    }
    return problem
}

private fun getMathString(problem: MathProblem): String? {
    return when (problem.operator) {
        ADD -> "${problem.numOne} + ${problem.numTwo}"
        SUBTRACT -> "${problem.numOne} - ${problem.numTwo}"
        TIMES -> "${problem.numOne} x ${problem.numTwo}"
        DIVIDE -> "${problem.numOne} / ${problem.numTwo}"
        else -> null
    }
}

data class MathProblem(
    var operator: Int = 0,
    var numOne: Int = 0,
    var numTwo: Int = 0,
    var answer: Int = 0
)

@ExperimentalComposeUiApi
@InternalCoroutinesApi
@ExperimentalMaterialApi
@Preview
@Composable
fun MathPreview() {
    MathAlarmTheme(darkTheme = true) {
//        MathScreen(
//            navController = rememberNavController(),
//            alarmId = 1L,
//            darkTheme = true
//        )
    }
}
