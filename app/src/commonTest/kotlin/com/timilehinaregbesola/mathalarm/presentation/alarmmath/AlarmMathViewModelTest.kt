package com.timilehinaregbesola.mathalarm.presentation.alarmmath

import app.cash.turbine.test
import co.touchlab.kermit.Logger
import com.timilehinaregbesola.mathalarm.data.AlarmRepository
import com.timilehinaregbesola.mathalarm.domain.model.Alarm
import com.timilehinaregbesola.mathalarm.fake.AlarmInteractorFake
import com.timilehinaregbesola.mathalarm.fake.AlarmRepositoryFake
import com.timilehinaregbesola.mathalarm.fake.AudioPlayerFake
import com.timilehinaregbesola.mathalarm.fake.DateTimeProviderFake
import com.timilehinaregbesola.mathalarm.fake.NotificationInteractorFake
import com.timilehinaregbesola.mathalarm.framework.Usecases
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
import com.timilehinaregbesola.mathalarm.usecases.SnoozeAlarm
import com.timilehinaregbesola.mathalarm.usecases.UpdateAlarm
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AlarmMathViewModelTest {

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
        
        audioPlayer = AudioPlayerFake()
        dataSource = AlarmRepositoryFake()
        repository = AlarmRepository(dataSource)
        alarmInteractor = AlarmInteractorFake()
        notificationInteractor = NotificationInteractorFake()
        dateTimeProvider = DateTimeProviderFake()
        
        val scheduleNextAlarm = ScheduleNextAlarm(alarmInteractor)
        
        usecases = Usecases(
            addAlarm = AddAlarm(repository),
            findAlarm = FindAlarm(repository),
            deleteAlarm = DeleteAlarm(repository, alarmInteractor),
            getSavedAlarms = GetSavedAlarms(repository),
            scheduleAlarm = ScheduleAlarm(repository, alarmInteractor),
            showAlarm = ShowAlarm(repository, notificationInteractor, scheduleNextAlarm),
            completeAlarm = CompleteAlarm(repository, alarmInteractor, notificationInteractor),
            updateAlarm = UpdateAlarm(repository),
            cancelAlarm = CancelAlarm(alarmInteractor),
            clearAlarms = ClearAlarms(repository, alarmInteractor),
            scheduleNextAlarm = scheduleNextAlarm,
            rescheduleFutureAlarms = RescheduleFutureAlarms(repository, alarmInteractor),
            snoozeAlarm = SnoozeAlarm(dateTimeProvider, notificationInteractor, alarmInteractor, repository)
        )
        
        viewModel = AlarmMathViewModel(
            usecases = usecases,
            audioPlayer = audioPlayer,
            logger = Logger.withTag("AlarmMathViewModelTest")
        )
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state should be stopped with empty answer`() {
        viewModel.answerText.value shouldBe ""
        viewModel.state.value.shouldBeInstanceOf<ToneState.Stopped>()
        viewModel.state.value.total shouldBe 0
    }

    @Test
    fun `onEvent with correct answer should emit CompleteAndClose event`() = runTest {
        val problem = MathProblem(
            operator = MathProblemOperator.Add,
            numOne = 10,
            numTwo = 20,
            answer = 30
        )
        
        viewModel.eventFlow.test {
            viewModel.onEvent(MathScreenEvent.EnteredAnswer("30"))
            viewModel.onEvent(MathScreenEvent.OnEnterClick(problem))

            awaitItem() shouldBe AlarmMathViewModel.UiEvent.StopVibrateAndHideKeyboard

            val lastEvent = awaitItem()
            lastEvent shouldBe AlarmMathViewModel.UiEvent.CompleteAndClose
            
            viewModel.answerText.value shouldBe ""
            
            audioPlayer.isStopped shouldBe true
        }
    }

    @Test
    fun `onEvent with incorrect answer should show error snackbar`() = runTest {
        val problem = MathProblem(
            operator = MathProblemOperator.Add,
            numOne = 10,
            numTwo = 20,
            answer = 30
        )
        
        viewModel.eventFlow.test {
            viewModel.onEvent(MathScreenEvent.EnteredAnswer("25")) // Wrong answer
            viewModel.onEvent(MathScreenEvent.OnEnterClick(problem))
            
            val event = awaitItem()
            event shouldBe AlarmMathViewModel.UiEvent.ShowSnackbar("Incorrect")
        }
    }

    @Test
    fun `onEvent with blank answer should show error snackbar`() = runTest {
        val problem = MathProblem(
            operator = MathProblemOperator.Add,
            numOne = 10,
            numTwo = 20,
            answer = 30
        )
        
        viewModel.eventFlow.test {
            viewModel.onEvent(MathScreenEvent.EnteredAnswer(""))
            viewModel.onEvent(MathScreenEvent.OnEnterClick(problem))
            
            expectNoEvents()
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
        val alarm = Alarm(alarmId = 123L, hour = 8, minute = 0, isSaved = true)
        usecases.addAlarm(alarm)
        advanceUntilIdle()
        
        viewModel.eventFlow.test {
            viewModel.onEvent(MathScreenEvent.OnSnoozeClick(alarm.alarmId))
            
            advanceUntilIdle()
            
            audioPlayer.isStopped shouldBe true
            
            val event = awaitItem()
            event shouldBe AlarmMathViewModel.UiEvent.StopVibrateAndHideKeyboard
        }
    }

    @Test
    fun `onEvent OnToneError should emit ShowSnackbar event`() = runTest {
        val errorMessage = "Failed to load tone"
        
        viewModel.eventFlow.test {
            viewModel.onEvent(MathScreenEvent.OnToneError(errorMessage))
            
            val event = awaitItem()
            event shouldBe AlarmMathViewModel.UiEvent.ShowSnackbar(errorMessage)
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
        val problem = MathProblem(
            operator = MathProblemOperator.Subtract,
            numOne = 50,
            numTwo = 20,
            answer = 30
        )
        
        viewModel.eventFlow.test {
            viewModel.onEvent(MathScreenEvent.EnteredAnswer("  30  ")) // With whitespace
            viewModel.onEvent(MathScreenEvent.OnEnterClick(problem))
            
            val event = awaitItem()
            event shouldBe AlarmMathViewModel.UiEvent.StopVibrateAndHideKeyboard

            val nextEvent = awaitItem()
            nextEvent shouldBe AlarmMathViewModel.UiEvent.CompleteAndClose
        }
    }

    @Test
    fun `multiple incorrect answers should show error each time`() = runTest {
        val problem = MathProblem(
            operator = MathProblemOperator.Times,
            numOne = 5,
            numTwo = 6,
            answer = 30
        )
        
        viewModel.eventFlow.test {
            viewModel.onEvent(MathScreenEvent.EnteredAnswer("25"))
            viewModel.onEvent(MathScreenEvent.OnEnterClick(problem))
            awaitItem() shouldBe AlarmMathViewModel.UiEvent.ShowSnackbar("Incorrect")
            
            viewModel.onEvent(MathScreenEvent.EnteredAnswer("28"))
            viewModel.onEvent(MathScreenEvent.OnEnterClick(problem))
            awaitItem() shouldBe AlarmMathViewModel.UiEvent.ShowSnackbar("Incorrect")
            
            viewModel.onEvent(MathScreenEvent.EnteredAnswer("30"))
            viewModel.onEvent(MathScreenEvent.OnEnterClick(problem))
            awaitItem() shouldBe AlarmMathViewModel.UiEvent.StopVibrateAndHideKeyboard
            awaitItem() shouldBe AlarmMathViewModel.UiEvent.CompleteAndClose
        }
    }
}
