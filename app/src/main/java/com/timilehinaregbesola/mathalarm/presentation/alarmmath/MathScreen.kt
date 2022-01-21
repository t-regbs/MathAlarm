package com.timilehinaregbesola.mathalarm.presentation.alarmmath

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.net.Uri
import android.os.Build
import android.os.VibrationEffect
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
import androidx.navigation.NavHostController
import com.timilehinaregbesola.mathalarm.domain.model.Alarm
import com.timilehinaregbesola.mathalarm.presentation.alarmlist.AlarmListViewModel
import com.timilehinaregbesola.mathalarm.presentation.components.AlarmSnack
import com.timilehinaregbesola.mathalarm.presentation.ui.*
import com.timilehinaregbesola.mathalarm.utils.EASY
import com.timilehinaregbesola.mathalarm.utils.HARD
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.coroutines.launch
import timber.log.Timber
import java.io.IOException
import kotlin.random.Random

// import org.koin.androidx.compose

private const val ADD = 0
private const val SUBTRACT = 1
private const val TIMES = 2
private const val DIVIDE = 3
var vibrateRunning = false

@ExperimentalMaterialApi
@InternalCoroutinesApi
@ExperimentalComposeUiApi
@Composable
fun MathScreen(
    navController: NavHostController,
    alarmId: Long,
    viewModel: AlarmListViewModel = hiltViewModel(),
) {
    BackHandler { }
    val settingsId = 1143682591
    val alarm = viewModel.retrieveAlarm(alarmId)
    val context = LocalContext.current
    alarm?.let {
        if (alarm.alarmTone.isNotEmpty()) {
            val alarmUri = Uri.parse(alarm.alarmTone)
            println(navController.previousBackStackEntry?.destination?.id)
            if (navController.previousBackStackEntry?.destination?.id == settingsId) {
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
            }
        } else {
            Timber.d("Tone not available")
//            Toast.makeText(
//                activity, getString(R.string.tone_not_available),
//                Toast.LENGTH_SHORT
//            ).show()
        }

        // Vibrate phone
        if (alarm.vibrate) {
            vibrateRunning = true
            val thread = Thread(
                Runnable {
                    while (vibrateRunning) {
                        val v =
                            context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
                        if (Build.VERSION.SDK_INT >= 26) {
                            v.vibrate(VibrationEffect.createOneShot(1000, 10))
                        } else {
                            @Suppress("DEPRECATION")
                            v.vibrate(1000)
                        }
                        try {
                            Thread.sleep(5000)
                        } catch (e: InterruptedException) {
                        }
                    }
                    if (!vibrateRunning) {
                        return@Runnable
                    }
                }
            )
            thread.start()
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
                    .padding(vertical = 24.dp)
            ) {
                Column {
                    val toneState = viewModel.state.observeAsState()
                    val answerText = remember { mutableStateOf("") }
                    val keyboardController = LocalSoftwareKeyboardController.current
                    val maxChar = 8

                    val progress = remember { mutableStateOf(0.1f) }
                    val animatedProgress = animateFloatAsState(
                        targetValue = progress.value,
                        animationSpec = ProgressIndicatorDefaults.ProgressAnimationSpec
                    ).value
                    val showSnack = remember { mutableStateOf(false) }

                    Spacer(modifier = Modifier.height(24.dp))
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
                                .padding(horizontal = 24.dp),
                            color = indicatorColor,
                            progress = animatedProgress
                        )
                    }
                    Spacer(modifier = Modifier.height(32.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Text(
                            text = question.value ?: "",
                            fontSize = 70.sp,
                            color = Color(0xFF272727),
                            fontWeight = FontWeight.Bold
                        )
                    }
                    Spacer(modifier = Modifier.height(16.dp))
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
                                    navController,
                                    alarm,
                                    viewModel
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
                            backgroundColor = unSelectedDay,
                            focusedIndicatorColor = Color.Transparent,
                            unfocusedIndicatorColor = Color.Transparent,
                            disabledIndicatorColor = Color.Transparent
                        ),
                        shape = MaterialTheme.shapes.medium.copy(CornerSize(24.dp))
                    )
                    Spacer(modifier = Modifier.height(16.dp))
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
                                    stopMusicAndHideKeyboard(viewModel.audioPlayer, viewModel, keyboardController)
                                    viewModel.snoozeAlarm(alarmId)
                                    navController.popBackStack()
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
                                    navController,
                                    alarm,
                                    viewModel
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
    navController: NavHostController,
    alarm: Alarm,
    viewModel: AlarmListViewModel,
    onWrongAnswer: () -> Unit
) {
    if (validateAnswer(answerText, problem)) {
        stopMusicAndHideKeyboard(mp, viewModel, keyboardController)
        navController
            .previousBackStackEntry?.savedStateHandle?.set("testAlarmId", alarm.alarmId)
        navController.popBackStack()
    } else {
        onWrongAnswer.invoke()
    }
}

@OptIn(ExperimentalComposeUiApi::class)
private fun stopMusicAndHideKeyboard(
    mp: MediaPlayer,
    viewModel: AlarmListViewModel,
    keyboardController: SoftwareKeyboardController?
) {
    vibrateRunning = false
    mp.run {
        if (isPlaying) stop()
//        release()
    }
    viewModel.stopTimer()
//                        TODO: cancel notification
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

sealed class ToneState(val total: Int) {
    class Stopped(seconds: Int = 0) : ToneState(seconds)
    class Countdown(total: Int, val seconds: Int) : ToneState(total)
}

@Preview
@Composable
fun MathScreenPreview() {
    MathAlarmTheme {
//        MathScreen(
//            rememberNavController(),
//            1L,
//            viewModel
//        )
    }
}
