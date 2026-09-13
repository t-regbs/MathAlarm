package com.timilehinaregbesola.mathalarm.presentation.alarmmath

import com.timilehinaregbesola.mathalarm.domain.model.MathChallenge
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MathChallengeTest {
    @Test fun countAndInvalidSettingsAreBounded() {
        assertEquals(1, generateChallengeProblems(MathChallenge(questionCount = 0)).size)
        assertEquals(10, generateChallengeProblems(MathChallenge(questionCount = 99)).size)
        val config = MathChallenge(3, 3, "?", -1, 90).normalized()
        assertEquals("+−×÷", config.operations)
        assertEquals(0, config.additionRange)
        assertEquals(2, config.factorRange)
    }

    @Test fun customChallengesHonorOperatorsRangesAndDoNotRepeat() {
        repeat(50) { seed ->
            val problems = generateChallengeProblems(MathChallenge(3, 10, "−÷", 1, 2), Random(seed))
            assertEquals(10, problems.distinct().size)
            assertEquals(5, problems.count { it.operator == MathProblemOperator.Subtract })
            assertEquals(5, problems.count { it.operator == MathProblemOperator.Divide })
            problems.forEach {
                assertTrue(it.answer >= 0)
                when (it.operator) {
                    MathProblemOperator.Subtract -> {
                        assertTrue(it.numOne in 100..999 && it.numTwo in 100..999)
                        assertEquals(it.numOne - it.numTwo, it.answer)
                    }
                    MathProblemOperator.Divide -> {
                        assertTrue(it.numTwo in 12..25 && it.answer in 12..25)
                        assertEquals(it.numOne, it.answer * it.numTwo)
                    }
                    else -> error("Unexpected operation")
                }
            }
        }
    }

    @Test fun presetsIgnoreStoredCustomChoices() {
        val problems = generateChallengeProblems(MathChallenge(0, 10, "×", 2, 2), Random(20))
        assertEquals(4, problems.map { it.operator }.distinct().size)
        problems.filter { it.operator == MathProblemOperator.Add }.forEach {
            assertTrue(it.numOne in 10..99 && it.numTwo in 10..99)
        }
    }
}
