package com.timilehinaregbesola.mathalarm.presentation.alarmsettings

import com.timilehinaregbesola.mathalarm.provider.AlarmTimeCalculatorImpl
import kotlinx.datetime.toInstant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.LocalDateTime
import androidx.compose.ui.text.input.TextFieldValue
import app.cash.turbine.test
import com.timilehinaregbesola.mathalarm.data.AlarmRepository
import com.timilehinaregbesola.mathalarm.domain.model.Alarm
import com.timilehinaregbesola.mathalarm.fake.*
import com.timilehinaregbesola.mathalarm.framework.Usecases
import com.timilehinaregbesola.mathalarm.interactors.AlarmInteractor
import com.timilehinaregbesola.mathalarm.usecases.*
import com.timilehinaregbesola.mathalarm.utils.AlarmErrorMessage
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class AlarmSettingsViewModelTest {

    private lateinit var viewModel: AlarmSettingsViewModel
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
        
        dataSource = AlarmRepositoryFake()
        repository = AlarmRepository(dataSource)
        alarmInteractor = AlarmInteractorFake()
        notificationInteractor = NotificationInteractorFake()
        dateTimeProvider = DateTimeProviderFake()
        
        val alarmTimeCalculator = AlarmTimeCalculatorFake()
        val scheduleNextAlarm = ScheduleNextAlarm(alarmInteractor, alarmTimeCalculator)
        
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
            rescheduleFutureAlarms = RescheduleFutureAlarms(repository, alarmInteractor, alarmTimeCalculator),
            snoozeAlarm = SnoozeAlarm(dateTimeProvider, notificationInteractor, alarmInteractor, repository)
        )
        
        viewModel = AlarmSettingsViewModel(usecases = usecases)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `failed save emits a localizable error and keeps the editor open`() = runTest {
        val backend = object : AlarmInteractor by alarmInteractor {
            override suspend fun schedule(alarm: Alarm, timeInMillis: Long) {
                error("Internal OS scheduling details")
            }
        }
        val commands = usecases.copy(scheduleAlarm = ScheduleAlarm(repository, backend, AlarmTimeCalculatorFake()))
        viewModel = AlarmSettingsViewModel(commands)
        viewModel.setAlarm(Alarm(isOn = true, alarmTone = "test_tone"))
        advanceUntilIdle()
        viewModel.eventFlow.test {
            viewModel.onEvent(AddEditAlarmEvent.OnSaveTodoClick)
            awaitItem() shouldBe AlarmSettingsViewModel.UiEvent.ShowError(AlarmErrorMessage.SAVE)
            advanceUntilIdle()
            expectNoEvents()
        }
    }

    @Test
    fun `changing snooze preserves remaining one time dates`() = runTest {
        dateTimeProvider.setFixedDateTime(LocalDateTime(2030, 1, 8, 6, 0))
        val calculator = AlarmTimeCalculatorImpl(dateTimeProvider)
        val commands = usecases.copy(scheduleAlarm = ScheduleAlarm(repository, alarmInteractor, calculator))
        viewModel = AlarmSettingsViewModel(commands)
        val remaining = LocalDateTime(2030, 1, 9, 7, 0)
            .toInstant(TimeZone.currentSystemDefault()).toEpochMilliseconds()
        val alarm = Alarm(
            alarmId = 881, hour = 7, minute = 0, isOn = true, isSaved = true,
            alarmTone = "test_tone", repeatDays = "FTFTFFF", scheduleInitialized = true,
            pendingTimes = listOf(remaining), snooze = 5
        )
        commands.addAlarm(alarm)
        viewModel.setAlarm(alarm)
        for (enabled in listOf(false, true)) {
            viewModel.onEvent(AddEditAlarmEvent.ToggleSnooze(enabled))
            viewModel.onEvent(AddEditAlarmEvent.OnSaveTodoClick)
            advanceUntilIdle()
            commands.findAlarm(881)!!.pendingTimes shouldBe listOf(remaining)
        }
    }

    @Test
    fun `disabling snooze cancels pending snooze without changing normal occurrences`() = runTest {
        var canceledSnoozeId: Long? = null
        var platformUpdate: Alarm? = null
        val backend = object : AlarmInteractor by alarmInteractor {
            override fun cancelSnooze(alarm: Alarm) { canceledSnoozeId = alarm.alarmId }
            override suspend fun update(alarm: Alarm) { platformUpdate = alarm }
        }
        val commands = usecases.copy(updateAlarm = UpdateAlarm(repository, backend))
        viewModel = AlarmSettingsViewModel(commands)
        val alarm = Alarm(
            alarmId = 882, isOn = true, isSaved = true, alarmTone = "test_tone",
            pendingTimes = listOf(2_000_000_000_000), snoozedUntil = 1_999_999_000_000,
            scheduleInitialized = true, snooze = 5
        )
        commands.addAlarm(alarm)
        viewModel.setAlarm(alarm)
        viewModel.onEvent(AddEditAlarmEvent.ToggleSnooze(false))
        viewModel.onEvent(AddEditAlarmEvent.OnSaveTodoClick)
        advanceUntilIdle()
        val updated = commands.findAlarm(882)!!
        canceledSnoozeId shouldBe 882L
        updated.snoozedUntil shouldBe null
        updated.pendingTimes shouldBe alarm.pendingTimes
        platformUpdate?.snooze shouldBe 0
        platformUpdate?.snoozedUntil shouldBe null
        platformUpdate?.pendingTimes shouldBe alarm.pendingTimes
    }

    @Test
    fun `initial state should have default values`() {
        with(viewModel) {
            alarmTime.value shouldBe TimeState()
            alarmTitle.value.text shouldBe "Good day"
            dayChooser.value shouldBe "FFFFFFF"
            repeatWeekly.value shouldBe false
            vibrate.value shouldBe false
            snoozeEnabled.value shouldBe true
            difficulty.value shouldBe 0
            isOn.value shouldBe false
            isSaved.value shouldBe false
        }
    }

    @Test
    fun `onEvent ChangeTime should update alarm time`() {
        val newTime = TimeState(hour = 8, minute = 30, formattedTime = "08:30 AM")
        
        viewModel.onEvent(AddEditAlarmEvent.ChangeTime(newTime))
        
        viewModel.alarmTime.value shouldBe newTime
    }

    @Test
    fun `onEvent EnteredTitle should update alarm title`() {
        val newTitle = TextFieldValue("Wake up!")
        
        viewModel.onEvent(AddEditAlarmEvent.EnteredTitle(newTitle))
        
        viewModel.alarmTitle.value shouldBe newTitle
    }

    @Test
    fun `onEvent ToggleRepeat should update repeat weekly state`() {
        viewModel.onEvent(AddEditAlarmEvent.ToggleRepeat(true))
        
        viewModel.repeatWeekly.value shouldBe true
        
        viewModel.onEvent(AddEditAlarmEvent.ToggleRepeat(false))
        
        viewModel.repeatWeekly.value shouldBe false
    }

    @Test
    fun `onEvent ToggleVibrate should update vibrate state`() {
        viewModel.onEvent(AddEditAlarmEvent.ToggleVibrate(true))
        
        viewModel.vibrate.value shouldBe true
    }

    @Test
    fun `onEvent ToggleSnooze should update snooze enabled state`() {
        viewModel.onEvent(AddEditAlarmEvent.ToggleSnooze(false))

        viewModel.snoozeEnabled.value shouldBe false

        viewModel.onEvent(AddEditAlarmEvent.ToggleSnooze(true))

        viewModel.snoozeEnabled.value shouldBe true
    }

    @Test
    fun `onEvent ToggleDayChooser should update day chooser state`() {
        val selectedDays = "TFTFTFT" // Monday, Wednesday, Friday, Sunday
        
        viewModel.onEvent(AddEditAlarmEvent.ToggleDayChooser(selectedDays))
        
        viewModel.dayChooser.value shouldBe selectedDays
    }

    @Test
    fun `onEvent OnDifficultyChange should update difficulty`() {
        viewModel.onEvent(AddEditAlarmEvent.OnDifficultyChange(2)) // HARD
        
        viewModel.difficulty.value shouldBe 2
    }

    @Test
    fun `onEvent OnToneChange should update tone`() {
        val toneUri = "content://media/internal/audio/media/123"
        
        viewModel.onEvent(AddEditAlarmEvent.OnToneChange(toneUri))
        
        viewModel.tone.value shouldBe toneUri
    }

    @Test
    fun `onEvent OnToneError should emit a localizable error`() = runTest {
        val errorMessage = "Failed to load tone"
        
        viewModel.eventFlow.test {
            viewModel.onEvent(AddEditAlarmEvent.OnToneError(errorMessage))
            
            val event = awaitItem()
            event.shouldBeInstanceOf<AlarmSettingsViewModel.UiEvent.ShowError>()
            event.error shouldBe AlarmErrorMessage.TONE
        }
    }

    @Test
    fun `onEvent OnTestClick should emit TestAlarm event`() = runTest {
        val testAlarm = Alarm(alarmId = 123, hour = 9, minute = 0, alarmTone = "test_tone")
        viewModel.setAlarm(testAlarm)
        
        viewModel.eventFlow.test {
            viewModel.onEvent(AddEditAlarmEvent.OnTestClick)
            
            val event = awaitItem()
            event.shouldBeInstanceOf<AlarmSettingsViewModel.UiEvent.TestAlarm>()
        }
    }

    @Test
    fun `onEvent OnSaveTodoClick should save new alarm and emit SaveAlarm event`() = runTest {
        val newAlarm = Alarm(alarmId = 456, hour = 7, minute = 30, alarmTone = "test_tone")
        viewModel.setAlarm(newAlarm)
        viewModel.onEvent(AddEditAlarmEvent.ChangeTime(TimeState(hour = 8, minute = 0)))
        viewModel.onEvent(AddEditAlarmEvent.EnteredTitle(TextFieldValue("Morning alarm")))
        viewModel.onEvent(AddEditAlarmEvent.ToggleVibrate(true))
        
        viewModel.eventFlow.test {
            viewModel.onEvent(AddEditAlarmEvent.OnSaveTodoClick)
            advanceUntilIdle()
            
            val event = awaitItem()
            event.shouldBeInstanceOf<AlarmSettingsViewModel.UiEvent.SaveAlarm>()
            
            val savedAlarm = usecases.findAlarm(newAlarm.alarmId)
            savedAlarm?.let {
                it.isSaved shouldBe true
                it.hour shouldBe 8
                it.minute shouldBe 0
                it.vibrate shouldBe true
            }
        }
    }

    @Test
    fun `setAlarm with new alarm should initialize with default day`() {
        val newAlarm = Alarm(alarmId = 0, hour = 6, minute = 45, repeatDays = "FFFFFFF", alarmTone = "test_tone")
        
        viewModel.setAlarm(newAlarm)

        with(viewModel) {
            currentAlarmId shouldBe 0
            alarmTime.value.hour shouldBe 6
            alarmTime.value.minute shouldBe 45
            dayChooser.value.count { it == 'T' } shouldBe 1
        }
    }

    @Test
    fun `setAlarm with existing alarm should load all properties`() {
        val existingAlarm = Alarm(
            alarmId = 999,
            hour = 10,
            minute = 15,
            repeat = true,
            repeatDays = "TFTFTFT",
            vibrate = true,
            difficulty = 2,
            alarmTone = "content://test/tone",
            title = "Test+Alarm",
            isOn = true,
            isSaved = true
        )
        
        viewModel.setAlarm(existingAlarm)

        with(viewModel) {
            currentAlarmId shouldBe 999
            alarmTime.value.hour shouldBe 10
            alarmTime.value.minute shouldBe 15
            repeatWeekly.value shouldBe true
            dayChooser.value shouldBe "TFTFTFT"
            vibrate.value shouldBe true
            difficulty.value shouldBe 2
            tone.value shouldBe "content://test/tone"
            alarmTitle.value.text shouldBe "Test Alarm"
            isOn.value shouldBe true
            isSaved.value shouldBe true
        }
    }

    @Test
    fun `setAlarm should disable snooze when saved snooze is zero`() {
        val alarm = Alarm(alarmId = 777, alarmTone = "test_tone", snooze = 0)

        viewModel.setAlarm(alarm)

        viewModel.snoozeEnabled.value shouldBe false
    }

    @Test
    fun `changing time on existing alarm should trigger reschedule`() = runTest {
        val existingAlarm = Alarm(
            alarmId = 111,
            hour = 9,
            minute = 0,
            repeatDays = "TTTTTTT",
            isOn = true,
            alarmTone = "test_tone"
        )
        viewModel.setAlarm(existingAlarm)
        
        viewModel.onEvent(AddEditAlarmEvent.ChangeTime(TimeState(hour = 10, minute = 0)))
        
        viewModel.alarmTime.value.hour shouldBe 10
        viewModel.alarmTime.value.minute shouldBe 0
    }

    @Test
    fun `saving alarm with repeat weekly should schedule with repeat`() = runTest {
        val alarm = Alarm(alarmId = 222, hour = 7, minute = 0, alarmTone = "test_tone")
        viewModel.setAlarm(alarm)
        viewModel.onEvent(AddEditAlarmEvent.ToggleRepeat(true))
        viewModel.onEvent(AddEditAlarmEvent.ToggleDayChooser("TTTTTTT")) // All days
        
        viewModel.eventFlow.test {
            viewModel.onEvent(AddEditAlarmEvent.OnSaveTodoClick)
            advanceUntilIdle()
            
            awaitItem().shouldBeInstanceOf<AlarmSettingsViewModel.UiEvent.SaveAlarm>()
            
            val savedAlarm = usecases.findAlarm(alarm.alarmId)
            savedAlarm?.repeat shouldBe true
            savedAlarm?.repeatDays shouldBe "TTTTTTT"
        }
    }

    @Test
    fun `saving alarm with snooze disabled should save snooze as zero`() = runTest {
        val alarm = Alarm(alarmId = 223, hour = 7, minute = 0, alarmTone = "test_tone")
        viewModel.setAlarm(alarm)
        viewModel.onEvent(AddEditAlarmEvent.ToggleSnooze(false))

        viewModel.eventFlow.test {
            viewModel.onEvent(AddEditAlarmEvent.OnSaveTodoClick)
            advanceUntilIdle()

            awaitItem().shouldBeInstanceOf<AlarmSettingsViewModel.UiEvent.SaveAlarm>()

            val savedAlarm = usecases.findAlarm(alarm.alarmId)
            savedAlarm?.snooze shouldBe 0
        }
    }

    @Test
    fun `saving alarm with snooze enabled should save default snooze minutes`() = runTest {
        val alarm = Alarm(alarmId = 224, hour = 7, minute = 0, alarmTone = "test_tone", snooze = 0)
        viewModel.setAlarm(alarm)
        viewModel.onEvent(AddEditAlarmEvent.ToggleSnooze(true))

        viewModel.eventFlow.test {
            viewModel.onEvent(AddEditAlarmEvent.OnSaveTodoClick)
            advanceUntilIdle()

            awaitItem().shouldBeInstanceOf<AlarmSettingsViewModel.UiEvent.SaveAlarm>()

            val savedAlarm = usecases.findAlarm(alarm.alarmId)
            savedAlarm?.snooze shouldBe 5
        }
    }

    @Test
    fun `saving existing on alarm after disabling snooze should retain its schedule with snooze disabled`() = runTest {
        val existingAlarm = Alarm(
            alarmId = 225,
            hour = 7,
            minute = 0,
            repeatDays = "FTFFFFF",
            isOn = true,
            isSaved = true,
            alarmTone = "test_tone",
        )
        usecases.addAlarm(existingAlarm)
        val trigger = 2_000_000_000_000L
        alarmInteractor.schedule(existingAlarm, trigger)
        viewModel.setAlarm(existingAlarm)
        viewModel.onEvent(AddEditAlarmEvent.ToggleSnooze(false))

        viewModel.eventFlow.test {
            viewModel.onEvent(AddEditAlarmEvent.OnSaveTodoClick)
            advanceUntilIdle()

            awaitItem().shouldBeInstanceOf<AlarmSettingsViewModel.UiEvent.SaveAlarm>()

            val savedAlarm = usecases.findAlarm(existingAlarm.alarmId)
            savedAlarm?.snooze shouldBe 0
            alarmInteractor.getAlarmTimeMillis(existingAlarm.alarmId) shouldBe trigger
            alarmInteractor.getScheduledAlarms()[existingAlarm.alarmId]?.updated shouldBe true
        }
    }

    @Test
    fun `saving existing off alarm after disabling snooze should not turn it on`() = runTest {
        val existingAlarm = Alarm(
            alarmId = 226,
            hour = 7,
            minute = 0,
            repeatDays = "FTFFFFF",
            isOn = false,
            isSaved = true,
            alarmTone = "test_tone",
        )
        viewModel.setAlarm(existingAlarm)
        viewModel.onEvent(AddEditAlarmEvent.ToggleSnooze(false))

        viewModel.eventFlow.test {
            viewModel.onEvent(AddEditAlarmEvent.OnSaveTodoClick)
            advanceUntilIdle()

            awaitItem().shouldBeInstanceOf<AlarmSettingsViewModel.UiEvent.SaveAlarm>()

            val savedAlarm = usecases.findAlarm(existingAlarm.alarmId)
            savedAlarm?.snooze shouldBe 0
            savedAlarm?.isOn shouldBe false
        }
    }

    @Test
    fun `saving existing off alarm after changing only days must stay off`() = runTest {
        val existingAlarm = Alarm(
            alarmId = 444,
            hour = 7,
            minute = 0,
            repeat = false,
            repeatDays = "FTFFFFF", // Monday only
            isOn = false,
            isSaved = true,
            alarmTone = "test_tone"
        )
        viewModel.setAlarm(existingAlarm)
        viewModel.onEvent(AddEditAlarmEvent.ToggleDayChooser("FFTFFFF")) // Tuesday only

        viewModel.eventFlow.test {
            viewModel.onEvent(AddEditAlarmEvent.OnSaveTodoClick)
            advanceUntilIdle()

            awaitItem().shouldBeInstanceOf<AlarmSettingsViewModel.UiEvent.SaveAlarm>()

            val savedAlarm = usecases.findAlarm(existingAlarm.alarmId)
            savedAlarm?.repeatDays shouldBe "FFTFFFF"
            savedAlarm?.isOn shouldBe false
            alarmInteractor.isAlarmScheduled(savedAlarm!!) shouldBe false
        }
    }

    @Test
    fun `editing existing alarm schedule without saving should not cancel current alarm`() = runTest {
        val existingAlarm = Alarm(
            alarmId = 445,
            hour = 7,
            minute = 0,
            repeat = false,
            repeatDays = "FTFFFFF",
            isOn = true,
            isSaved = true,
            alarmTone = "test_tone"
        )
        dataSource.addAlarm(existingAlarm)
        alarmInteractor.schedule(existingAlarm, 1_000_000L)

        viewModel.setAlarm(existingAlarm)
        viewModel.onEvent(AddEditAlarmEvent.ToggleDayChooser("FFTFFFF"))
        advanceUntilIdle()

        alarmInteractor.isAlarmScheduled(existingAlarm) shouldBe true
    }

    @Test
    fun `multiple changes should be tracked correctly`() {
        val alarm = Alarm(alarmId = 333, alarmTone = "test_tone")
        viewModel.setAlarm(alarm)
        
        viewModel.onEvent(AddEditAlarmEvent.ChangeTime(TimeState(hour = 11, minute = 30)))
        viewModel.onEvent(AddEditAlarmEvent.EnteredTitle(TextFieldValue("Custom Title")))
        viewModel.onEvent(AddEditAlarmEvent.ToggleVibrate(true))
        viewModel.onEvent(AddEditAlarmEvent.OnDifficultyChange(1))
        viewModel.onEvent(AddEditAlarmEvent.ToggleRepeat(true))
        
        viewModel.alarmTime.value.hour shouldBe 11
        viewModel.alarmTime.value.minute shouldBe 30
        viewModel.alarmTitle.value.text shouldBe "Custom Title"
        viewModel.vibrate.value shouldBe true
        viewModel.difficulty.value shouldBe 1
        viewModel.repeatWeekly.value shouldBe true
    }

    @Test
    fun `setAlarm should only initialize once`() {
        val alarm1 = Alarm(alarmId = 444, hour = 8, minute = 0, alarmTone = "test_tone")
        val alarm2 = Alarm(alarmId = 555, hour = 9, minute = 0, alarmTone = "test_tone")
        
        viewModel.setAlarm(alarm1)
        viewModel.setAlarm(alarm2) // Should be ignored
        
        viewModel.currentAlarmId shouldBe 444
        viewModel.alarmTime.value.hour shouldBe 8
    }
}
