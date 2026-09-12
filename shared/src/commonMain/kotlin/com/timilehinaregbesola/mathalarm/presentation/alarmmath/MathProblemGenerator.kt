package com.timilehinaregbesola.mathalarm.presentation.alarmmath

import com.timilehinaregbesola.mathalarm.presentation.alarmmath.MathProblemOperator.Add
import com.timilehinaregbesola.mathalarm.presentation.alarmmath.MathProblemOperator.Divide
import com.timilehinaregbesola.mathalarm.presentation.alarmmath.MathProblemOperator.Subtract
import com.timilehinaregbesola.mathalarm.presentation.alarmmath.MathProblemOperator.Times
import kotlin.random.Random
import com.timilehinaregbesola.mathalarm.domain.model.MathChallenge

data class MathProblem(
    val operator: MathProblemOperator = Add,
    val numOne: Int = 0,
    val numTwo: Int = 0,
    val answer: Int = 0,
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

/** One shared generator for the editor example, Test Alarm, and scheduled alarms. */
fun generateChallengeProblems(
    challenge: MathChallenge,
    random: Random = Random.Default,
): List<MathProblem> {
    val config = challenge.normalized()
    val problems = mutableListOf<MathProblem>()
    if (config.difficultyMix.isEmpty()) {
        appendProblems(config, random, problems)
    } else {
        config.mixedDifficulties.groupingBy { it }.eachCount().forEach { (level, count) ->
            appendProblems(config.copy(difficulty = level, questionCount = count), random, problems)
        }
    }
    return problems
}

private fun appendProblems(
    config: MathChallenge,
    random: Random,
    problems: MutableList<MathProblem>,
) {
    val custom = config.difficulty == MathChallenge.CUSTOM
    val addition = MathChallenge.ADDITION_RANGES[if (custom) config.additionRange else config.difficulty]
    val factors = MathChallenge.FACTOR_RANGES[if (custom) config.factorRange else config.difficulty]
    val operators = (if (custom) config.operations else MathChallenge.ALL_OPERATIONS).map {
        when (it) {
            '+' -> Add
            '−' -> Subtract
            '×' -> Times
            else -> Divide
        }
    }
    val cycle = mutableListOf<MathProblemOperator>()
    repeat(config.questionCount) {
        if (cycle.isEmpty()) cycle.addAll(operators.shuffled(random))
        val operator = cycle.removeAt(0)
        val range = if (operator == Add || operator == Subtract) addition else factors
        var problem = generateProblem(operator, range, random)
        while (problem in problems) problem = generateProblem(operator, range, random)
        problems.add(problem)
    }
}

private fun generateProblem(operator: MathProblemOperator, range: IntRange, random: Random): MathProblem {
    val a = random.nextInt(range.first, range.last + 1)
    val b = random.nextInt(range.first, range.last + 1)
    return when (operator) {
        Add -> MathProblem(operator, a, b, a + b)
        Subtract -> MathProblem(operator, maxOf(a, b), minOf(a, b), kotlin.math.abs(a - b))
        Times -> MathProblem(operator, a, b, a * b)
        Divide -> MathProblem(operator, a * b, b, a)
    }
}
