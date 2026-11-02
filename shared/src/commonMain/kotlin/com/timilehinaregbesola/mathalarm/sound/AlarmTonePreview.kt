package com.timilehinaregbesola.mathalarm.sound

import com.timilehinaregbesola.mathalarm.platform.previewOwnedAlarmTone
import com.timilehinaregbesola.mathalarm.platform.stopOwnedAlarmTonePreview

enum class TonePreviewResult { FINISHED, STOPPED, REPLACED, UNAVAILABLE, BLOCKED_BY_ALARM, INTERRUPTED_BY_ALARM }

/** Narrow native API. A picker can stop its own tone audition only, never real alarm audio. */
object AlarmTonePreview {
    fun play(ownerId: String, tone: String, onFinished: (TonePreviewResult) -> Unit) {
        require(ownerId.isNotBlank())
        previewOwnedAlarmTone(ownerId, tone, onFinished)
    }

    fun stop(ownerId: String) = stopOwnedAlarmTonePreview(ownerId)
}

/** The platform manager owns this lease alongside its player. Late cleanup is owner-scoped. */
internal class TonePreviewOwnership {
    private var owner: String? = null
    private var completion: ((TonePreviewResult) -> Unit)? = null
    private var requestGeneration = 0L

    fun nextRequest(): Long = ++requestGeneration
    fun isCurrentRequest(request: Long): Boolean = request == requestGeneration

    fun begin(ownerId: String, onFinished: (TonePreviewResult) -> Unit) {
        check(owner == null)
        owner = ownerId
        completion = onFinished
    }

    fun rejectWhileAlarmActive(alarmActive: Boolean, onFinished: (TonePreviewResult) -> Unit): Boolean {
        if (!alarmActive) return false
        onFinished(TonePreviewResult.BLOCKED_BY_ALARM)
        return true
    }

    fun owns(ownerId: String): Boolean = owner == ownerId

    fun finish(result: TonePreviewResult) {
        val callback = completion
        owner = null
        completion = null
        callback?.invoke(result)
    }
}
