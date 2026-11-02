package com.timilehinaregbesola.mathalarm.presentation.alarmmath

import com.timilehinaregbesola.mathalarm.domain.model.Alarm
import com.timilehinaregbesola.mathalarm.utils.AlarmErrorMessage

enum class ChallengeReadiness { IDLE, INITIALIZING, READY, ERROR, RESOLVED }
data class ChallengeState(
    val readiness: ChallengeReadiness = ChallengeReadiness.IDLE,
    val alarm: Alarm? = null,
    val occurrenceId: String? = null,
    val preview: Boolean = false,
    val problems: List<MathProblem> = emptyList(),
    val questionIndex: Int = 0,
    val answerText: String = "",
    val startedAt: Long = 0,
    val incorrectAnswers: Int = 0,
    val finishing: Boolean = false,
    val error: AlarmErrorMessage? = null,
    val results: List<ChallengeResult> = emptyList(),
) {
    val currentProblem: MathProblem? get() = problems.getOrNull(questionIndex)
    val questionCount: Int get() = problems.size
}
data class ChallengeResult(val id: Long, val outcome: ChallengeOutcome)
sealed class ChallengeOutcome {
    data class Failure(val error: AlarmErrorMessage) : ChallengeOutcome()
    data object Completed : ChallengeOutcome()
    data object Snoozed : ChallengeOutcome()
}
