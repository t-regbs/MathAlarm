package com.timilehinaregbesola.mathalarm.presentation.alarmmath

import com.russhwolf.settings.Settings
import com.timilehinaregbesola.mathalarm.domain.model.MathChallenge
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/** Keeps each alarm's latest occurrence separate, including simultaneous ringing alarms. */
class ChallengeProgressStore(private val settings: Settings) {
    @Serializable
    data class Progress(val activeAt: Long, val problems: List<MathProblem>, val questionIndex: Int)

    fun load(alarmId: Long, activeAt: Long): Progress? {
        val encoded = settings.getStringOrNull(key(alarmId)) ?: return null
        val progress = try {
            Json.decodeFromString<Progress>(encoded)
        } catch (_: IllegalArgumentException) {
            null
        }
        return progress?.takeIf {
            it.activeAt == activeAt && it.problems.size in 1..MathChallenge.MAX_QUESTIONS &&
                it.questionIndex in it.problems.indices
        }
    }

    fun save(alarmId: Long, progress: Progress) {
        settings.putString(key(alarmId), Json.encodeToString(progress))
    }

    fun clear(alarmId: Long, activeAt: Long) {
        if (load(alarmId, activeAt) != null) settings.remove(key(alarmId))
    }

    private fun key(alarmId: Long) = "mathalarm_challenge_progress_$alarmId"
}
