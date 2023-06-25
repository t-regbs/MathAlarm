package com.timilehinaregbesola.mathalarm.presentation.alarmmath

import com.timilehinaregbesola.mathalarm.presentation.alarmmath.MathProblemGenerator.ADD
import com.timilehinaregbesola.mathalarm.presentation.alarmmath.MathProblemGenerator.DIVIDE
import com.timilehinaregbesola.mathalarm.presentation.alarmmath.MathProblemGenerator.SUBTRACT
import com.timilehinaregbesola.mathalarm.presentation.alarmmath.MathProblemGenerator.TIMES
import com.timilehinaregbesola.mathalarm.utils.EASY
import com.timilehinaregbesola.mathalarm.utils.HARD
import kotlin.random.Random

data class MathProblem(
    var operator: Int = 0,
    var numOne: Int = 0,
    var numTwo: Int = 0,
    var answer: Int = 0,
)

fun buildQuestionString(problem: MathProblem): String? {
    return when (problem.operator) {
        ADD -> "${problem.numOne} + ${problem.numTwo}"
        SUBTRACT -> "${problem.numOne} - ${problem.numTwo}"
        TIMES -> "${problem.numOne} x ${problem.numTwo}"
        DIVIDE -> "${problem.numOne} / ${problem.numTwo}"
        else -> null
    }
}

// Creates the math problem based on the user-set difficulty
fun generateMathProblem(difficulty: Int): MathProblem {
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

private object MathProblemGenerator {
    const val ADD = 0
    const val SUBTRACT = 1
    const val TIMES = 2
    const val DIVIDE = 3
}
