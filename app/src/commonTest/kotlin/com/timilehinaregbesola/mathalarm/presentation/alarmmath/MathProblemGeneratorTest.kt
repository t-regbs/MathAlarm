package com.timilehinaregbesola.mathalarm.presentation.alarmmath

import com.timilehinaregbesola.mathalarm.presentation.alarmmath.MathProblemOperator.Add
import com.timilehinaregbesola.mathalarm.presentation.alarmmath.MathProblemOperator.Divide
import com.timilehinaregbesola.mathalarm.presentation.alarmmath.MathProblemOperator.Subtract
import com.timilehinaregbesola.mathalarm.presentation.alarmmath.MathProblemOperator.Times
import com.timilehinaregbesola.mathalarm.utils.EASY
import com.timilehinaregbesola.mathalarm.utils.HARD
import com.timilehinaregbesola.mathalarm.utils.MEDIUM
import io.kotest.matchers.ints.shouldBeGreaterThan
import io.kotest.matchers.ints.shouldBeGreaterThanOrEqual
import io.kotest.matchers.ints.shouldBeLessThan
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class MathProblemGeneratorTest {

    @Test
    fun `generateMathProblem with EASY difficulty should create valid addition problem`() {
        val difficulty = EASY
        
        repeat(10) {
            val problem = generateMathProblem(difficulty)
            
            if (problem.operator == Add) {
                val expectedAnswer = problem.numOne + problem.numTwo
                problem.answer shouldBe expectedAnswer
                
                problem.numOne shouldBeGreaterThanOrEqual 10
                problem.numOne shouldBeLessThan 100
                problem.numTwo shouldBeGreaterThanOrEqual 10
                problem.numTwo shouldBeLessThan 100
            }
        }
    }

    @Test
    fun `generateMathProblem with EASY difficulty should create valid subtraction problem`() {
        val difficulty = EASY
        
        repeat(10) {
            val problem = generateMathProblem(difficulty)

            with(problem) {
                if (operator == Subtract) {
                    val expectedAnswer = numOne - numTwo
                    answer shouldBe expectedAnswer
                    answer shouldBeGreaterThanOrEqual 0
                    numOne shouldBeGreaterThanOrEqual numTwo
                }
            }
        }
    }

    @Test
    fun `generateMathProblem with EASY difficulty should create valid multiplication problem`() {
        val difficulty = EASY
        
        repeat(10) {
            val problem = generateMathProblem(difficulty)

            with(problem) {
                if (operator == Times) {
                    val expectedAnswer = numOne * numTwo
                    answer shouldBe expectedAnswer

                    // Verify numbers are within EASY multiplication range (3-13)
                    numOne shouldBeGreaterThanOrEqual 3
                    numOne shouldBeLessThan 13
                    numTwo shouldBeGreaterThanOrEqual 3
                    numTwo shouldBeLessThan 13
                }
            }
        }
    }

    @Test
    fun `generateMathProblem with EASY difficulty should create valid division problem`() {
        val difficulty = EASY
        
        repeat(10) {
            val problem = generateMathProblem(difficulty)

            with(problem) {
                if (operator == Divide) {
                    val expectedResult = numOne / numTwo
                    answer shouldBe expectedResult
                    (numOne % numTwo) shouldBe 0
                    numTwo shouldBeGreaterThan 0  // No division by zero
                }
            }
        }
    }

    @Test
    fun `generateMathProblem with MEDIUM difficulty should create harder problems`() {
        val difficulty = MEDIUM
        
        repeat(10) {
            val problem = generateMathProblem(difficulty)
            
            when (problem.operator) {
                Add -> {
                    problem.answer shouldBe (problem.numOne + problem.numTwo)
                    // MEDIUM range: 100-1000
                    problem.numOne shouldBeGreaterThanOrEqual 100
                    problem.numOne shouldBeLessThan 1000
                }
                Subtract -> {
                    problem.answer shouldBe (problem.numOne - problem.numTwo)
                    problem.answer shouldBeGreaterThanOrEqual 0
                    problem.numOne shouldBeGreaterThanOrEqual problem.numTwo
                }
                Times -> {
                    problem.answer shouldBe (problem.numOne * problem.numTwo)
                    // MEDIUM multiplication range: 3-16
                    problem.numOne shouldBeGreaterThanOrEqual 3
                    problem.numOne shouldBeLessThan 16
                }
                Divide -> {
                    problem.answer shouldBe (problem.numOne / problem.numTwo)
                    (problem.numOne % problem.numTwo) shouldBe 0
                    problem.numTwo shouldBeGreaterThan 0
                }
            }
        }
    }

    @Test
    fun `generateMathProblem with HARD difficulty should create challenging problems`() {
        val difficulty = HARD
        
        repeat(10) {
            val problem = generateMathProblem(difficulty)
            
            when (problem.operator) {
                Add -> {
                    problem.answer shouldBe (problem.numOne + problem.numTwo)
                    // HARD range: 1000-10000
                    problem.numOne shouldBeGreaterThanOrEqual 1000
                    problem.numOne shouldBeLessThan 10000
                }
                Subtract -> {
                    problem.answer shouldBe (problem.numOne - problem.numTwo)
                    problem.answer shouldBeGreaterThanOrEqual 0
                }
                Times -> {
                    problem.answer shouldBe (problem.numOne * problem.numTwo)
                    // HARD multiplication range: 12-26
                    problem.numOne shouldBeGreaterThanOrEqual 12
                    problem.numOne shouldBeLessThan 26
                }
                Divide -> {
                    problem.answer shouldBe (problem.numOne / problem.numTwo)
                    (problem.numOne % problem.numTwo) shouldBe 0
                }
            }
        }
    }

    @Test
    fun `generateMathProblem should create problems with all operators over multiple generations`() {
        val difficulty = MEDIUM
        val operators = mutableSetOf<MathProblemOperator>()
        
        repeat(100) {
            val problem = generateMathProblem(difficulty)
            operators.add(problem.operator)
        }
        
        operators.size shouldBeGreaterThan 1 // At least 2 different operators
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

    @Test
    fun `generateMathProblem should never divide by zero`() {
        val difficulties = listOf(EASY, MEDIUM, HARD)
        difficulties.forEach { difficulty ->
            repeat(50) {
                val problem = generateMathProblem(difficulty)
                
                if (problem.operator == Divide) {
                    problem.numTwo shouldBeGreaterThan 0
                }
            }
        }
    }
}
