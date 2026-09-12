package com.timilehinaregbesola.mathalarm.presentation.alarmmath

import com.timilehinaregbesola.mathalarm.domain.model.Alarm
import com.timilehinaregbesola.mathalarm.domain.model.MathChallenge
import com.timilehinaregbesola.mathalarm.domain.model.mathChallenge
import com.timilehinaregbesola.mathalarm.framework.database.AlarmEntity
import com.timilehinaregbesola.mathalarm.framework.database.AlarmMapper
import kotlinx.serialization.json.Json
import kotlin.random.Random
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

class MixedDifficultyTest {
    @Test fun mixDeterminesTotalAndRejectsInvalidLevels() {
        val config = MathChallenge(questionCount = 9, difficultyMix = "2?1009").normalized()
        assertEquals("0012", config.difficultyMix)
        assertEquals(4, config.questionCount)
        assertEquals(10, MathChallenge(difficultyMix = "2".repeat(99)).normalized().questionCount)
        assertEquals("", MathChallenge(difficulty = 3, difficultyMix = "012").normalized().difficultyMix)
        assertEquals(1, MathChallenge(questionCount = 0, difficultyMix = "bad").normalized().questionCount)
    }

    @Test fun generatesRequestedPresetCountsEasiestFirst() {
        val config = MathChallenge(difficultyMix = "221100").normalized()
        repeat(30) { seed ->
            val problems = generateChallengeProblems(config, Random(seed))
            assertEquals(6, problems.size)
            assertEquals(problems.size, problems.distinct().size)
            problems.zip(config.mixedDifficulties).forEach { (problem, level) ->
                val operands = when (problem.operator) {
                    MathProblemOperator.Add, MathProblemOperator.Subtract -> MathChallenge.ADDITION_RANGES[level]
                    else -> MathChallenge.FACTOR_RANGES[level]
                }
                val firstOperand = if (problem.operator == MathProblemOperator.Divide) problem.answer else problem.numOne
                assertTrue(firstOperand in operands)
                assertTrue(problem.numTwo in operands)
            }
        }
    }

    @Test fun notificationAndDatabaseMappingPreserveMix() {
        val mapper = AlarmMapper()
        val alarm = Alarm(alarmId = 73, difficultyMix = "00112")
        val restored = mapper.mapToDomainModel(Json.decodeFromString<AlarmEntity>(Json.encodeToString(mapper.mapFromDomainModel(alarm))))
        assertEquals(alarm.mathChallenge, restored.mathChallenge)
        assertEquals(5, restored.questionCount)
    }
}
