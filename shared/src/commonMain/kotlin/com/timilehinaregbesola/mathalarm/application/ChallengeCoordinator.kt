package com.timilehinaregbesola.mathalarm.application

import co.touchlab.kermit.Logger
import com.timilehinaregbesola.mathalarm.analytics.*
import com.timilehinaregbesola.mathalarm.domain.model.Alarm
import com.timilehinaregbesola.mathalarm.domain.model.mathChallenge
import com.timilehinaregbesola.mathalarm.framework.Usecases
import com.timilehinaregbesola.mathalarm.framework.consumeDueOccurrence
import com.timilehinaregbesola.mathalarm.interactors.AudioPlayer
import com.timilehinaregbesola.mathalarm.platform.getDefaultAlarmTone
import com.timilehinaregbesola.mathalarm.platform.shouldStartMathScreenAlarmAudio
import com.timilehinaregbesola.mathalarm.platform.stopAlarmTonePreview
import com.timilehinaregbesola.mathalarm.platform.stopPlatformAlarmAudio
import com.timilehinaregbesola.mathalarm.presentation.alarmmath.*
import com.timilehinaregbesola.mathalarm.utils.AlarmErrorMessage
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlin.time.Clock

/** Application-owned real sessions. Observer removal never stops audio, progress or commands. */
internal class ChallengeCoordinator(
    private val usecases: Usecases,
    private val audio: AudioPlayer,
    private val logger: Logger,
    private val store: ChallengeProgressStore,
    private val analytics: AnalyticsTracker,
    private val defaultTone: () -> String = { "" },
    private val previewVibrationFactory: () -> com.timilehinaregbesola.mathalarm.platform.PreviewVibration = { com.timilehinaregbesola.mathalarm.platform.NoPreviewVibration },
) {
    private val initialization = Mutex()
    private var previewSession: ChallengeSession? = null
    private var realSession: ChallengeSession? = null
    private var previewVibration: com.timilehinaregbesola.mathalarm.platform.PreviewVibration? = null
    private var audioOwner: ChallengeSession? = null
    private var nextResultId = 0L
    private val retained = mutableMapOf<String, ChallengeSession>()

    init {
        usecases.applicationScope.launch {
            usecases.getSavedAlarms().collect { alarms ->
                val session = realSession ?: return@collect
                val state = session.state.value
                if (state.finishing || state.readiness != ChallengeReadiness.READY) return@collect
                val current = alarms.firstOrNull { it.alarmId == state.alarm?.alarmId }
                if (current == null || (current.scheduleError == null && (!current.isOn || current.activeAt != state.alarm?.activeAt))) {
                    session.fail(AlarmErrorMessage.STALE_OCCURRENCE)
                    releaseAudio(session)
                    state.occurrenceId?.let { retained.remove(it) }
                    realSession = null
                }
            }
        }
    }

    /** Awaiting may be cancelled; initialization itself continues in the application scope. */
    suspend fun open(alarm: Alarm, preview: Boolean): ChallengeSession = if (preview) {
        // Preview is screen work. Cancellation while waiting must never start orphan audio.
        coroutineScope { openOwned(alarm, true) }
    } else usecases.applicationScope.async { openOwned(alarm, false) }.await()

    private suspend fun openOwned(alarm: Alarm, preview: Boolean): ChallengeSession =
        initialization.withLock {
            val session = ChallengeSession(preview) { ++nextResultId }
            session.update { it.copy(readiness = ChallengeReadiness.INITIALIZING) }
            try {
                val authoritative = if (preview) alarm else usecases.command {
                    val current = findAlarm(alarm.alarmId)
                        ?: return@command null
                    if (!current.isOn) return@command null
                    val active = realSession?.state?.value
                    if (active?.readiness == ChallengeReadiness.READY) {
                        if (active.alarm?.alarmId == current.alarmId &&
                            (alarm.activeAt == null || alarm.activeAt == active.alarm.activeAt)) return@command current
                        return@command null // Later handoff remains in the native ordered queue.
                    }
                    // An unresolved occurrence takes priority over later deliveries of the same alarm.
                    if (current.activeAt == null && alarm.activeAt != null) {
                        val expected = alarm.activeAt
                        if (expected !in current.pendingTimes && expected != current.snoozedUntil) return@command null
                        showAlarm(current.alarmId, expected, snoozed = expected == current.snoozedUntil)
                    }
                    val consumed = if (current.activeAt != null || alarm.activeAt != null) findAlarm(current.alarmId) else consumeDueOccurrence(current.alarmId)
                    if (consumed?.activeAt == null && !current.scheduleInitialized) showAlarm(current.alarmId)
                    findAlarm(current.alarmId)?.takeIf { saved ->
                        saved.activeAt != null && (alarm.activeAt == null || alarm.activeAt == saved.activeAt)
                    }
                }
                if (authoritative == null) { session.fail(AlarmErrorMessage.INITIALIZATION); return@withLock session }
                val key = if (preview) null else "${authoritative.alarmId}:${authoritative.activeAt}"
                key?.let { retained[it] }?.takeIf { it.state.value.readiness == ChallengeReadiness.READY }?.let {
                    realSession = it
                    return@withLock it
                }
                val restored = if (preview) null else store.load(authoritative.alarmId, authoritative.activeAt!!)
                val problems = restored?.problems ?: generateChallengeProblems(authoritative.mathChallenge)
                session.update { it.copy(
                    alarm = authoritative, occurrenceId = key, problems = problems,
                    questionIndex = restored?.questionIndex ?: 0,
                    startedAt = restored?.startedAt?.takeIf { time -> time > 0 } ?: Clock.System.now().toEpochMilliseconds(),
                    incorrectAnswers = restored?.incorrectAnswers ?: 0,
                ) }
                persist(session) // Durable unresolved record precedes readiness and native acknowledgement.
                session.update { it.copy(readiness = ChallengeReadiness.READY) }
                if (!preview) { realSession = session; retained[key!!] = session } else previewSession = session
                if (preview) analytics.trackSafely(AnalyticsEvents.alarmPreviewStarted)
                if (restored == null) analytics.trackSafely(AnalyticsEvents.challengeStarted(preview, authoritative))
                startAudio(session)
                AlarmApplicationStatus.state.value.failures.firstOrNull { it.alarmId == authoritative.alarmId }?.let {
                    session.result(ChallengeOutcome.Failure(it.error))
                }
                session
            } catch (error: CancellationException) { throw error }
            catch (error: Exception) {
                logger.e(error) { "Challenge initialization failed" }
                session.fail(AlarmErrorMessage.INITIALIZATION)
                session // No successful key is cached; retry repeats the authoritative load.
            }
        }

    fun answer(session: ChallengeSession, value: String) = session.update { it.copy(answerText = value) }
    fun submit(session: ChallengeSession, problem: MathProblem) {
        val state = session.state.value
        if (state.readiness != ChallengeReadiness.READY || state.finishing || problem != state.currentProblem) return
        if (state.answerText.isBlank() || state.answerText.trim().toIntOrNull() != problem.answer) {
            session.update { it.copy(incorrectAnswers = it.incorrectAnswers + if (it.answerText.isNotBlank()) 1 else 0) }
            if (!persistProgressChange(session, state)) return
            session.result(ChallengeOutcome.Failure(AlarmErrorMessage.INCORRECT_ANSWER))
            return
        }
        session.update { it.copy(answerText = "") }
        if (state.questionIndex < state.problems.lastIndex) {
            session.update { it.copy(questionIndex = it.questionIndex + 1) }
            persistProgressChange(session, state)
        } else finish(session, snooze = false)
    }

    private fun persistProgressChange(session: ChallengeSession, previous: ChallengeState): Boolean = try {
        persist(session)
        true
    } catch (error: Exception) {
        logger.e(error) { "Challenge progress update failed" }
        session.update { previous }
        session.result(ChallengeOutcome.Failure(AlarmErrorMessage.UPDATE))
        false
    }

    fun finish(session: ChallengeSession, snooze: Boolean) {
        val state = session.state.value
        if (state.readiness != ChallengeReadiness.READY || state.finishing || state.alarm == null) return
        if (snooze && !state.alarm.canSnooze) return
        session.update { it.copy(finishing = true) }
        usecases.launchCommand {
            try {
                val accepted = state.preview || command {
                    val activeAt = state.alarm.activeAt ?: return@command false
                    if (snooze) snoozeAlarm(state.alarm.alarmId, expectedActiveAt = activeAt)
                    else completeAlarm(state.alarm.alarmId, expectedActiveAt = activeAt)
                }
                if (!accepted) {
                    session.result(ChallengeOutcome.Failure(if (snooze) AlarmErrorMessage.SNOOZE else AlarmErrorMessage.DISMISS))
                    return@launchCommand
                }
                if (!state.preview) runCatching { store.clear(state.alarm.alarmId, state.alarm.activeAt!!) }
                    .onFailure { logger.e(it) { "Progress cleanup failed after accepted resolution" } }
                if (!snooze) analytics.trackSafely(AnalyticsEvents.challengeCompleted(state.preview,
                    (Clock.System.now().toEpochMilliseconds() - state.startedAt).coerceAtLeast(0) / 1000, state.incorrectAnswers))
                if (AlarmApplicationStatus.state.value.failures.any { it.alarmId == state.alarm.alarmId }) {
                    runCatching { AlarmApplicationStatus.clearRecoveryFailure(state.alarm.alarmId) }
                        .onFailure { logger.e(it) { "Accepted resolution status cleanup failed" } }
                }
                session.update { it.copy(readiness = ChallengeReadiness.RESOLVED) }
                releaseAudio(session)
                if (realSession === session) realSession = null
                state.occurrenceId?.let { retained.remove(it) }
                if (!state.preview) previewSession?.takeIf { it.state.value.readiness == ChallengeReadiness.READY }?.let(::startAudio)
                session.result(if (snooze) ChallengeOutcome.Snoozed else ChallengeOutcome.Completed)
            } catch (error: CancellationException) { throw error }
            catch (error: Exception) {
                logger.e(error) { "Challenge resolution failed" }
                session.result(ChallengeOutcome.Failure(if (snooze) AlarmErrorMessage.SNOOZE else AlarmErrorMessage.DISMISS))
            } finally { session.update { it.copy(finishing = false) } }
        }
    }

    fun closePreview(session: ChallengeSession) {
        if (!session.state.value.preview) return
        releaseAudio(session)
        if (previewSession === session) previewSession = null
    }

    private fun persist(session: ChallengeSession) {
        val state = session.state.value
        if (state.preview) return
        val alarm = state.alarm ?: return
        store.save(alarm.alarmId, ChallengeProgressStore.Progress(alarm.activeAt!!, state.problems,
            state.questionIndex, state.startedAt, state.incorrectAnswers))
    }

    private fun startAudio(session: ChallengeSession) {
        val state = session.state.value
        if (state.preview && realSession != null) return
        if (!state.preview) audioOwner?.takeIf { it.state.value.preview }?.let(::releaseAudio)
        if (!shouldStartMathScreenAlarmAudio(state.preview)) return // Android real audio remains service-owned.
        val alarm = state.alarm ?: return
        // Own partially initialized resources too, so a tone/setup failure can clean them.
        audioOwner = session
        try {
            stopAlarmTonePreview()
            previewVibration?.stop(); previewVibration = null
            if (state.preview && alarm.vibrate && !com.timilehinaregbesola.mathalarm.platform.isIosPlatform()) {
                previewVibration = previewVibrationFactory().also { it.start() }
            }
            audio.stop(); audio.init(); audio.reset()
            audio.setDataSourceFromString(alarm.alarmTone.ifEmpty(defaultTone))
            audio.setVibrate(alarm.vibrate)
            audio.startAlarmAudio()
        } catch (error: Exception) {
            logger.e(error) { "Challenge audio failed" }
            releaseAudio(session)
            session.result(ChallengeOutcome.Failure(AlarmErrorMessage.TONE))
        }
    }

    private fun releaseAudio(session: ChallengeSession) {
        if (audioOwner !== session) return
        runCatching { previewVibration?.stop() }.onFailure { logger.e(it) { "Preview vibration cleanup failed" } }
        previewVibration = null
        try { audio.stop() }
        catch (error: Exception) { logger.e(error) { "Audio cleanup failed after accepted resolution" } }
        try { if (!session.state.value.preview) stopPlatformAlarmAudio() }
        catch (error: Exception) { logger.e(error) { "Platform audio cleanup failed" } }
        finally { audioOwner = null }
    }
}

internal class ChallengeSession(preview: Boolean, private val nextResultId: () -> Long) {
    private val mutable = MutableStateFlow(ChallengeState(preview = preview))
    val state = mutable.asStateFlow()
    fun update(block: (ChallengeState) -> ChallengeState) { mutable.value = block(mutable.value) }
    fun result(outcome: ChallengeOutcome) = update { it.copy(results = it.results + ChallengeResult(nextResultId(), outcome)) }
    fun acknowledge(id: Long) = update { it.copy(results = it.results.filterNot { result -> result.id == id }) }
    fun fail(error: AlarmErrorMessage) {
        update { it.copy(readiness = ChallengeReadiness.ERROR, error = error) }
        result(ChallengeOutcome.Failure(error))
    }
}
