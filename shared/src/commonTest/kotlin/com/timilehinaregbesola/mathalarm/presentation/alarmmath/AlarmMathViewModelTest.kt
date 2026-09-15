package com.timilehinaregbesola.mathalarm.presentation.alarmmath

import app.cash.turbine.test
import co.touchlab.kermit.Logger
import com.timilehinaregbesola.mathalarm.data.AlarmRepository
import com.timilehinaregbesola.mathalarm.domain.model.Alarm
import com.timilehinaregbesola.mathalarm.fake.AlarmInteractorFake
import com.timilehinaregbesola.mathalarm.fake.AlarmRepositoryFake
import com.timilehinaregbesola.mathalarm.fake.AlarmTimeCalculatorFake
import com.timilehinaregbesola.mathalarm.fake.AudioPlayerFake
import com.timilehinaregbesola.mathalarm.fake.DateTimeProviderFake
import com.timilehinaregbesola.mathalarm.fake.NotificationInteractorFake
import com.timilehinaregbesola.mathalarm.framework.Usecases
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
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
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
            progressStore = progressStore
        )
    }

    @Test
    fun `challenge advances only on correct answers and keeps audio playing until final completion`() = runTest {
        val alarm = Alarm(alarmId = 90, difficulty = 3, questionCount = 3, challengeOperations = "+")
        viewModel.initializeChallenge(alarm, preview = true)
        audioPlayer.startAlarmAudio()
        viewModel.eventFlow.test {
            val first = viewModel.currentProblem!!
            viewModel.onEvent(MathScreenEvent.EnteredAnswer("-1"))
            viewModel.onEvent(MathScreenEvent.OnEnterClick(first))
            awaitItem() shouldBe AlarmMathViewModel.UiEvent.ShowError(AlarmErrorMessage.INCORRECT_ANSWER)
            viewModel.questionIndex.value shouldBe 0
            repeat(3) { index ->
                val problem = viewModel.currentProblem!!
                viewModel.onEvent(MathScreenEvent.EnteredAnswer(problem.answer.toString()))
                viewModel.onEvent(MathScreenEvent.OnEnterClick(problem))
                if (index < 2) {
                    expectNoEvents()
                    viewModel.questionIndex.value shouldBe index + 1
                    audioPlayer.isPlaying shouldBe true
                    // Rotation/recomposition must not restart an in-progress challenge.
                    viewModel.initializeChallenge(alarm, preview = true)
                    viewModel.questionIndex.value shouldBe index + 1
                } else {
                    awaitItem() shouldBe AlarmMathViewModel.UiEvent.CompleteAndClose
                }
            }
        }
    }

    @Test
    fun `scheduled alarms recover saved challenge while test alarm uses draft`() = runTest {
        val saved = Alarm(alarmId = 91, difficulty = 3, questionCount = 7, challengeOperations = "÷", factorRange = 2)
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
        usecases.addAlarm(Alarm(alarmId = 92, difficultyMix = "00112", questionCount = 5))
        viewModel.initializeChallenge(Alarm(alarmId = 92), preview = false)
        viewModel.questionCount shouldBe 5
        viewModel.initializeChallenge(Alarm(alarmId = 92, difficultyMix = "12", questionCount = 2), preview = true)
        viewModel.questionCount shouldBe 2
    }

    private fun recreatedViewModel() = AlarmMathViewModel(
        usecases, AudioPlayerFake(), Logger.withTag("restored"), ChallengeProgressStore(progressSettings)
    )

    @Test
    fun `progress and exact questions survive a new view model and store`() = runTest {
        val alarm = Alarm(alarmId = 501, activeAt = 1000, questionCount = 10)
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
        restored.questionIndex.value shouldBe 9
        restored.questionCount shouldBe 10
        restored.currentProblem shouldBe lastProblem
    }

    @Test
    fun `new occurrences reset progress and previews do not overwrite it`() = runTest {
        val alarm = Alarm(alarmId = 502, activeAt = 1000, questionCount = 3)
        usecases.addAlarm(alarm)
        viewModel.initializeChallenge(alarm, preview = false)
        val problem = viewModel.currentProblem!!
        viewModel.onEvent(MathScreenEvent.EnteredAnswer(problem.answer.toString()))
        viewModel.onEvent(MathScreenEvent.OnEnterClick(problem))
        val preview = recreatedViewModel()
        preview.initializeChallenge(alarm, preview = true)
        preview.questionIndex.value shouldBe 0
        preview.completeAlarm(alarm, preview = true)
        advanceUntilIdle()
        progressStore.load(502, 1000)!!.questionIndex shouldBe 1
        usecases.addAlarm(alarm.copy(activeAt = 2000))
        val next = recreatedViewModel()
        next.initializeChallenge(alarm, preview = false)
        next.questionIndex.value shouldBe 0
        progressStore.load(502, 2000)!!.questionIndex shouldBe 0
    }

    @Test
    fun `successful completion and snooze clear persisted progress`() = runTest {
        for (snooze in listOf(false, true)) {
            val alarm = Alarm(alarmId = 503, activeAt = 1000, questionCount = 3, isOn = true)
            usecases.addAlarm(alarm)
            val vm = recreatedViewModel()
            vm.initializeChallenge(alarm, preview = false)
            if (snooze) vm.onEvent(MathScreenEvent.OnSnoozeClick(alarm.alarmId))
            else vm.completeAlarm(alarm)
            advanceUntilIdle()
            progressStore.load(503, 1000) shouldBe null
        }
    }

    @Test
    fun `enter before initialization and stale questions cannot complete a challenge`() = runTest {
        viewModel.eventFlow.test {
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
            viewModel.questionIndex.value shouldBe 1
            expectNoEvents()
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
        val alarm = Alarm(alarmId = 804, isOn = true, snooze = 5, activeAt = 1000, questionCount = 3)
        commands.addAlarm(alarm)
        viewModel.initializeChallenge(alarm, preview = false)
        viewModel.eventFlow.test {
            viewModel.onEvent(MathScreenEvent.OnSnoozeClick(804))
            awaitItem() shouldBe AlarmMathViewModel.UiEvent.ShowError(AlarmErrorMessage.SNOOZE)
            advanceUntilIdle()
            expectNoEvents()
        }
        commands.findAlarm(804)!!.snoozedUntil shouldBe null
        progressStore.load(804, 1000)!!.questionIndex shouldBe 0
    }

    @Test
    fun `initial answer should be empty`() {
        viewModel.answerText.value shouldBe ""
    }

    @Test
    fun `onEvent with correct answer should emit CompleteAndClose event`() = runTest {
        viewModel.initializeChallenge(Alarm(), preview = true)
        val problem = viewModel.currentProblem!!
        
        viewModel.eventFlow.test {
            viewModel.onEvent(MathScreenEvent.EnteredAnswer(problem.answer.toString()))
            viewModel.onEvent(MathScreenEvent.OnEnterClick(problem))

            val lastEvent = awaitItem()
            lastEvent shouldBe AlarmMathViewModel.UiEvent.CompleteAndClose
            
            viewModel.answerText.value shouldBe ""
            
            // Audio is stopped only after the completion command succeeds.
        }
    }

    @Test
    fun `onEvent with incorrect answer should show error snackbar`() = runTest {
        viewModel.initializeChallenge(Alarm(), preview = true)
        val problem = viewModel.currentProblem!!
        
        viewModel.eventFlow.test {
            viewModel.onEvent(MathScreenEvent.EnteredAnswer("-1")) // Wrong answer
            viewModel.onEvent(MathScreenEvent.OnEnterClick(problem))
            
            val event = awaitItem()
            event shouldBe AlarmMathViewModel.UiEvent.ShowError(AlarmErrorMessage.INCORRECT_ANSWER)
        }
    }

    @Test
    fun `onEvent with blank answer should show error snackbar`() = runTest {
        viewModel.initializeChallenge(Alarm(), preview = true)
        val problem = viewModel.currentProblem!!
        
        viewModel.eventFlow.test {
            viewModel.onEvent(MathScreenEvent.EnteredAnswer(""))
            viewModel.onEvent(MathScreenEvent.OnEnterClick(problem))
            
            awaitItem() shouldBe AlarmMathViewModel.UiEvent.ShowError(AlarmErrorMessage.INCORRECT_ANSWER)
        }
    }

    @Test
    fun `onEvent OnClearClick should clear answer text`() {
        viewModel.onEvent(MathScreenEvent.EnteredAnswer("123"))
        
        viewModel.onEvent(MathScreenEvent.OnClearClick)
        
        viewModel.answerText.value shouldBe ""
    }

    @Test
    fun `onEvent EnteredAnswer should update answer text`() {
        viewModel.onEvent(MathScreenEvent.EnteredAnswer("42"))
        
        viewModel.answerText.value shouldBe "42"
    }

    @Test
    fun `onEvent OnSnoozeClick should snooze alarm and stop audio`() = runTest {
        val alarm = Alarm(alarmId = 123L, hour = 8, minute = 0, isSaved = true, isOn = true)
        usecases.addAlarm(alarm)
        advanceUntilIdle()
        
        viewModel.eventFlow.test {
            viewModel.onEvent(MathScreenEvent.OnSnoozeClick(alarm.alarmId))
            
            advanceUntilIdle()
            
            audioPlayer.isStopped shouldBe true
            
            val event = awaitItem()
            event shouldBe AlarmMathViewModel.UiEvent.StopVibrateAndHideKeyboard
            awaitItem() shouldBe AlarmMathViewModel.UiEvent.Close
        }
    }

    @Test
    fun `onEvent OnToneError should emit a localizable error`() = runTest {
        val errorMessage = "Failed to load tone"
        
        viewModel.eventFlow.test {
            viewModel.onEvent(MathScreenEvent.OnToneError(errorMessage))
            
            val event = awaitItem()
            event shouldBe AlarmMathViewModel.UiEvent.ShowError(AlarmErrorMessage.TONE)
        }
    }

    @Test
    fun `startAlarmWith should initialize and play audio with tone`() {
        val tone = "content://media/internal/audio/media/123"
        
        viewModel.startAlarmWith(tone)
        
        audioPlayer.isInitialized shouldBe true
        audioPlayer.isReset shouldBe true
        audioPlayer.dataSource shouldBe tone
        audioPlayer.isPlaying shouldBe true
    }

    @Test
    fun `completeAlarm should call completeAlarm use case`() = runTest {
        val alarm = Alarm(alarmId = 456, hour = 9, minute = 0, isOn = true, isSaved = true)
        usecases.addAlarm(alarm)
        advanceUntilIdle()
        
        viewModel.completeAlarm(alarm)
        advanceUntilIdle()
        
        val completedAlarm = usecases.findAlarm(alarm.alarmId)
        completedAlarm shouldBe alarm.copy(isOn = false)
    }

    @Test
    fun `answer with correct value after trimming whitespace should be accepted`() = runTest {
        viewModel.initializeChallenge(Alarm(), preview = true)
        val problem = viewModel.currentProblem!!
        
        viewModel.eventFlow.test {
            viewModel.onEvent(MathScreenEvent.EnteredAnswer("  ${problem.answer}  ")) // With whitespace
            viewModel.onEvent(MathScreenEvent.OnEnterClick(problem))
            
            val nextEvent = awaitItem()
            nextEvent shouldBe AlarmMathViewModel.UiEvent.CompleteAndClose
        }
    }

    @Test
    fun `multiple incorrect answers should show error each time`() = runTest {
        viewModel.initializeChallenge(Alarm(), preview = true)
        val problem = viewModel.currentProblem!!
        
        viewModel.eventFlow.test {
            viewModel.onEvent(MathScreenEvent.EnteredAnswer("-1"))
            viewModel.onEvent(MathScreenEvent.OnEnterClick(problem))
            awaitItem() shouldBe AlarmMathViewModel.UiEvent.ShowError(AlarmErrorMessage.INCORRECT_ANSWER)
            
            viewModel.onEvent(MathScreenEvent.EnteredAnswer("-2"))
            viewModel.onEvent(MathScreenEvent.OnEnterClick(problem))
            awaitItem() shouldBe AlarmMathViewModel.UiEvent.ShowError(AlarmErrorMessage.INCORRECT_ANSWER)
            
            viewModel.onEvent(MathScreenEvent.EnteredAnswer(problem.answer.toString()))
            viewModel.onEvent(MathScreenEvent.OnEnterClick(problem))
            awaitItem() shouldBe AlarmMathViewModel.UiEvent.CompleteAndClose
        }
    }
    @Test fun `preview completion never mutates the saved alarm`() = runTest {
        val alarm = Alarm(alarmId = 778, isOn = true, isSaved = true)
        usecases.addAlarm(alarm)
        viewModel.eventFlow.test {
            viewModel.completeAlarm(alarm, preview = true)
            awaitItem() shouldBe AlarmMathViewModel.UiEvent.StopVibrateAndHideKeyboard
            awaitItem() shouldBe AlarmMathViewModel.UiEvent.Close
        }
        usecases.findAlarm(778)?.isOn shouldBe true
    }
    @Test fun `preview snooze never schedules a live occurrence`() = runTest {
        val alarm = Alarm(alarmId = 779, isOn = true, isSaved = true)
        usecases.addAlarm(alarm)
        viewModel.eventFlow.test {
            viewModel.onEvent(MathScreenEvent.OnSnoozeClick(779, preview = true))
            awaitItem() shouldBe AlarmMathViewModel.UiEvent.StopVibrateAndHideKeyboard
            awaitItem() shouldBe AlarmMathViewModel.UiEvent.Close
        }
        usecases.findAlarm(779)?.snoozedUntil shouldBe null
    }

}
