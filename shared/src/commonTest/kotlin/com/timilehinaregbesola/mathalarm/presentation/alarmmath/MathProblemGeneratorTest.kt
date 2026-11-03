package com.timilehinaregbesola.mathalarm.presentation.alarmmath

import com.timilehinaregbesola.mathalarm.presentation.alarmmath.MathProblemOperator.Add
import com.timilehinaregbesola.mathalarm.presentation.alarmmath.MathProblemOperator.Divide
import com.timilehinaregbesola.mathalarm.presentation.alarmmath.MathProblemOperator.Subtract
import com.timilehinaregbesola.mathalarm.presentation.alarmmath.MathProblemOperator.Times
import io.kotest.matchers.ints.shouldBeGreaterThanOrEqual
import io.kotest.matchers.shouldBe
import com.timilehinaregbesola.mathalarm.domain.model.MathChallenge
import kotlin.random.Random
import kotlin.test.assertTrue
import kotlin.test.Test

class MathProblemGeneratorTest {

    @Test
    fun `broad generated questions agree with independently evaluated displayed arithmetic`() {
        val operations = MathChallenge.ALL_OPERATIONS
        val configs = (0..2).map { MathChallenge(difficulty = it, questionCount = 10) } +
            (1 until (1 shl operations.length)).flatMap { mask ->
                (0..2).flatMap { addition -> (0..2).map { factors ->
                    MathChallenge(difficulty = 3, questionCount = 10,
                        operations = operations.filterIndexed { index, _ -> mask and (1 shl index) != 0 },
                        additionRange = addition, factorRange = factors)
                } }
            } + listOf(MathChallenge(questionCount = 10, difficultyMix = "0001112222"))
        configs.forEachIndexed { configIndex, config ->
            repeat(100) { seed ->
                generateChallengeProblems(config, Random(configIndex * 100 + seed)).forEach { problem ->
                    val rendered = buildQuestionString(problem).split(" ")
                    val first = rendered[0].toLong()
                    val second = rendered[2].toLong()
                    val evaluated = when (rendered[1]) {
                        "+" -> first + second
                        "-" -> first - second
                        "x" -> first * second
                        "/" -> { assertTrue(second != 0L && first % second == 0L); first / second }
                        else -> error("Unknown displayed operator")
                    }
                    evaluated shouldBe problem.answer.toLong()
                    assertTrue(evaluated >= 0 && evaluated.toString().length <= 8)
                    parseChallengeAnswer(evaluated.toString()) shouldBe problem.answer
                }
            }
        }
    }

    @Test
    fun `presets generate every operator with valid answers and operand ranges`() {
        val ranges = listOf(10..99 to 3..12, 100..999 to 3..15, 1000..9999 to 12..25)
        ranges.forEachIndexed { difficulty, (additionRange, factorRange) ->
            val problems = generateChallengeProblems(
                MathChallenge(difficulty = difficulty, questionCount = 4),
                Random(difficulty),
            )
            problems.map { it.operator }.toSet() shouldBe MathProblemOperator.entries.toSet()
            problems.forEach { problem ->
                val range = if (problem.operator == Add || problem.operator == Subtract) additionRange else factorRange
                val firstOperand = if (problem.operator == Divide) problem.answer else problem.numOne
                assertTrue(firstOperand in range)
                assertTrue(problem.numTwo in range)
                val expected = when (problem.operator) {
                    Add -> problem.numOne + problem.numTwo
                    Subtract -> problem.numOne - problem.numTwo
                    Times -> problem.numOne * problem.numTwo
                    Divide -> {
                        (problem.numOne % problem.numTwo) shouldBe 0
                        problem.numOne / problem.numTwo
                    }
                }
                problem.answer shouldBe expected
                problem.answer shouldBeGreaterThanOrEqual 0
            }
        }
    }

    @Test
    fun `buildQuestionString should format addition correctly`() {
        val problem = MathProblem(
            operator = Add,
            numOne = 10,
            numTwo = 20,
            answer = 30
        )
        
        val questionString = buildQuestionString(problem)
        questionString shouldBe "10 + 20"
    }

    @Test
    fun `buildQuestionString should format subtraction correctly`() {
        val problem = MathProblem(
            operator = Subtract,
            numOne = 50,
            numTwo = 20,
            answer = 30
        )
        
        val questionString = buildQuestionString(problem)
        questionString shouldBe "50 - 20"
    }

    @Test
    fun `buildQuestionString should format multiplication correctly`() {
        val problem = MathProblem(
            operator = Times,
            numOne = 5,
            numTwo = 6,
            answer = 30
        )
        
        val questionString = buildQuestionString(problem)
        questionString shouldBe "5 x 6"
    }

    @Test
    fun `buildQuestionString should format division correctly`() {
        val problem = MathProblem(
            operator = Divide,
            numOne = 30,
            numTwo = 6,
            answer = 5
        )
        val questionString = buildQuestionString(problem)
        questionString shouldBe "30 / 6"
    }

}
