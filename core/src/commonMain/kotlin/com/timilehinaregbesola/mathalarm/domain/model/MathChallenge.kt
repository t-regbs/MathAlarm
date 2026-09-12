package com.timilehinaregbesola.mathalarm.domain.model

/** Range indices retain the existing Easy / Medium / Hard operand bounds. */
data class MathChallenge(
    val difficulty: Int = 0,
    val questionCount: Int = 1,
    val operations: String = "+−×÷",
    val additionRange: Int = 0,
    val factorRange: Int = 0,
    val difficultyMix: String = "",
) {
    /** One preset digit per question, stored easiest first; empty means a single preset/custom setup. */
    val mixedDifficulties: List<Int>
        get() = difficultyMix
            .mapNotNull { digit ->
                digit.digitToIntOrNull()?.takeIf { it in 0 until CUSTOM }
            }
            .sorted()
            .take(MAX_QUESTIONS)

    fun normalized(): MathChallenge {
        val mix = if (difficulty.coerceIn(0, CUSTOM) == CUSTOM) emptyList() else mixedDifficulties
        return copy(
            difficulty = difficulty.coerceIn(0, CUSTOM),
            questionCount = mix.size.takeIf { it > 0 } ?: questionCount.coerceIn(1, MAX_QUESTIONS),
            difficultyMix = mix.joinToString(""),
            operations = ALL_OPERATIONS.filter { it in operations }.ifEmpty { ALL_OPERATIONS },
            additionRange = additionRange.coerceIn(ADDITION_RANGES.indices),
            factorRange = factorRange.coerceIn(FACTOR_RANGES.indices),
        )
    }

    companion object {
        const val CUSTOM = 3
        const val MAX_QUESTIONS = 10
        const val ALL_OPERATIONS = "+−×÷"
        val ADDITION_RANGES = listOf(10..99, 100..999, 1000..9999)
        val FACTOR_RANGES = listOf(3..12, 3..15, 12..25)
    }
}

val Alarm.mathChallenge: MathChallenge
    get() = MathChallenge(
        difficulty = difficulty,
        questionCount = questionCount,
        operations = challengeOperations,
        additionRange = additionRange,
        factorRange = factorRange,
        difficultyMix = difficultyMix
    ).normalized()
