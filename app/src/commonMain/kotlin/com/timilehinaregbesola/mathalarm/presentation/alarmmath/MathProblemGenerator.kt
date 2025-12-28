package com.timilehinaregbesola.mathalarm.presentation.alarmmath

import com.timilehinaregbesola.mathalarm.presentation.alarmmath.MathProblemGenerator.EASY_ADD_SUB_LIMIT_ONE
import com.timilehinaregbesola.mathalarm.presentation.alarmmath.MathProblemGenerator.EASY_ADD_SUB_LIMIT_TWO
import com.timilehinaregbesola.mathalarm.presentation.alarmmath.MathProblemGenerator.EASY_MULTI_DIV_LIMIT_ONE
import com.timilehinaregbesola.mathalarm.presentation.alarmmath.MathProblemGenerator.EASY_MULTI_DIV_LIMIT_TWO
import com.timilehinaregbesola.mathalarm.presentation.alarmmath.MathProblemGenerator.HARD_ADD_SUB_LIMIT_ONE
import com.timilehinaregbesola.mathalarm.presentation.alarmmath.MathProblemGenerator.HARD_ADD_SUB_LIMIT_TWO
import com.timilehinaregbesola.mathalarm.presentation.alarmmath.MathProblemGenerator.HARD_MULTI_DIV_LIMIT_ONE
import com.timilehinaregbesola.mathalarm.presentation.alarmmath.MathProblemGenerator.HARD_MULTI_DIV_LIMIT_TWO
import com.timilehinaregbesola.mathalarm.presentation.alarmmath.MathProblemGenerator.NORMAL_ADD_SUB_LIMIT_ONE
import com.timilehinaregbesola.mathalarm.presentation.alarmmath.MathProblemGenerator.NORMAL_ADD_SUB_LIMIT_TWO
import com.timilehinaregbesola.mathalarm.presentation.alarmmath.MathProblemGenerator.NORMAL_MULTI_DIV_LIMIT_ONE
import com.timilehinaregbesola.mathalarm.presentation.alarmmath.MathProblemGenerator.NORMAL_MULTI_DIV_LIMIT_TWO
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
            add1 = EASY_ADD_SUB_LIMIT_ONE
            add2 = EASY_ADD_SUB_LIMIT_TWO
            mult1 = EASY_MULTI_DIV_LIMIT_ONE
            mult2 = EASY_MULTI_DIV_LIMIT_TWO
        }
        HARD -> {
            add1 = HARD_ADD_SUB_LIMIT_ONE
            add2 = HARD_ADD_SUB_LIMIT_TWO
            mult1 = HARD_MULTI_DIV_LIMIT_ONE
            mult2 = HARD_MULTI_DIV_LIMIT_TWO
        }
        else -> {
            add1 = NORMAL_ADD_SUB_LIMIT_ONE
            add2 = NORMAL_ADD_SUB_LIMIT_TWO
            mult1 = NORMAL_MULTI_DIV_LIMIT_ONE
            mult2 = NORMAL_MULTI_DIV_LIMIT_TWO
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
    const val EASY_ADD_SUB_LIMIT_ONE = 90
    const val EASY_ADD_SUB_LIMIT_TWO = 10
    const val EASY_MULTI_DIV_LIMIT_ONE = 10
    const val EASY_MULTI_DIV_LIMIT_TWO = 3
    const val HARD_ADD_SUB_LIMIT_ONE = 9000
    const val HARD_ADD_SUB_LIMIT_TWO = 1000
    const val HARD_MULTI_DIV_LIMIT_ONE = 14
    const val HARD_MULTI_DIV_LIMIT_TWO = 12
    const val NORMAL_ADD_SUB_LIMIT_ONE = 900
    const val NORMAL_ADD_SUB_LIMIT_TWO = 100
    const val NORMAL_MULTI_DIV_LIMIT_ONE = 13
    const val NORMAL_MULTI_DIV_LIMIT_TWO = 3
}
