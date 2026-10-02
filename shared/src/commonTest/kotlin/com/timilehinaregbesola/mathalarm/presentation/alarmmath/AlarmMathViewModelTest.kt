package com.timilehinaregbesola.mathalarm.presentation.alarmmath

import app.cash.turbine.test
import co.touchlab.kermit.Logger
import com.timilehinaregbesola.mathalarm.data.AlarmRepository
import com.timilehinaregbesola.mathalarm.domain.model.Alarm
import com.timilehinaregbesola.mathalarm.analytics.AnalyticsEvent
import com.timilehinaregbesola.mathalarm.analytics.AnalyticsTracker
import com.timilehinaregbesola.mathalarm.fake.AlarmInteractorFake
import com.timilehinaregbesola.mathalarm.fake.AlarmRepositoryFake
import com.timilehinaregbesola.mathalarm.fake.AlarmTimeCalculatorFake
import com.timilehinaregbesola.mathalarm.fake.AudioPlayerFake
import com.timilehinaregbesola.mathalarm.fake.DateTimeProviderFake
import com.timilehinaregbesola.mathalarm.fake.NotificationInteractorFake
import com.timilehinaregbesola.mathalarm.framework.Usecases
import com.timilehinaregbesola.mathalarm.framework.snoozeFromNotification
import kotlin.test.assertFailsWith
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toInstant
import com.timilehinaregbesola.mathalarm.interactors.AlarmInteractor
import com.timilehinaregbesola.mathalarm.usecases.AddAlarm
import com.timilehinaregbesola.mathalarm.usecases.CancelAlarm
import com.timilehinaregbesola.mathalarm.usecases.ClearAlarms
import com.timilehinaregbesola.mathalarm.usecases.CompleteAlarm
import com.timilehinaregbesola.mathalarm.usecases.DeleteAlarm
import com.timilehinaregbesola.mathalarm.usecases.FindAlarm
import com.timilehinaregbesola.mathalarm.usecases.GetSavedAlarms
import com.timilehinaregbesola.mathalarm.usecases.RescheduleFutureAlarms
import com.timilehinaregbesola.mathalarm.usecases.ScheduleAlarm
import com.timilehinaregbesola.mathalarm.usecases.ScheduleNextAlarm
import com.timilehinaregbesola.mathalarm.usecases.ShowAlarm
import com.timilehinaregbesola.mathalarm.usecases.SkipNextAlarm
import com.timilehinaregbesola.mathalarm.usecases.SnoozeAlarm
import com.timilehinaregbesola.mathalarm.usecases.UpdateAlarm
import com.timilehinaregbesola.mathalarm.utils.AlarmErrorMessage
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlinx.coroutines.launch
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.setMain

@OptIn(ExperimentalCoroutinesApi::class)
class AlarmMathViewModelTest {

    private lateinit var progressStore: ChallengeProgressStore
    private lateinit var progressSettings: com.russhwolf.settings.MapSettings
    private lateinit var viewModel: AlarmMathViewModel
    private lateinit var audioPlayer: AudioPlayerFake
    private lateinit var dataSource: AlarmRepositoryFake
    private lateinit var repository: AlarmRepository
    private lateinit var alarmInteractor: AlarmInteractorFake
    private lateinit var notificationInteractor: NotificationInteractorFake
    private lateinit var dateTimeProvider: DateTimeProviderFake
    private lateinit var usecases: Usecases
    private val analyticsEvents = mutableListOf<AnalyticsEvent>()
    private val analytics = AnalyticsTracker { analyticsEvents.add(it) }
    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        
        progressSettings = com.russhwolf.settings.MapSettings()
        progressStore = ChallengeProgressStore(progressSettings)
        audioPlayer = AudioPlayerFake()
        dataSource = AlarmRepositoryFake()
        repository = AlarmRepository(dataSource)
        alarmInteractor = AlarmInteractorFake()
        notificationInteractor = NotificationInteractorFake()
        dateTimeProvider = DateTimeProviderFake()
        
        val alarmTimeCalculator = AlarmTimeCalculatorFake()
        val scheduleNextAlarm = ScheduleNextAlarm(alarmInteractor, alarmTimeCalculator)
        val rescheduleFutureAlarms = RescheduleFutureAlarms(repository, alarmInteractor, alarmTimeCalculator)
        
        usecases = Usecases(
            addAlarm = AddAlarm(repository),
            findAlarm = FindAlarm(repository),
            deleteAlarm = DeleteAlarm(repository, alarmInteractor, notificationInteractor),
            getSavedAlarms = GetSavedAlarms(repository),
            scheduleAlarm = ScheduleAlarm(repository, alarmInteractor, alarmTimeCalculator),
            showAlarm = ShowAlarm(repository, notificationInteractor, scheduleNextAlarm),
            completeAlarm = CompleteAlarm(repository, alarmInteractor, notificationInteractor, dateTimeProvider),
            updateAlarm = UpdateAlarm(repository, alarmInteractor),
            cancelAlarm = CancelAlarm(alarmInteractor),
            clearAlarms = ClearAlarms(repository, DeleteAlarm(repository, alarmInteractor, notificationInteractor)),
            scheduleNextAlarm = scheduleNextAlarm,
            rescheduleFutureAlarms = rescheduleFutureAlarms,
            snoozeAlarm = SnoozeAlarm(dateTimeProvider, notificationInteractor, alarmInteractor, repository),
            skipNextAlarm = SkipNextAlarm(repository, alarmTimeCalculator, rescheduleFutureAlarms),
        )
        
        viewModel = AlarmMathViewModel(
            usecases = usecases,
            audioPlayer = audioPlayer,
            logger = Logger.withTag("AlarmMathViewModelTest"),
            progressStore = progressStore,
            analytics = analytics,
        )
    }

    @Test
    fun `challenge advances only on correct answers and keeps audio playing until final completion`() = runTest {
        val alarm = Alarm(alarmId = 90, difficulty = 3, questionCount = 3, challengeOperations = "+")
        viewModel.initializeChallenge(alarm, preview = true)
        audioPlayer.startAlarmAudio()
        viewModel.resultEvents().test {
            val first = viewModel.currentProblem!!
            viewModel.onEvent(MathScreenEvent.EnteredAnswer("-1"))
            viewModel.onEvent(MathScreenEvent.OnEnterClick(first))
            awaitItem() shouldBe ChallengeOutcome.Failure(AlarmErrorMessage.INCORRECT_ANSWER)
            viewModel.state.value.questionIndex shouldBe 0
            repeat(3) { index ->
                val problem = viewModel.currentProblem!!
                viewModel.onEvent(MathScreenEvent.EnteredAnswer(problem.answer.toString()))
                viewModel.onEvent(MathScreenEvent.OnEnterClick(problem))
                if (index < 2) {
                    expectNoEvents()
                    viewModel.state.value.questionIndex shouldBe index + 1
                    audioPlayer.isPlaying shouldBe true
                    // Rotation/recomposition must not restart an in-progress challenge.
                    viewModel.initializeChallenge(alarm, preview = true)
                    viewModel.state.value.questionIndex shouldBe index + 1
                } else {
                    awaitItem() shouldBe ChallengeOutcome.Completed
                }
            }
        }
    }

    @Test
    fun `scheduled alarms recover saved challenge while test alarm uses draft`() = runTest {
        val saved = Alarm(alarmId = 91, isOn = true, isSaved = true, activeAt = 1000, difficulty = 3, questionCount = 7, challengeOperations = "÷", factorRange = 2)
        usecases.addAlarm(saved)
        viewModel.initializeChallenge(Alarm(alarmId = 91), preview = false)
        viewModel.questionCount shouldBe 7
        viewModel.currentProblem!!.operator shouldBe MathProblemOperator.Divide
        viewModel.initializeChallenge(saved.copy(questionCount = 2, challengeOperations = "+"), preview = true)
        viewModel.questionCount shouldBe 2
        viewModel.currentProblem!!.operator shouldBe MathProblemOperator.Add
    }

    @Test
    fun `scheduled alarm uses saved mix and preview uses its own mix`() = runTest {
        usecases.addAlarm(Alarm(alarmId = 92, isOn = true, isSaved = true, activeAt = 1000, difficultyMix = "00112", questionCount = 5))
        viewModel.initializeChallenge(Alarm(alarmId = 92), preview = false)
        viewModel.questionCount shouldBe 5
        viewModel.initializeChallenge(Alarm(alarmId = 92, isOn = true, isSaved = true, activeAt = 1000, difficultyMix = "12", questionCount = 2), preview = true)
        viewModel.questionCount shouldBe 2
    }

    private fun recreatedViewModel() = AlarmMathViewModel(
        usecases, AudioPlayerFake(), Logger.withTag("restored"), ChallengeProgressStore(progressSettings), analytics
    )

    @Test
    fun `challenge analytics survives recreation without a second start`() = runTest {
        val alarm = Alarm(alarmId = 900, activeAt = 1000, isSaved = true, isOn = true, questionCount = 1)
        usecases.addAlarm(alarm)
        viewModel.initializeChallenge(alarm, preview = false)
        val problem = viewModel.currentProblem!!
        viewModel.onEvent(MathScreenEvent.EnteredAnswer("wrong"))
        viewModel.onEvent(MathScreenEvent.OnEnterClick(problem))
        advanceUntilIdle()

        val restored = recreatedViewModel()
        restored.initializeChallenge(alarm, preview = false)
        restored.completeAlarm(alarm)
        advanceUntilIdle()

        analyticsEvents.map { it.name } shouldBe listOf("challenge_started", "challenge_completed")
        analyticsEvents.last().counts["incorrect_answers"] shouldBe 1L
    }

    @Test
    fun `progress and exact questions survive a new view model and store`() = runTest {
        val alarm = Alarm(alarmId = 501, activeAt = 1000, isSaved = true, isOn = true, questionCount = 10)
        usecases.addAlarm(alarm)
        viewModel.initializeChallenge(alarm, preview = false)
        repeat(9) {
            val problem = viewModel.currentProblem!!
            viewModel.onEvent(MathScreenEvent.EnteredAnswer(problem.answer.toString()))
            viewModel.onEvent(MathScreenEvent.OnEnterClick(problem))
        }
        val lastProblem = viewModel.currentProblem
        val restored = recreatedViewModel()
        restored.initializeChallenge(alarm, preview = false)
        restored.state.value.questionIndex shouldBe 9
        restored.questionCount shouldBe 10
        restored.currentProblem shouldBe lastProblem
    }

    @Test
    fun `new occurrences reset progress and previews do not overwrite it`() = runTest {
        val alarm = Alarm(alarmId = 502, isOn = true, isSaved = true, activeAt = 1000, questionCount = 3)
        usecases.addAlarm(alarm)
        viewModel.initializeChallenge(alarm, preview = false)
        val problem = viewModel.currentProblem!!
        viewModel.onEvent(MathScreenEvent.EnteredAnswer(problem.answer.toString()))
        viewModel.onEvent(MathScreenEvent.OnEnterClick(problem))
        val preview = recreatedViewModel()
        preview.initializeChallenge(alarm, preview = true)
        preview.state.value.questionIndex shouldBe 0
        preview.completeAlarm(alarm, preview = true)
        advanceUntilIdle()
        progressStore.load(502, 1000)!!.questionIndex shouldBe 1
        usecases.addAlarm(alarm.copy(activeAt = 2000))
        val next = recreatedViewModel()
        next.initializeChallenge(alarm.copy(activeAt = 2000), preview = false) shouldBe true
        next.state.value.questionIndex shouldBe 0
        progressStore.load(502, 2000)!!.questionIndex shouldBe 0
    }

    @Test
    fun `successful completion and snooze clear persisted progress`() = runTest {
        for (snooze in listOf(false, true)) {
            val alarm = Alarm(alarmId = 503, isOn = true, isSaved = true, activeAt = 1000, questionCount = 3)
            usecases.addAlarm(alarm)
            val vm = recreatedViewModel()
            vm.initializeChallenge(alarm, preview = false) shouldBe true
            if (snooze) vm.onEvent(MathScreenEvent.OnSnoozeClick(alarm.alarmId))
            else vm.completeAlarm(alarm)
            advanceUntilIdle()
            progressStore.load(503, 1000) shouldBe null
        }
    }

    @Test
    fun `enter before initialization and stale questions cannot complete a challenge`() = runTest {
        viewModel.resultEvents().test {
            viewModel.onEvent(MathScreenEvent.EnteredAnswer("0"))
            viewModel.onEvent(MathScreenEvent.OnEnterClick(MathProblem()))
            advanceUntilIdle()
            expectNoEvents()
            viewModel.initializeChallenge(Alarm(questionCount = 2), preview = true)
            val first = viewModel.currentProblem!!
            viewModel.onEvent(MathScreenEvent.EnteredAnswer(first.answer.toString()))
            viewModel.onEvent(MathScreenEvent.OnEnterClick(first))
            viewModel.onEvent(MathScreenEvent.EnteredAnswer(viewModel.currentProblem!!.answer.toString()))
            viewModel.onEvent(MathScreenEvent.OnEnterClick(first))
            advanceUntilIdle()
            viewModel.state.value.questionIndex shouldBe 1
            expectNoEvents()
        }
    }

    @Test
    fun `failed initialization never caches readiness and retries authoritative loading`() = runTest {
        var failures = 2
        val source = object : com.timilehinaregbesola.mathalarm.data.AlarmDataSource by dataSource {
            override suspend fun findAlarm(id: Long): Alarm? {
                if (failures-- > 0) error("storage temporarily unavailable")
                return dataSource.findAlarm(id)
            }
        }
        val alarm = Alarm(alarmId = 991, isOn = true, isSaved = true, activeAt = 1000)
        usecases.addAlarm(alarm)
        val commands = usecases.copy(findAlarm = FindAlarm(AlarmRepository(source)))
        val vm = AlarmMathViewModel(commands, audioPlayer, Logger.withTag("retry"), progressStore)
        vm.initializeOccurrence(alarm.alarmId, alarm.activeAt) shouldBe false
        vm.state.value.readiness shouldBe ChallengeReadiness.ERROR
        vm.initializeOccurrence(alarm.alarmId, alarm.activeAt) shouldBe false
        progressStore.load(991, 1000) shouldBe null
        vm.initializeOccurrence(alarm.alarmId, alarm.activeAt) shouldBe true
        vm.state.value.readiness shouldBe ChallengeReadiness.READY
        progressStore.load(991, 1000)!!.problems shouldBe vm.state.value.problems
    }

    @Test
    fun `correct final answer resolves without a UI observer and survives owner removal`() = runTest {
        val alarm = Alarm(alarmId = 992, isOn = true, isSaved = true, activeAt = 1000)
        usecases.addAlarm(alarm)
        viewModel.initializeChallenge(alarm, false) shouldBe true
        val problem = viewModel.currentProblem!!
        viewModel.onEvent(MathScreenEvent.EnteredAnswer(problem.answer.toString()))
        viewModel.onEvent(MathScreenEvent.OnEnterClick(problem))
        viewModel.close()
        advanceUntilIdle()
        val persisted = usecases.findAlarm(992)!!
        persisted.activeAt shouldBe null
        persisted.isOn shouldBe false
        alarmInteractor.getAlarmTimeMillis(992) shouldBe null
        progressStore.load(992, 1000) shouldBe null
    }

    @Test
    fun `removing unresolved real observer keeps occurrence and exact progress`() = runTest {
        val alarm = Alarm(alarmId = 993, isOn = true, isSaved = true, activeAt = 1000, questionCount = 2)
        usecases.addAlarm(alarm)
        viewModel.initializeChallenge(alarm, false)
        val before = progressStore.load(993, 1000)!!
        viewModel.close()
        advanceUntilIdle()
        usecases.findAlarm(993)!!.activeAt shouldBe 1000L
        progressStore.load(993, 1000) shouldBe before
        val restored = recreatedViewModel()
        restored.initializeChallenge(alarm, false) shouldBe true
        restored.state.value.problems shouldBe before.problems
    }

    @Test
    fun `two retained observers share a real occurrence session and later delivery waits`() = runTest {
        val first = Alarm(alarmId = 994, isOn = true, isSaved = true, activeAt = 1000, questionCount = 2)
        val later = Alarm(alarmId = 995, isOn = true, isSaved = true, activeAt = 2000)
        usecases.addAlarm(first); usecases.addAlarm(later)
        val coordinator = com.timilehinaregbesola.mathalarm.application.ChallengeCoordinator(usecases, audioPlayer,
            Logger.withTag("retained"), progressStore, analytics)
        fun owner() = AlarmMathViewModel(usecases, audioPlayer, Logger.withTag("owner"), progressStore, analytics, coordinator)
        val a = owner(); val b = owner(); val queued = owner()
        a.initializeChallenge(first, false) shouldBe true
        val problem = a.currentProblem!!
        a.onEvent(MathScreenEvent.EnteredAnswer(problem.answer.toString()))
        a.onEvent(MathScreenEvent.OnEnterClick(problem))
        b.initializeChallenge(first, false) shouldBe true
        b.state.value.questionIndex shouldBe 1
        b.state.value.problems shouldBe a.state.value.problems
        queued.initializeChallenge(later, false) shouldBe false
        progressStore.load(995, 2000) shouldBe null
        a.completeAlarm(first)
        advanceUntilIdle()
        queued.initializeChallenge(later, false) shouldBe true
        usecases.findAlarm(995)!!.activeAt shouldBe 2000L
    }

    @Test
    fun `explicit obsolete occurrence cannot initialize or consume a newer delivery`() = runTest {
        val alarm = Alarm(alarmId = 996, isOn = true, isSaved = true, activeAt = 2000,
            scheduleInitialized = true, pendingTimes = listOf(3000))
        usecases.addAlarm(alarm)
        viewModel.initializeChallenge(alarm.copy(activeAt = 1000), false) shouldBe false
        viewModel.initializeChallenge(alarm.copy(activeAt = 1000), false) shouldBe false
        usecases.findAlarm(996) shouldBe alarm
        progressStore.load(996, 2000) shouldBe null
    }

    @Test
    fun `cancelled preview initialization cannot start orphan playback`() = runTest {
        val job = launch { viewModel.initializeChallenge(Alarm(), true) }
        job.cancel()
        viewModel.close()
        advanceUntilIdle()
        audioPlayer.isPlaying shouldBe false
        viewModel.state.value.readiness shouldBe ChallengeReadiness.IDLE
    }

    @Test
    fun `accepted real initialization survives cancellation and restores without acknowledgement`() = runTest {
        val gate = kotlinx.coroutines.CompletableDeferred<Unit>()
        val blocker = launch { usecases.command { gate.await() } }
        runCurrent()
        val alarm = Alarm(alarmId = 997, isOn = true, isSaved = true,
            scheduleInitialized = true, pendingTimes = listOf(1000))
        usecases.addAlarm(alarm)
        val caller = launch { viewModel.initializeChallenge(alarm, false) }
        runCurrent()
        viewModel.close(); caller.cancel()
        gate.complete(Unit); blocker.join()
        advanceUntilIdle()
        val saved = usecases.findAlarm(997)!!
        saved.activeAt shouldBe 1000L
        val persisted = progressStore.load(997, 1000)!!
        val replacement = recreatedViewModel()
        replacement.initializeChallenge(saved, false) shouldBe true
        replacement.state.value.problems shouldBe persisted.problems
    }

    @Test
    fun `failed progress persistence never reports readiness and retries before acknowledgement`() = runTest {
        var reject = true
        val settings = object : com.russhwolf.settings.Settings by progressSettings {
            override fun putString(key: String, value: String) {
                if (reject) error("progress persistence unavailable")
                progressSettings.putString(key, value)
            }
        }
        val alarm = Alarm(alarmId = 998, isOn = true, isSaved = true, activeAt = 1000)
        usecases.addAlarm(alarm)
        val vm = AlarmMathViewModel(usecases, audioPlayer, Logger.withTag("progressRetry"), ChallengeProgressStore(settings))
        vm.initializeChallenge(alarm, false) shouldBe false
        vm.initializeChallenge(alarm, false) shouldBe false
        progressStore.load(998, 1000) shouldBe null
        reject = false
        vm.initializeChallenge(alarm, false) shouldBe true
        progressStore.load(998, 1000)!!.problems shouldBe vm.state.value.problems
    }

    @Test
    fun `failed progress update retains current problem and answer for retry`() = runTest {
        var reject = false
        val settings = object : com.russhwolf.settings.Settings by progressSettings {
            override fun putString(key: String, value: String) {
                if (reject) error("progress persistence unavailable")
                progressSettings.putString(key, value)
            }
        }
        val alarm = Alarm(alarmId = 1001, isOn = true, isSaved = true, activeAt = 1000, questionCount = 2)
        usecases.addAlarm(alarm)
        val vm = AlarmMathViewModel(usecases, audioPlayer, Logger.withTag("progressUpdate"), ChallengeProgressStore(settings))
        vm.initializeChallenge(alarm, false) shouldBe true
        val problem = vm.currentProblem!!
        vm.onEvent(MathScreenEvent.EnteredAnswer(problem.answer.toString()))
        reject = true
        vm.onEvent(MathScreenEvent.OnEnterClick(problem))
        vm.state.value.questionIndex shouldBe 0
        vm.state.value.answerText shouldBe problem.answer.toString()
        vm.state.value.results.last().outcome shouldBe ChallengeOutcome.Failure(AlarmErrorMessage.UPDATE)
        progressStore.load(1001, 1000)!!.questionIndex shouldBe 0
        reject = false
        vm.onEvent(MathScreenEvent.OnEnterClick(problem))
        vm.state.value.questionIndex shouldBe 1
        progressStore.load(1001, 1000)!!.questionIndex shouldBe 1
        usecases.findAlarm(1001)!!.activeAt shouldBe 1000L
    }

    @Test
    fun `failed preview audio setup cleans partial player and vibration resources`() = runTest {
        var vibrationStarts = 0
        var vibrationStops = 0
        val vibration = object : com.timilehinaregbesola.mathalarm.platform.PreviewVibration {
            override fun start() { vibrationStarts++ }
            override fun stop() { vibrationStops++ }
        }
        val audio = object : com.timilehinaregbesola.mathalarm.interactors.AudioPlayer by audioPlayer {
            override fun setDataSourceFromString(path: String) { error("tone unavailable") }
        }
        val coordinator = com.timilehinaregbesola.mathalarm.application.ChallengeCoordinator(usecases, audio,
            Logger.withTag("partialAudio"), progressStore, analytics, previewVibrationFactory = { vibration })
        val vm = AlarmMathViewModel(usecases, audio, Logger.withTag("previewFailure"), progressStore, analytics, coordinator)
        vm.initializeChallenge(Alarm(vibrate = true), true) shouldBe true
        vm.state.value.results.last().outcome shouldBe ChallengeOutcome.Failure(AlarmErrorMessage.TONE)
        audioPlayer.isStopped shouldBe true
        if (!com.timilehinaregbesola.mathalarm.platform.isIosPlatform()) vibrationStarts shouldBe 1
        vibrationStops shouldBe vibrationStarts
        vm.close()
        vibrationStops shouldBe vibrationStarts
        progressStore.load(0, 1000) shouldBe null
    }

    @Test
    fun `preview observer cleanup after real initialization cannot stop the real audio owner`() = runTest {
        var stops = 0
        val audio = object : com.timilehinaregbesola.mathalarm.interactors.AudioPlayer by audioPlayer {
            override fun stop() { stops++; audioPlayer.stop() }
        }
        val coordinator = com.timilehinaregbesola.mathalarm.application.ChallengeCoordinator(usecases, audio,
            Logger.withTag("audioOwner"), progressStore, analytics)
        val preview = AlarmMathViewModel(usecases, audio, Logger.withTag("preview"), progressStore, analytics, coordinator)
        val real = AlarmMathViewModel(usecases, audio, Logger.withTag("real"), progressStore, analytics, coordinator)
        preview.initializeChallenge(Alarm(), true) shouldBe true
        audioPlayer.isPlaying shouldBe true
        val alarm = Alarm(alarmId = 999, isOn = true, isSaved = true, activeAt = 1000)
        usecases.addAlarm(alarm)
        real.initializeChallenge(alarm, false) shouldBe true
        val beforeClose = stops
        preview.close()
        stops shouldBe beforeClose
        usecases.findAlarm(999)!!.activeAt shouldBe 1000L
        progressStore.load(999, 1000) shouldBe real.state.value.let {
            ChallengeProgressStore.Progress(1000, it.problems, it.questionIndex, it.startedAt, it.incorrectAnswers)
        }
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `failed snooze emits a localizable error and keeps the screen open`() = runTest {
        val backend = object : AlarmInteractor by alarmInteractor {
            override suspend fun scheduleSnooze(alarm: Alarm, timeInMillis: Long) {
                error("Internal OS scheduling details")
            }
        }
        val commands = usecases.copy(
            snoozeAlarm = SnoozeAlarm(dateTimeProvider, notificationInteractor, backend, repository)
        )
        viewModel = AlarmMathViewModel(commands, audioPlayer, Logger.withTag("ErrorTest"), progressStore)
        val alarm = Alarm(alarmId = 804, isOn = true, isSaved = true, snooze = 5, activeAt = 1000, questionCount = 3)
        commands.addAlarm(alarm)
        viewModel.initializeChallenge(alarm, preview = false)
        viewModel.resultEvents().test {
            viewModel.onEvent(MathScreenEvent.OnSnoozeClick(804))
            awaitItem() shouldBe ChallengeOutcome.Failure(AlarmErrorMessage.SNOOZE)
            advanceUntilIdle()
            expectNoEvents()
        }
        commands.findAlarm(804)!!.snoozedUntil shouldBe null
        progressStore.load(804, 1000)!!.questionIndex shouldBe 0
    }

    @Test
    fun `stale challenge cannot snooze a newer active occurrence`() = runTest {
        val first = Alarm(alarmId = 805, isOn = true, isSaved = true, activeAt = 1000)
        usecases.addAlarm(first)
        viewModel.initializeChallenge(first, preview = false)
        repository.updateAlarm(first.copy(activeAt = 2000))

        viewModel.resultEvents().test {
            viewModel.onEvent(MathScreenEvent.OnSnoozeClick(first.alarmId))
            awaitItem() shouldBe ChallengeOutcome.Failure(AlarmErrorMessage.SNOOZE)
            advanceUntilIdle()
            expectNoEvents()
        }

        val saved = usecases.findAlarm(first.alarmId)!!
        saved.activeAt shouldBe 2000L
        saved.snoozedUntil shouldBe null
        saved.snoozeCount shouldBe 0
    }

    @Test
    fun `stale challenge cannot complete after notification snooze`() = runTest {
        val alarm = Alarm(alarmId = 806, isOn = true, isSaved = true, activeAt = 1000)
        usecases.addAlarm(alarm)
        viewModel.initializeChallenge(alarm, preview = false)
        usecases.command { snoozeAlarm(alarm.alarmId, expectedActiveAt = 1000) } shouldBe true
        val snoozed = usecases.findAlarm(alarm.alarmId)!!

        viewModel.resultEvents().test {
            viewModel.completeAlarm(alarm)
            awaitItem() shouldBe ChallengeOutcome.Failure(AlarmErrorMessage.DISMISS)
            advanceUntilIdle()
            expectNoEvents()
        }

        usecases.findAlarm(alarm.alarmId) shouldBe snoozed
        alarmInteractor.getAlarmTimeMillis(alarm.alarmId) shouldBe snoozed.snoozedUntil
    }

    @Test
    fun `initial answer should be empty`() {
        viewModel.state.value.answerText shouldBe ""
    }

    @Test
    fun `onEvent with correct answer should emit CompleteAndClose event`() = runTest {
        viewModel.initializeChallenge(Alarm(), preview = true)
        val problem = viewModel.currentProblem!!
        
        viewModel.resultEvents().test {
            viewModel.onEvent(MathScreenEvent.EnteredAnswer(problem.answer.toString()))
            viewModel.onEvent(MathScreenEvent.OnEnterClick(problem))

            val lastEvent = awaitItem()
            lastEvent shouldBe ChallengeOutcome.Completed
            
            viewModel.state.value.answerText shouldBe ""
            
            // Audio is stopped only after the completion command succeeds.
        }
    }

    @Test
    fun `onEvent with incorrect answer should show error snackbar`() = runTest {
        viewModel.initializeChallenge(Alarm(), preview = true)
        val problem = viewModel.currentProblem!!
        
        viewModel.resultEvents().test {
            viewModel.onEvent(MathScreenEvent.EnteredAnswer("-1")) // Wrong answer
            viewModel.onEvent(MathScreenEvent.OnEnterClick(problem))
            
            val event = awaitItem()
            event shouldBe ChallengeOutcome.Failure(AlarmErrorMessage.INCORRECT_ANSWER)
        }
    }

    @Test
    fun `onEvent with blank answer should show error snackbar`() = runTest {
        viewModel.initializeChallenge(Alarm(), preview = true)
        val problem = viewModel.currentProblem!!
        
        viewModel.resultEvents().test {
            viewModel.onEvent(MathScreenEvent.EnteredAnswer(""))
            viewModel.onEvent(MathScreenEvent.OnEnterClick(problem))
            
            awaitItem() shouldBe ChallengeOutcome.Failure(AlarmErrorMessage.INCORRECT_ANSWER)
        }
    }

    @Test
    fun `onEvent OnClearClick should clear answer text`() {
        viewModel.onEvent(MathScreenEvent.EnteredAnswer("123"))
        
        viewModel.onEvent(MathScreenEvent.OnClearClick)
        
        viewModel.state.value.answerText shouldBe ""
    }

    @Test
    fun `onEvent EnteredAnswer should update answer text`() {
        viewModel.onEvent(MathScreenEvent.EnteredAnswer("42"))
        
        viewModel.state.value.answerText shouldBe "42"
    }

    @Test
    fun `onEvent OnSnoozeClick should snooze alarm and stop audio`() = runTest {
        val alarm = Alarm(alarmId = 123L, hour = 8, minute = 0, isSaved = true, isOn = true, activeAt = 1000)
        usecases.addAlarm(alarm)
        viewModel.initializeChallenge(alarm, preview = false)
        advanceUntilIdle()
        
        viewModel.resultEvents().test {
            viewModel.onEvent(MathScreenEvent.OnSnoozeClick(alarm.alarmId))
            
            advanceUntilIdle()
            
            audioPlayer.isPlaying shouldBe false
            
            val event = awaitItem()
            event shouldBe ChallengeOutcome.Snoozed
        }
    }

    @Test
    fun `onEvent OnToneError should emit a localizable error`() = runTest {
        viewModel.initializeChallenge(Alarm(), preview = true)
        viewModel.resultEvents().test {
            viewModel.onEvent(MathScreenEvent.OnToneError)
            
            val event = awaitItem()
            event shouldBe ChallengeOutcome.Failure(AlarmErrorMessage.TONE)
        }
    }

    @Test
    fun `preview initialization owns audio with its draft tone`() = runTest {
        val tone = "content://media/internal/audio/media/123"
        
        viewModel.initializeChallenge(Alarm(alarmTone = tone), preview = true)
        
        audioPlayer.isInitialized shouldBe true
        audioPlayer.isReset shouldBe true
        audioPlayer.dataSource shouldBe tone
        audioPlayer.isPlaying shouldBe true
    }

    @Test
    fun `completeAlarm should call completeAlarm use case`() = runTest {
        val alarm = Alarm(alarmId = 456, hour = 9, minute = 0, isOn = true, isSaved = true, activeAt = 1000)
        usecases.addAlarm(alarm)
        viewModel.initializeChallenge(alarm, preview = false)
        advanceUntilIdle()
        
        viewModel.completeAlarm(alarm)
        advanceUntilIdle()
        
        val completedAlarm = usecases.findAlarm(alarm.alarmId)
        completedAlarm shouldBe alarm.copy(isOn = false, activeAt = null)
    }

    @Test
    fun `answer with correct value after trimming whitespace should be accepted`() = runTest {
        viewModel.initializeChallenge(Alarm(), preview = true)
        val problem = viewModel.currentProblem!!
        
        viewModel.resultEvents().test {
            viewModel.onEvent(MathScreenEvent.EnteredAnswer("  ${problem.answer}  ")) // With whitespace
            viewModel.onEvent(MathScreenEvent.OnEnterClick(problem))
            
            val nextEvent = awaitItem()
            nextEvent shouldBe ChallengeOutcome.Completed
        }
    }

    @Test
    fun `multiple incorrect answers should show error each time`() = runTest {
        viewModel.initializeChallenge(Alarm(), preview = true)
        val problem = viewModel.currentProblem!!
        
        viewModel.resultEvents().test {
            viewModel.onEvent(MathScreenEvent.EnteredAnswer("-1"))
            viewModel.onEvent(MathScreenEvent.OnEnterClick(problem))
            awaitItem() shouldBe ChallengeOutcome.Failure(AlarmErrorMessage.INCORRECT_ANSWER)
            
            viewModel.onEvent(MathScreenEvent.EnteredAnswer("-2"))
            viewModel.onEvent(MathScreenEvent.OnEnterClick(problem))
            awaitItem() shouldBe ChallengeOutcome.Failure(AlarmErrorMessage.INCORRECT_ANSWER)
            
            viewModel.onEvent(MathScreenEvent.EnteredAnswer(problem.answer.toString()))
            viewModel.onEvent(MathScreenEvent.OnEnterClick(problem))
            awaitItem() shouldBe ChallengeOutcome.Completed
        }
    }
    @Test fun `preview completion never mutates the saved alarm`() = runTest {
        val alarm = Alarm(alarmId = 778, isOn = true, isSaved = true)
        usecases.addAlarm(alarm)
        viewModel.initializeChallenge(alarm, preview = true)
        viewModel.resultEvents().test {
            viewModel.completeAlarm(alarm, preview = true)
            awaitItem() shouldBe ChallengeOutcome.Completed
        }
        usecases.findAlarm(778)?.isOn shouldBe true
    }
    @Test fun `preview snooze never schedules a live occurrence`() = runTest {
        val alarm = Alarm(alarmId = 779, isOn = true, isSaved = true)
        usecases.addAlarm(alarm)
        viewModel.initializeChallenge(alarm, preview = true)
        viewModel.resultEvents().test {
            viewModel.onEvent(MathScreenEvent.OnSnoozeClick(779, preview = true))
            awaitItem() shouldBe ChallengeOutcome.Snoozed
        }
        usecases.findAlarm(779)?.snoozedUntil shouldBe null
    }


    @Test
    fun `opening a due system snooze consumes its delivery without resetting count`() = runTest {
        val alarm = Alarm(alarmId = 905, isOn = true, isSaved = true, scheduleInitialized = true,
            snoozedUntil = 1000, snoozeCount = 2)
        usecases.addAlarm(alarm)
        viewModel.initializeChallenge(alarm, preview = false)
        viewModel.currentAlarm!!.activeAt shouldBe 1000L
        viewModel.currentAlarm!!.snoozedUntil shouldBe null
        viewModel.currentAlarm!!.snoozeCount shouldBe 2
    }

    @Test
    fun `opening a later handoff retains the unresolved occurrence and allowance`() = runTest {
        val alarm = Alarm(alarmId = 906, isOn = true, isSaved = true, scheduleInitialized = true,
            activeAt = 1000, pendingTimes = listOf(2000), snoozeCount = 3)
        usecases.addAlarm(alarm)
        viewModel.initializeChallenge(alarm, preview = false)
        viewModel.currentAlarm!!.activeAt shouldBe 1000L
        viewModel.currentAlarm!!.snoozeCount shouldBe 3
        viewModel.currentAlarm!!.canSnooze shouldBe false
    }

    @Test
    fun `notification snooze consumes delivery and uses saved duration`() = runTest {
        val now = dateTimeProvider.getCurrentDateTime().toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds()
        usecases.addAlarm(Alarm(alarmId = 910, isOn = true, isSaved = true, scheduleInitialized = true,
            pendingTimes = listOf(now), snooze = 12, maxSnoozes = 2))
        usecases.snoozeFromNotification(910, now) shouldBe true
        val saved = usecases.findAlarm(910)!!
        saved.pendingTimes shouldBe emptyList()
        saved.activeAt shouldBe null
        saved.snoozedUntil shouldBe now + 12 * 60_000L
        saved.snoozeCount shouldBe 1
        notificationInteractor.isNotificationShown(910) shouldBe false
    }

    @Test
    fun `duplicate notification snooze succeeds without consuming another allowance`() = runTest {
        val now = dateTimeProvider.getCurrentDateTime().toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds()
        usecases.addAlarm(Alarm(alarmId = 911, isOn = true, activeAt = now, maxSnoozes = 1))
        usecases.snoozeFromNotification(911, now) shouldBe true
        val accepted = usecases.findAlarm(911)
        usecases.snoozeFromNotification(911, now) shouldBe true
        usecases.findAlarm(911) shouldBe accepted
    }

    @Test
    fun `notification snooze rejects exhausted or disabled policy and keeps delivery active`() = runTest {
        for (disabled in listOf(false, true)) {
            usecases.addAlarm(Alarm(alarmId = 912, isOn = true, isSaved = true, scheduleInitialized = true,
                snoozedUntil = 1000, snoozeCount = 3, maxSnoozes = 3,
                snooze = if (disabled) 0 else 5))
            usecases.snoozeFromNotification(912, 1000) shouldBe false
            val saved = usecases.findAlarm(912)!!
            saved.activeAt shouldBe 1000L
            saved.snoozeCount shouldBe 3
            notificationInteractor.isNotificationShown(912) shouldBe true
        }
    }

    @Test
    fun `failed notification scheduling preserves allowance and active alarm`() = runTest {
        val backend = object : AlarmInteractor by alarmInteractor {
            override suspend fun scheduleSnooze(alarm: Alarm, timeInMillis: Long) {
                error("Scheduling failed")
            }
        }
        val commands = usecases.copy(snoozeAlarm = SnoozeAlarm(dateTimeProvider, notificationInteractor, backend, repository))
        commands.addAlarm(Alarm(alarmId = 913, isOn = true, isSaved = true, scheduleInitialized = true,
            snoozedUntil = 1000, snoozeCount = 1))
        assertFailsWith<IllegalStateException> { commands.snoozeFromNotification(913, 1000) }
        val saved = commands.findAlarm(913)!!
        saved.activeAt shouldBe 1000L
        saved.snoozeCount shouldBe 1
        saved.snoozedUntil shouldBe null
        notificationInteractor.isNotificationShown(913) shouldBe true
    }

    @Test
    fun `notification snooze cannot create an occurrence for an inactive alarm`() = runTest {
        usecases.snoozeFromNotification(914, 1000) shouldBe false
        usecases.addAlarm(Alarm(alarmId = 914, isOn = true, isSaved = true, scheduleInitialized = true, pendingTimes = listOf(2000)))
        usecases.snoozeFromNotification(914, 1000) shouldBe false
        usecases.findAlarm(914)!!.snoozeCount shouldBe 0
    }

}

private fun AlarmMathViewModel.resultEvents() = kotlinx.coroutines.flow.flow {
    val delivered = mutableSetOf<Long>()
    state.collect { value -> value.results.forEach { if (delivered.add(it.id)) { emit(it.outcome); acknowledgeResult(it.id) } } }
}
