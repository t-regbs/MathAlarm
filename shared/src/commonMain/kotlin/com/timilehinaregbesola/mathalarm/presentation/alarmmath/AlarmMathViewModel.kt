package com.timilehinaregbesola.mathalarm.presentation.alarmmath

import co.touchlab.kermit.Logger
import com.timilehinaregbesola.mathalarm.analytics.AnalyticsTracker
import com.timilehinaregbesola.mathalarm.analytics.NoopAnalyticsTracker
import com.timilehinaregbesola.mathalarm.application.ChallengeCoordinator
import com.timilehinaregbesola.mathalarm.application.ChallengeSession
import com.timilehinaregbesola.mathalarm.domain.model.Alarm
import com.timilehinaregbesola.mathalarm.framework.Usecases
import com.timilehinaregbesola.mathalarm.interactors.AudioPlayer
import com.timilehinaregbesola.mathalarm.presentation.SharedFeatureViewModel
import com.rickclephas.kmp.observableviewmodel.MutableStateFlow
import com.rickclephas.kmp.observableviewmodel.coroutineScope
import com.rickclephas.kmp.nativecoroutines.NativeCoroutines
import com.rickclephas.kmp.nativecoroutines.NativeCoroutinesState
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.asStateFlow

/** Observation adapter over the application session; no durable work belongs to its scope. */
class AlarmMathViewModel internal constructor(
    usecases: Usecases,
    audioPlayer: AudioPlayer,
    logger: Logger,
    progressStore: ChallengeProgressStore,
    analytics: AnalyticsTracker = NoopAnalyticsTracker,
    private val coordinator: ChallengeCoordinator = ChallengeCoordinator(usecases, audioPlayer, logger, progressStore, analytics),
) : SharedFeatureViewModel() {
    private val mutable = MutableStateFlow(viewModelScope, ChallengeState())
    @NativeCoroutinesState
    val state: kotlinx.coroutines.flow.StateFlow<ChallengeState> = mutable.asStateFlow()
    private var session: ChallengeSession? = null
    private var observation: Job? = null
    val currentAlarm get() = state.value.alarm
    val questionCount get() = state.value.questionCount.coerceAtLeast(1)
    val currentProblem get() = state.value.currentProblem

    /** Native callers supply identity only; all saved configuration is loaded below the UI. */
    @NativeCoroutines
    suspend fun initializeOccurrence(alarmId: Long, activeAt: Long?): Boolean =
        initializeChallenge(Alarm(alarmId = alarmId, activeAt = activeAt), preview = false)

    @NativeCoroutines
    suspend fun initializeChallenge(alarm: Alarm, preview: Boolean): Boolean {
        if (isClosed) return false
        val existing = session?.state?.value
        if (existing?.readiness == ChallengeReadiness.READY && existing.preview == preview &&
            existing.alarm?.alarmId == alarm.alarmId && (alarm.activeAt == null || existing.alarm.activeAt == alarm.activeAt)) return true
        mutable.value = ChallengeState(readiness = ChallengeReadiness.INITIALIZING, preview = preview)
        val opened = coordinator.open(alarm, preview)
        if (isClosed) { if (preview) coordinator.closePreview(opened); return false }
        observation?.cancel()
        session = opened
        mutable.value = opened.state.value
        observation = viewModelScope.coroutineScope.launch { opened.state.collect { mutable.value = it } }
        return opened.state.value.readiness == ChallengeReadiness.READY
    }

    fun onEvent(event: MathScreenEvent) {
        if (isClosed) return
        val current = session
        when (event) {
            MathScreenEvent.OnClearClick -> if (current != null) coordinator.answer(current, "") else mutable.value = mutable.value.copy(answerText = "")
            is MathScreenEvent.EnteredAnswer -> if (current != null) coordinator.answer(current, event.value) else mutable.value = mutable.value.copy(answerText = event.value)
            is MathScreenEvent.OnEnterClick -> current?.let { coordinator.submit(it, event.problem) }
            is MathScreenEvent.OnSnoozeClick -> current?.takeIf { it.state.value.preview == event.preview && it.state.value.alarm?.alarmId == event.alarm }?.let { coordinator.finish(it, true) }
            is MathScreenEvent.OnToneError -> current?.result(ChallengeOutcome.Failure(com.timilehinaregbesola.mathalarm.utils.AlarmErrorMessage.TONE))
        }
        current?.let { mutable.value = it.state.value }
    }

    fun acknowledgeResult(id: Long) { session?.let { it.acknowledge(id); mutable.value = it.state.value } }
    fun stopPreview() { session?.let { coordinator.closePreview(it) } }
    fun completeAlarm(alarm: Alarm, preview: Boolean = false) {
        if (isClosed) return
        session?.takeIf { it.state.value.alarm?.alarmId == alarm.alarmId && it.state.value.preview == preview }?.let { coordinator.finish(it, false) }
    }

    override fun onCleared() {
        stopPreview()
        super.onCleared()
    }
}
