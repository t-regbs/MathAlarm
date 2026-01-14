package com.timilehinaregbesola.mathalarm.presentation.alarmsettings

import androidx.compose.ui.text.input.TextFieldValue
import app.cash.turbine.test
import com.timilehinaregbesola.mathalarm.data.AlarmRepository
import com.timilehinaregbesola.mathalarm.domain.model.Alarm
import com.timilehinaregbesola.mathalarm.fake.*
import com.timilehinaregbesola.mathalarm.framework.Usecases
import com.timilehinaregbesola.mathalarm.usecases.*
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

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
            deleteAlarm = DeleteAlarm(repository, alarmInteractor),
            getSavedAlarms = GetSavedAlarms(repository),
            scheduleAlarm = ScheduleAlarm(repository, alarmInteractor, alarmTimeCalculator),
            showAlarm = ShowAlarm(repository, notificationInteractor),
            completeAlarm = CompleteAlarm(repository, alarmInteractor, notificationInteractor, scheduleNextAlarm),
            updateAlarm = UpdateAlarm(repository),
            cancelAlarm = CancelAlarm(alarmInteractor),
            clearAlarms = ClearAlarms(repository, alarmInteractor),
            scheduleNextAlarm = scheduleNextAlarm,
            rescheduleFutureAlarms = RescheduleFutureAlarms(repository, alarmInteractor, alarmTimeCalculator, scheduleNextAlarm),
            snoozeAlarm = SnoozeAlarm(dateTimeProvider, notificationInteractor, alarmInteractor, repository)
        )
        
        viewModel = AlarmSettingsViewModel(usecases = usecases)
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state should have default values`() {
        with(viewModel) {
            alarmTime.value shouldBe TimeState()
            alarmTitle.value.text shouldBe "Good day"
            dayChooser.value shouldBe "FFFFFFF"
            repeatWeekly.value shouldBe false
            vibrate.value shouldBe false
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
    fun `onEvent OnToneError should emit ShowSnackbar event`() = runTest {
        val errorMessage = "Failed to load tone"
        
        viewModel.eventFlow.test {
            viewModel.onEvent(AddEditAlarmEvent.OnToneError(errorMessage))
            
            val event = awaitItem()
            event.shouldBeInstanceOf<AlarmSettingsViewModel.UiEvent.ShowSnackbar>()
            event.message shouldBe errorMessage
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
        val newAlarm = Alarm(alarmId = 789, hour = 6, minute = 45, repeatDays = "FFFFFFF", alarmTone = "test_tone")
        
        viewModel.setAlarm(newAlarm)

        with(viewModel) {
            currentAlarmId shouldBe 789
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
