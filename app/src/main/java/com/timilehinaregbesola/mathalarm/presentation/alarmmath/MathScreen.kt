package com.timilehinaregbesola.mathalarm.presentation.alarmmath

import android.media.MediaPlayer
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CornerSize
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavHostController
import com.timilehinaregbesola.mathalarm.presentation.alarmlist.AlarmListViewModel
import com.timilehinaregbesola.mathalarm.presentation.ui.*
import com.timilehinaregbesola.mathalarm.utils.EASY
import com.timilehinaregbesola.mathalarm.utils.HARD
import kotlin.random.Random

private const val ADD = 0
private const val SUBTRACT = 1
private const val TIMES = 2
private const val DIVIDE = 3

@ExperimentalComposeUiApi
@Composable
fun MathScreen(
    navController: NavHostController,
    alarmId: Long,
    viewModel: AlarmListViewModel
) {
    val alarm = viewModel.retrieveAlarm(alarmId)
    val question: MutableState<String?> = remember { mutableStateOf(null) }
    val mp = MediaPlayer()
    var vibrateRunning = false

    alarm?.let {
        // Get difficulty
        val problem = remember { getMathProblem(it.difficulty) }

        // Initialize the buttons and the on click actions
        question.value = getMathString(problem)

        Surface(
            Modifier
                .fillMaxSize()
                .padding(vertical = 24.dp)
        ) {
            Column {
                val answerText = remember { mutableStateOf("") }
                val keyboardController = LocalSoftwareKeyboardController.current
                val maxChar = 8
                Spacer(modifier = Modifier.height(24.dp))
                LinearProgressIndicator(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(10.dp)
                        .padding(horizontal = 24.dp),
                    color = indicatorColor,
                    progress = 0.7f
                )
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
                            if (validateAnswer(answerText, problem)) {
                                keyboardController?.hide()
                                navController
                                    .previousBackStackEntry?.savedStateHandle?.set("testAlarmId", alarm.alarmId)
                                navController.popBackStack()
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
                            onClick = { /*TODO*/ },
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
                            if (validateAnswer(answerText, problem)) {
                                keyboardController?.hide()
                                navController
                                    .previousBackStackEntry?.savedStateHandle?.set("testAlarmId", alarm.alarmId)
                                navController.popBackStack()
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
