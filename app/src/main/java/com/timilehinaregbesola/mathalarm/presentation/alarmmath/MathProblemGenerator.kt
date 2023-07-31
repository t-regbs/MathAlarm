package com.timilehinaregbesola.mathalarm.presentation.alarmmath

import com.timilehinaregbesola.mathalarm.presentation.alarmmath.MathProblemGenerator.EasyAddSubLimitOne
import com.timilehinaregbesola.mathalarm.presentation.alarmmath.MathProblemGenerator.EasyAddSubLimitTwo
import com.timilehinaregbesola.mathalarm.presentation.alarmmath.MathProblemGenerator.EasyMultiDivLimitOne
import com.timilehinaregbesola.mathalarm.presentation.alarmmath.MathProblemGenerator.EasyMultiDivLimitTwo
import com.timilehinaregbesola.mathalarm.presentation.alarmmath.MathProblemGenerator.HardAddSubLimitOne
import com.timilehinaregbesola.mathalarm.presentation.alarmmath.MathProblemGenerator.HardAddSubLimitTwo
import com.timilehinaregbesola.mathalarm.presentation.alarmmath.MathProblemGenerator.HardMultiDivLimitOne
import com.timilehinaregbesola.mathalarm.presentation.alarmmath.MathProblemGenerator.HardMultiDivLimitTwo
import com.timilehinaregbesola.mathalarm.presentation.alarmmath.MathProblemGenerator.NormalAddSubLimitOne
import com.timilehinaregbesola.mathalarm.presentation.alarmmath.MathProblemGenerator.NormalAddSubLimitTwo
import com.timilehinaregbesola.mathalarm.presentation.alarmmath.MathProblemGenerator.NormalMultiDivLimitOne
import com.timilehinaregbesola.mathalarm.presentation.alarmmath.MathProblemGenerator.NormalMultiDivLimitTwo
import com.timilehinaregbesola.mathalarm.presentation.alarmmath.MathProblemOperator.Add
import com.timilehinaregbesola.mathalarm.presentation.alarmmath.MathProblemOperator.Divide
import com.timilehinaregbesola.mathalarm.presentation.alarmmath.MathProblemOperator.Subtract
import com.timilehinaregbesola.mathalarm.presentation.alarmmath.MathProblemOperator.Times
import com.timilehinaregbesola.mathalarm.utils.EASY
import com.timilehinaregbesola.mathalarm.utils.HARD
import kotlin.random.Random

data class MathProblem(
    var operator: MathProblemOperator = Add,
    var numOne: Int = 0,
    var numTwo: Int = 0,
    var answer: Int = 0,
)

enum class MathProblemOperator {
    Add, Subtract, Times, Divide
}

fun buildQuestionString(problem: MathProblem): String {
    return when (problem.operator) {
        Add -> "${problem.numOne} + ${problem.numTwo}"
        Subtract -> "${problem.numOne} - ${problem.numTwo}"
        Times -> "${problem.numOne} x ${problem.numTwo}"
        Divide -> "${problem.numOne} / ${problem.numTwo}"
    }
}

// Creates the math problem based on the user-set difficulty
fun generateMathProblem(difficulty: Int): MathProblem {
    val problem = MathProblem()
    val random = Random
    problem.operator = MathProblemOperator.values().random()
    val add1: Int
    val add2: Int
    val mult1: Int
    val mult2: Int
    when (difficulty) {
        EASY -> {
            add1 = EasyAddSubLimitOne
            add2 = EasyAddSubLimitTwo
            mult1 = EasyMultiDivLimitOne
            mult2 = EasyMultiDivLimitTwo
        }
        HARD -> {
            add1 = HardAddSubLimitOne
            add2 = HardAddSubLimitTwo
            mult1 = HardMultiDivLimitOne
            mult2 = HardMultiDivLimitTwo
        }
        else -> {
            add1 = NormalAddSubLimitOne
            add2 = NormalAddSubLimitTwo
            mult1 = NormalMultiDivLimitOne
            mult2 = NormalMultiDivLimitTwo
        }
    }
    when (problem.operator) {
        Add -> {
            problem.numOne = random.nextInt(add1) + add2
            problem.numTwo = random.nextInt(add1) + add2
            problem.answer = problem.numOne + problem.numTwo
        }
        Subtract -> {
            problem.numOne = random.nextInt(add1) + add2
            problem.numTwo = random.nextInt(add1) + add2
            if (problem.numOne < problem.numTwo) {
                val temp: Int = problem.numOne
                problem.numOne = problem.numTwo
                problem.numTwo = temp
            }
            problem.answer = problem.numOne - problem.numTwo
        }
        Times -> {
            problem.numOne = random.nextInt(mult1) + mult2
            problem.numTwo = random.nextInt(mult1) + mult2
            problem.answer = problem.numOne * problem.numTwo
        }
        Divide -> {
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

private object MathProblemGenerator {
    const val EasyAddSubLimitOne = 90
    const val EasyAddSubLimitTwo = 10
    const val EasyMultiDivLimitOne = 10
    const val EasyMultiDivLimitTwo = 3
    const val HardAddSubLimitOne = 9000
    const val HardAddSubLimitTwo = 1000
    const val HardMultiDivLimitOne = 14
    const val HardMultiDivLimitTwo = 12
    const val NormalAddSubLimitOne = 900
    const val NormalAddSubLimitTwo = 100
    const val NormalMultiDivLimitOne = 13
    const val NormalMultiDivLimitTwo = 3
}
