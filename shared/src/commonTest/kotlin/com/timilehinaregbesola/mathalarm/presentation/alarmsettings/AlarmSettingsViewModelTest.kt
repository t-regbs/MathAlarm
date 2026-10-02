package com.timilehinaregbesola.mathalarm.presentation.alarmsettings

import com.timilehinaregbesola.mathalarm.provider.AlarmTimeCalculatorImpl
import kotlinx.datetime.toInstant
import kotlinx.datetime.TimeZone
import kotlinx.datetime.LocalDateTime
import com.timilehinaregbesola.mathalarm.domain.model.MathChallenge
import com.timilehinaregbesola.mathalarm.domain.model.mathChallenge
import app.cash.turbine.test
import com.timilehinaregbesola.mathalarm.data.AlarmRepository
import com.timilehinaregbesola.mathalarm.domain.model.Alarm
import com.timilehinaregbesola.mathalarm.fake.*
import com.timilehinaregbesola.mathalarm.framework.Usecases
import com.timilehinaregbesola.mathalarm.interactors.AlarmInteractor
import com.timilehinaregbesola.mathalarm.usecases.*
import com.timilehinaregbesola.mathalarm.utils.AlarmErrorMessage
import com.timilehinaregbesola.mathalarm.sound.AlarmSoundCatalog
import com.timilehinaregbesola.mathalarm.platform.isIosPlatform
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.launch
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.cancel
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*

@OptIn(ExperimentalCoroutinesApi::class)
class AlarmSettingsViewModelTest {

    @Test
    fun `failed new alarm scheduling retains its allocated identity for retry`() = runTest {
        var failSchedule = true
        val backend = object : AlarmInteractor by alarmInteractor {
            override suspend fun schedule(alarm: Alarm, timeInMillis: Long) {
                if (failSchedule) error("registration unavailable")
                alarmInteractor.schedule(alarm, timeInMillis)
            }
        }
        val commands = usecases.copy(scheduleAlarm = ScheduleAlarm(repository, backend, AlarmTimeCalculatorFake()))
        viewModel = AlarmSettingsViewModel(commands, permission)
        viewModel.setAlarm(Alarm(alarmTone = "test_tone"))
        viewModel.onEvent(AddEditAlarmEvent.EnteredTitle("Retained scheduling draft"))
        viewModel.onEvent(AddEditAlarmEvent.ChangeTime(TimeState(8, 30)))
        viewModel.onEvent(AddEditAlarmEvent.OnSaveTodoClick)
        advanceUntilIdle()
        val inserted = usecases.getSavedAlarms().first().single()
        (inserted.alarmId != 0L) shouldBe true
        viewModel.currentAlarmId shouldBe inserted.alarmId
        inserted.scheduleError shouldBe "registration unavailable"
        inserted.pendingTimes.isNotEmpty() shouldBe true
        alarmInteractor.getScheduledAlarms().isEmpty() shouldBe true
        viewModel.state.value.alarmTitle shouldBe "Retained scheduling draft"
        viewModel.state.value.alarmTime shouldBe TimeState(8, 30)
        viewModel.state.value.hasUnsavedChanges shouldBe true
        val failure = viewModel.state.value.results.single()
        failure.event shouldBe AlarmSettingsViewModel.UiEvent.ShowError(AlarmErrorMessage.SAVE)
        failSchedule = false
        viewModel.onEvent(AddEditAlarmEvent.OnSaveTodoClick)
        advanceUntilIdle()
        val saved = usecases.getSavedAlarms().first().single()
        saved.alarmId shouldBe inserted.alarmId
        viewModel.currentAlarmId shouldBe inserted.alarmId
        saved.title shouldBe "Retained scheduling draft"
        saved.hour shouldBe 8
        saved.minute shouldBe 30
        saved.scheduleError shouldBe null
        saved.pendingTimes shouldBe inserted.pendingTimes
        listOf(alarmInteractor.getScheduledAlarms()[saved.alarmId]!!.timeInMillis) shouldBe saved.pendingTimes
        viewModel.state.value.hasUnsavedChanges shouldBe false
        viewModel.state.value.results.first() shouldBe failure
        viewModel.state.value.results.last().event shouldBe AlarmSettingsViewModel.UiEvent.SaveAlarm
    }

    @Test
    fun `cancelling a result waiter preserves a later accepted save failure and retry`() = runTest {
        viewModel.setAlarm(Alarm(alarmTone = "test_tone"))
        viewModel.onEvent(AddEditAlarmEvent.EnteredTitle("Retained waiter draft"))
        permission.setPermission(false)
        val locked = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        val command = launch { usecases.command { locked.complete(Unit); release.await() } }
        locked.await()
        val waiter = async { viewModel.awaitResult(0L) }
        runCurrent()
        viewModel.onEvent(AddEditAlarmEvent.OnSaveTodoClick)
        waiter.cancelAndJoin()
        release.complete(Unit)
        command.join()
        advanceUntilIdle()
        val failure = viewModel.state.value.results.single()
        failure.event shouldBe AlarmSettingsViewModel.UiEvent.RequestExactAlarmPermission
        viewModel.awaitResult(0L) shouldBe failure
        viewModel.state.value.alarmTitle shouldBe "Retained waiter draft"
        viewModel.state.value.hasUnsavedChanges shouldBe true
        usecases.getSavedAlarms().first() shouldBe emptyList()
        alarmInteractor.getScheduledAlarms().isEmpty() shouldBe true
        permission.setPermission(true)
        viewModel.onEvent(AddEditAlarmEvent.OnSaveTodoClick)
        advanceUntilIdle()
        val success = viewModel.awaitResult(failure.id)
        success.event shouldBe AlarmSettingsViewModel.UiEvent.SaveAlarm
        viewModel.state.value.hasUnsavedChanges shouldBe false
        viewModel.state.value.results.first() shouldBe failure
        val saved = usecases.getSavedAlarms().first().single()
        saved.title shouldBe "Retained waiter draft"
        listOf(alarmInteractor.getScheduledAlarms()[saved.alarmId]!!.timeInMillis) shouldBe saved.pendingTimes
    }

    @Test
    fun `accepted save survives owner closure and retains its result`() = runTest {
        viewModel.setAlarm(Alarm(alarmTone = "test_tone"))
        val locked = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        val command = launch { usecases.command { locked.complete(Unit); release.await() } }
        locked.await()
        viewModel.onEvent(AddEditAlarmEvent.OnSaveTodoClick)
        viewModel.close()
        release.complete(Unit)
        command.join()
        advanceUntilIdle()
        val saved = usecases.getSavedAlarms().first().single()
        saved.isSaved shouldBe true
        saved.pendingTimes.isNotEmpty() shouldBe true
        listOf(alarmInteractor.getScheduledAlarms()[saved.alarmId]!!.timeInMillis) shouldBe saved.pendingTimes
        viewModel.state.value.isSaving shouldBe false
        val result = viewModel.state.value.results.single()
        result.event shouldBe AlarmSettingsViewModel.UiEvent.SaveAlarm
        viewModel.acknowledgeResult(result.id)
        viewModel.state.value.results shouldBe emptyList()
        viewModel.onEvent(AddEditAlarmEvent.OnSaveTodoClick)
        advanceUntilIdle()
        usecases.getSavedAlarms().first().size shouldBe 1
    }

    @Test
    fun `invalid time fails without writing and remains retryable`() = runTest {
        viewModel.setAlarm(Alarm(alarmTone = "test_tone"))
        viewModel.onEvent(AddEditAlarmEvent.ChangeTime(TimeState(24, 0)))
        viewModel.state.value.validation shouldBe AlarmEditorValidation.INVALID_TIME
        viewModel.onEvent(AddEditAlarmEvent.OnSaveTodoClick)
        advanceUntilIdle()
        usecases.getSavedAlarms().first() shouldBe emptyList()
        alarmInteractor.getScheduledAlarms().isEmpty() shouldBe true
        viewModel.state.value.results.single().event shouldBe
            AlarmSettingsViewModel.UiEvent.ValidationFailed(AlarmEditorValidation.INVALID_TIME)
        viewModel.onEvent(AddEditAlarmEvent.ChangeTime(TimeState(7, 0)))
        viewModel.state.value.validation shouldBe AlarmEditorValidation.NONE
        viewModel.onEvent(AddEditAlarmEvent.OnSaveTodoClick)
        advanceUntilIdle()
        val saved = usecases.getSavedAlarms().first().single()
        saved.hour shouldBe 7
        listOf(alarmInteractor.getScheduledAlarms()[saved.alarmId]!!.timeInMillis) shouldBe saved.pendingTimes
        viewModel.state.value.results.last().event shouldBe AlarmSettingsViewModel.UiEvent.SaveAlarm
    }

    @Test
    fun `preview result is retained and cannot persist an occurrence`() = runTest {
        viewModel.setAlarm(Alarm(alarmTone = "test_tone"))
        viewModel.onEvent(AddEditAlarmEvent.EnteredTitle("Unsaved preview"))
        viewModel.onEvent(AddEditAlarmEvent.OnTestClick)
        val result = viewModel.state.value.results.single()
        (result.event as AlarmSettingsViewModel.UiEvent.TestAlarm).alarm.title shouldBe "Unsaved preview"
        usecases.getSavedAlarms().first() shouldBe emptyList()
        alarmInteractor.getScheduledAlarms().isEmpty() shouldBe true
        viewModel.state.value.hasUnsavedChanges shouldBe true
        viewModel.acknowledgeResult(result.id)
        viewModel.state.value.hasUnsavedChanges shouldBe true
        viewModel.state.value.alarmTitle shouldBe "Unsaved preview"
    }

    @Test
    fun `new iOS alarm defaults to Orbit and saves that tone with its occurrence`() = runTest {
        if (!isIosPlatform()) return@runTest
        viewModel.setAlarm(Alarm(isOn = true))
        viewModel.state.value.tone shouldBe AlarmSoundCatalog.DEFAULT_SOUND
        viewModel.hasUnsavedChanges shouldBe false
        viewModel.resultEvents().test {
            viewModel.onEvent(AddEditAlarmEvent.OnSaveTodoClick)
            awaitItem() shouldBe AlarmSettingsViewModel.UiEvent.SaveAlarm
        }
        val saved = usecases.findAlarm(viewModel.currentAlarmId!!)!!
        saved.alarmTone shouldBe "alarm_orbit"
        saved.pendingTimes.isNotEmpty() shouldBe true
        listOf(alarmInteractor.getScheduledAlarms()[saved.alarmId]!!.timeInMillis) shouldBe saved.pendingTimes
    }

    @Test
    fun `editing existing alarm preserves its bundled or device selection and occurrence`() = runTest {
        for ((index, tone) in listOf("alarm_glass_garden", "alarm_daybreak", "content://media/internal/audio/media/42",
            "content://settings/system/alarm_alert").withIndex()) {
            val original = Alarm(alarmId = 980L + index, isSaved = true, isOn = true, alarmTone = tone)
            usecases.addAlarm(original)
            usecases.scheduleAlarm(original, true)
            val scheduled = usecases.findAlarm(original.alarmId)!!
            viewModel = AlarmSettingsViewModel(usecases, permission)
            viewModel.setAlarm(scheduled)
            viewModel.state.value.tone shouldBe tone
            viewModel.onEvent(AddEditAlarmEvent.EnteredTitle("Edited sound fixture"))
            viewModel.resultEvents().test {
                viewModel.onEvent(AddEditAlarmEvent.OnSaveTodoClick)
                awaitItem() shouldBe AlarmSettingsViewModel.UiEvent.SaveAlarm
            }
            val saved = usecases.findAlarm(original.alarmId)!!
            saved.alarmTone shouldBe tone
            saved.pendingTimes shouldBe scheduled.pendingTimes
            listOf(alarmInteractor.getScheduledAlarms()[saved.alarmId]!!.timeInMillis) shouldBe saved.pendingTimes
        }
    }

    private val permission = AlarmPermissionFake()
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
        permission.setPermission(true)
        
        dataSource = AlarmRepositoryFake()
        repository = AlarmRepository(dataSource)
        alarmInteractor = AlarmInteractorFake()
        notificationInteractor = NotificationInteractorFake()
        dateTimeProvider = DateTimeProviderFake()
        
        val alarmTimeCalculator = AlarmTimeCalculatorFake()
        val scheduleNextAlarm = ScheduleNextAlarm(alarmInteractor, alarmTimeCalculator)
        val rescheduleFutureAlarms = RescheduleFutureAlarms(repository, alarmInteractor, alarmTimeCalculator)
        
        usecases = Usecases(
            applicationScope = kotlinx.coroutines.CoroutineScope(testDispatcher + kotlinx.coroutines.SupervisorJob()),
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
        
        viewModel = AlarmSettingsViewModel(usecases = usecases, permission = permission)
    }

    @Test
    fun `two save taps while another command runs create only one alarm`() = runTest {
        viewModel.setAlarm(Alarm(alarmTone = "test_tone"))
        val locked = CompletableDeferred<Unit>()
        val release = CompletableDeferred<Unit>()
        val command = launch {
            usecases.command { locked.complete(Unit); release.await() }
        }
        locked.await()
        viewModel.resultEvents().test {
            viewModel.onEvent(AddEditAlarmEvent.OnSaveTodoClick)
            viewModel.onEvent(AddEditAlarmEvent.OnSaveTodoClick)
            release.complete(Unit)
            command.join()
            awaitItem() shouldBe AlarmSettingsViewModel.UiEvent.SaveAlarm
            advanceUntilIdle()
            expectNoEvents()
            usecases.getSavedAlarms().first().size shouldBe 1
        }
    }

    @Test
    fun `saving without exact alarm permission requests permission before writing`() = runTest {
        permission.setPermission(false)
        viewModel.setAlarm(Alarm(alarmTone = "test_tone"))
        viewModel.resultEvents().test {
            viewModel.onEvent(AddEditAlarmEvent.OnSaveTodoClick)
            awaitItem() shouldBe AlarmSettingsViewModel.UiEvent.RequestExactAlarmPermission
            usecases.getSavedAlarms().first().size shouldBe 0
            permission.setPermission(true)
            viewModel.onEvent(AddEditAlarmEvent.OnSaveTodoClick)
            awaitItem() shouldBe AlarmSettingsViewModel.UiEvent.SaveAlarm
            usecases.getSavedAlarms().first().size shouldBe 1
        }
    }

    @Test
    fun `saving after enabling in list preserves enabled state and occurrences`() = runTest {
        for (changeTime in listOf(false, true)) {
            val original = Alarm(alarmId = if (changeTime) 996 else 995,
                isSaved = true, isOn = false, alarmTone = "test_tone")
            usecases.addAlarm(original)
            viewModel = AlarmSettingsViewModel(usecases, permission)
            viewModel.setAlarm(original)
            usecases.command { scheduleAlarm(original, true) }
            val enabled = usecases.findAlarm(original.alarmId)!!
            viewModel.onEvent(AddEditAlarmEvent.EnteredTitle("Edited"))
            if (changeTime) viewModel.onEvent(AddEditAlarmEvent.ChangeTime(TimeState(8, 30)))
            viewModel.resultEvents().test {
                viewModel.onEvent(AddEditAlarmEvent.OnSaveTodoClick)
                awaitItem() shouldBe AlarmSettingsViewModel.UiEvent.SaveAlarm
            }
            val saved = usecases.findAlarm(original.alarmId)!!
            saved.isOn shouldBe true
            saved.title shouldBe "Edited"
            saved.pendingTimes shouldBe enabled.pendingTimes
            listOf(alarmInteractor.getScheduledAlarms()[saved.alarmId]!!.timeInMillis) shouldBe saved.pendingTimes
        }
    }

    @Test
    fun `saving after disabling in list stays disabled even without permission`() = runTest {
        for (changeTime in listOf(false, true)) {
            val original = Alarm(alarmId = if (changeTime) 998 else 997,
                isSaved = true, isOn = true, alarmTone = "test_tone")
            usecases.addAlarm(original)
            usecases.scheduleAlarm(original, true)
            viewModel = AlarmSettingsViewModel(usecases, permission)
            viewModel.setAlarm(usecases.findAlarm(original.alarmId)!!)
            usecases.command {
                val latest = findAlarm(original.alarmId)!!
                cancelAlarm(latest)
                updateAlarm(latest.copy(isOn = false, pendingTimes = emptyList()))
            }
            permission.setPermission(false)
            viewModel.onEvent(AddEditAlarmEvent.EnteredTitle("Edited"))
            if (changeTime) viewModel.onEvent(AddEditAlarmEvent.ChangeTime(TimeState(8, 30)))
            viewModel.resultEvents().test {
                viewModel.onEvent(AddEditAlarmEvent.OnSaveTodoClick)
                awaitItem() shouldBe AlarmSettingsViewModel.UiEvent.SaveAlarm
            }
            val saved = usecases.findAlarm(original.alarmId)!!
            saved.isOn shouldBe false
            saved.title shouldBe "Edited"
            saved.pendingTimes shouldBe emptyList()
            alarmInteractor.isAlarmScheduled(saved) shouldBe false
        }
    }

    @Test
    fun `permission check uses latest enabled state before saving edits`() = runTest {
        val original = Alarm(alarmId = 999, isSaved = true, isOn = false, alarmTone = "test_tone")
        usecases.addAlarm(original)
        viewModel.setAlarm(original)
        usecases.command { scheduleAlarm(original, true) }
        val enabled = usecases.findAlarm(original.alarmId)!!
        val scheduled = alarmInteractor.getScheduledAlarms()
        permission.setPermission(false)
        viewModel.onEvent(AddEditAlarmEvent.EnteredTitle("Edited"))
        viewModel.resultEvents().test {
            viewModel.onEvent(AddEditAlarmEvent.OnSaveTodoClick)
            awaitItem() shouldBe AlarmSettingsViewModel.UiEvent.RequestExactAlarmPermission
            usecases.findAlarm(original.alarmId) shouldBe enabled
            alarmInteractor.getScheduledAlarms() shouldBe scheduled
        }
    }

    @Test
    fun `draft detection ignores initialization and clears when edits are reverted`() {
        viewModel.hasUnsavedChanges shouldBe false
        viewModel.setAlarm(Alarm(alarmId = 99, title = "Morning", alarmTone = "test_tone"))
        viewModel.hasUnsavedChanges shouldBe false
        viewModel.onEvent(AddEditAlarmEvent.EnteredTitle("Changed"))
        viewModel.hasUnsavedChanges shouldBe true
        // Re-entering an existing destination after a resize must not reset its draft.
        viewModel.setAlarm(Alarm(alarmId = 99, title = "Morning", alarmTone = "test_tone"))
        viewModel.state.value.alarmTitle shouldBe "Changed"
        viewModel.onEvent(AddEditAlarmEvent.EnteredTitle("Morning"))
        viewModel.hasUnsavedChanges shouldBe false
        viewModel.onEvent(AddEditAlarmEvent.ToggleVibrate(true))
        viewModel.hasUnsavedChanges shouldBe true
    }

    @AfterTest
    fun tearDown() {
        viewModel.close()
        usecases.applicationScope.cancel()
        Dispatchers.resetMain()
    }

    @Test
    fun `applied challenge is used by test alarm and survives saving`() = runTest {
        viewModel.setAlarm(Alarm(alarmId = 732, alarmTone = "test_tone"))
        val config = MathChallenge(3, 7, "+×", 1, 2)
        viewModel.onEvent(AddEditAlarmEvent.OnChallengeChange(config))
        viewModel.resultEvents().test {
            viewModel.onEvent(AddEditAlarmEvent.OnTestClick)
            (awaitItem() as AlarmSettingsViewModel.UiEvent.TestAlarm).alarm.mathChallenge shouldBe config
            viewModel.onEvent(AddEditAlarmEvent.OnSaveTodoClick)
            awaitItem() shouldBe AlarmSettingsViewModel.UiEvent.SaveAlarm
        }
        val saved = usecases.findAlarm(732)!!
        saved.mathChallenge shouldBe config
        val reopened = AlarmSettingsViewModel(usecases, permission)
        reopened.setAlarm(saved)
        reopened.state.value.challenge shouldBe config
        reopened.onEvent(AddEditAlarmEvent.OnChallengeChange(config.copy(questionCount = 99)))
        reopened.state.value.challenge.questionCount shouldBe 10
    }

    @Test
    fun `mixed challenge is used for preview and persisted when saved`() = runTest {
        viewModel.setAlarm(Alarm(alarmId = 733, alarmTone = "test_tone"))
        val config = MathChallenge(difficultyMix = "00112").normalized()
        viewModel.onEvent(AddEditAlarmEvent.OnChallengeChange(config))
        viewModel.resultEvents().test {
            viewModel.onEvent(AddEditAlarmEvent.OnTestClick)
            (awaitItem() as AlarmSettingsViewModel.UiEvent.TestAlarm).alarm.mathChallenge shouldBe config
            viewModel.onEvent(AddEditAlarmEvent.OnSaveTodoClick)
            awaitItem() shouldBe AlarmSettingsViewModel.UiEvent.SaveAlarm
        }
        val reopened = AlarmSettingsViewModel(usecases, permission)
        reopened.setAlarm(usecases.findAlarm(733)!!)
        reopened.state.value.challenge shouldBe config
    }

    @Test
    fun `failed save emits a localizable error and keeps the editor open`() = runTest {
        val backend = object : AlarmInteractor by alarmInteractor {
            override suspend fun schedule(alarm: Alarm, timeInMillis: Long) {
                error("Internal OS scheduling details")
            }
        }
        val commands = usecases.copy(scheduleAlarm = ScheduleAlarm(repository, backend, AlarmTimeCalculatorFake()))
        viewModel = AlarmSettingsViewModel(commands, permission)
        viewModel.setAlarm(Alarm(isOn = true, alarmTone = "test_tone"))
        advanceUntilIdle()
        viewModel.resultEvents().test {
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
        viewModel = AlarmSettingsViewModel(commands, permission)
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
        viewModel = AlarmSettingsViewModel(commands, permission)
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
            state.value.alarmTime shouldBe TimeState()
            state.value.alarmTitle shouldBe "Good day"
            state.value.dayChooser shouldBe "FFFFFFF"
            state.value.repeatWeekly shouldBe false
            state.value.vibrate shouldBe false
            state.value.snoozeEnabled shouldBe true
            state.value.challenge.difficulty shouldBe 0
            state.value.isOn shouldBe false
            state.value.isSaved shouldBe false
        }
    }

    @Test
    fun `onEvent ChangeTime should update alarm time`() {
        val newTime = TimeState(hour = 8, minute = 30)
        
        viewModel.onEvent(AddEditAlarmEvent.ChangeTime(newTime))
        
        viewModel.state.value.alarmTime shouldBe newTime
    }

    @Test
    fun `onEvent EnteredTitle should update alarm title`() {
        val newTitle = "Wake up!"
        
        viewModel.onEvent(AddEditAlarmEvent.EnteredTitle(newTitle))
        
        viewModel.state.value.alarmTitle shouldBe newTitle
    }

    @Test
    fun `onEvent ToggleRepeat should update repeat weekly state`() {
        viewModel.onEvent(AddEditAlarmEvent.ToggleRepeat(true))
        
        viewModel.state.value.repeatWeekly shouldBe true
        
        viewModel.onEvent(AddEditAlarmEvent.ToggleRepeat(false))
        
        viewModel.state.value.repeatWeekly shouldBe false
    }

    @Test
    fun `onEvent ToggleVibrate should update vibrate state`() {
        viewModel.onEvent(AddEditAlarmEvent.ToggleVibrate(true))
        
        viewModel.state.value.vibrate shouldBe true
    }

    @Test
    fun `onEvent ToggleSnooze should update snooze enabled state`() {
        viewModel.onEvent(AddEditAlarmEvent.ToggleSnooze(false))

        viewModel.state.value.snoozeEnabled shouldBe false

        viewModel.onEvent(AddEditAlarmEvent.ToggleSnooze(true))

        viewModel.state.value.snoozeEnabled shouldBe true
    }

    @Test
    fun `onEvent ToggleDayChooser should update day chooser state`() {
        val selectedDays = "TFTFTFT" // Monday, Wednesday, Friday, Sunday
        
        viewModel.onEvent(AddEditAlarmEvent.ToggleDayChooser(selectedDays))
        
        viewModel.state.value.dayChooser shouldBe selectedDays
    }

    @Test
    fun `onEvent OnToneChange should update tone`() {
        val toneUri = "content://media/internal/audio/media/123"
        
        viewModel.onEvent(AddEditAlarmEvent.OnToneChange(toneUri))
        
        viewModel.state.value.tone shouldBe toneUri
    }

    @Test
    fun `onEvent OnToneError should emit a localizable error`() = runTest {
        val errorMessage = "Failed to load tone"
        
        viewModel.resultEvents().test {
            viewModel.onEvent(AddEditAlarmEvent.OnToneError)
            
            val event = awaitItem()
            event.shouldBeInstanceOf<AlarmSettingsViewModel.UiEvent.ShowError>()
            event.error shouldBe AlarmErrorMessage.TONE
        }
    }

    @Test
    fun `onEvent OnTestClick should emit TestAlarm event`() = runTest {
        val testAlarm = Alarm(alarmId = 123, hour = 9, minute = 0, alarmTone = "test_tone")
        viewModel.setAlarm(testAlarm)
        
        viewModel.resultEvents().test {
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
        viewModel.onEvent(AddEditAlarmEvent.EnteredTitle("Morning alarm"))
        viewModel.onEvent(AddEditAlarmEvent.ToggleVibrate(true))
        
        viewModel.resultEvents().test {
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
            state.value.alarmTime.hour shouldBe 6
            state.value.alarmTime.minute shouldBe 45
            state.value.dayChooser.count { it == 'T' } shouldBe 1
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
            state.value.alarmTime.hour shouldBe 10
            state.value.alarmTime.minute shouldBe 15
            state.value.repeatWeekly shouldBe true
            state.value.dayChooser shouldBe "TFTFTFT"
            state.value.vibrate shouldBe true
            state.value.challenge.difficulty shouldBe 2
            state.value.tone shouldBe "content://test/tone"
            state.value.alarmTitle shouldBe "Test Alarm"
            state.value.isOn shouldBe true
            state.value.isSaved shouldBe true
        }
    }

    @Test
    fun `setAlarm should disable snooze when saved snooze is zero`() {
        val alarm = Alarm(alarmId = 777, alarmTone = "test_tone", snooze = 0)

        viewModel.setAlarm(alarm)

        viewModel.state.value.snoozeEnabled shouldBe false
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
        
        viewModel.state.value.alarmTime.hour shouldBe 10
        viewModel.state.value.alarmTime.minute shouldBe 0
    }

    @Test
    fun `saving alarm with repeat weekly should schedule with repeat`() = runTest {
        val alarm = Alarm(alarmId = 222, hour = 7, minute = 0, alarmTone = "test_tone")
        viewModel.setAlarm(alarm)
        viewModel.onEvent(AddEditAlarmEvent.ToggleRepeat(true))
        viewModel.onEvent(AddEditAlarmEvent.ToggleDayChooser("TTTTTTT")) // All days
        
        viewModel.resultEvents().test {
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

        viewModel.resultEvents().test {
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

        viewModel.resultEvents().test {
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

        viewModel.resultEvents().test {
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

        viewModel.resultEvents().test {
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

        viewModel.resultEvents().test {
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
        viewModel.onEvent(AddEditAlarmEvent.EnteredTitle("Custom Title"))
        viewModel.onEvent(AddEditAlarmEvent.ToggleVibrate(true))
        viewModel.onEvent(AddEditAlarmEvent.OnChallengeChange(MathChallenge(difficulty = 1)))
        viewModel.onEvent(AddEditAlarmEvent.ToggleRepeat(true))
        
        viewModel.state.value.alarmTime.hour shouldBe 11
        viewModel.state.value.alarmTime.minute shouldBe 30
        viewModel.state.value.alarmTitle shouldBe "Custom Title"
        viewModel.state.value.vibrate shouldBe true
        viewModel.state.value.challenge.difficulty shouldBe 1
        viewModel.state.value.repeatWeekly shouldBe true
    }

    @Test
    fun `setAlarm should only initialize once`() {
        val alarm1 = Alarm(alarmId = 444, hour = 8, minute = 0, alarmTone = "test_tone")
        val alarm2 = Alarm(alarmId = 555, hour = 9, minute = 0, alarmTone = "test_tone")
        
        viewModel.setAlarm(alarm1)
        viewModel.setAlarm(alarm2) // Should be ignored
        
        viewModel.currentAlarmId shouldBe 444
        viewModel.state.value.alarmTime.hour shouldBe 8
    }

    @Test
    fun `new alarms default to three snoozes of five minutes`() = runTest {
        viewModel.setAlarm(Alarm(alarmTone = "test_tone"))
        viewModel.resultEvents().test {
            viewModel.onEvent(AddEditAlarmEvent.OnTestClick)
            val draft = (awaitItem() as AlarmSettingsViewModel.UiEvent.TestAlarm).alarm
            draft.maxSnoozes shouldBe 3
            draft.snooze shouldBe 5
        }
    }

    @Test
    fun `snooze settings persist and editing preserves occurrence count and duration`() = runTest {
        val alarm = Alarm(alarmId = 904, alarmTone = "test_tone", snooze = 10, maxSnoozes = 0,
            snoozeCount = 1, activeAt = 1000)
        usecases.addAlarm(alarm)
        viewModel.setAlarm(alarm)
        viewModel.state.value.maxSnoozes shouldBe 0
        viewModel.onEvent(AddEditAlarmEvent.ChangeMaxSnoozes(2))
        viewModel.onEvent(AddEditAlarmEvent.ChangeSnoozeDuration(12))
        viewModel.resultEvents().test {
            viewModel.onEvent(AddEditAlarmEvent.OnSaveTodoClick)
            awaitItem() shouldBe AlarmSettingsViewModel.UiEvent.SaveAlarm
        }
        val saved = usecases.findAlarm(904)!!
        saved.maxSnoozes shouldBe 2
        saved.snoozeCount shouldBe 1
        saved.snooze shouldBe 12
        val reopened = AlarmSettingsViewModel(usecases, permission)
        reopened.setAlarm(saved)
        reopened.state.value.snoozeMinutes shouldBe 12
        reopened.state.value.maxSnoozes shouldBe 2
    }


    @Test
    fun `snooze duration validates boundaries and survives toggling snooze`() = runTest {
        viewModel.setAlarm(Alarm(alarmTone = "test_tone"))
        viewModel.onEvent(AddEditAlarmEvent.ChangeSnoozeDuration(1))
        viewModel.state.value.snoozeMinutes shouldBe 1
        viewModel.onEvent(AddEditAlarmEvent.ChangeSnoozeDuration(30))
        viewModel.state.value.snoozeMinutes shouldBe 30
        for (invalid in listOf(0, -1, 31, 60)) {
            viewModel.onEvent(AddEditAlarmEvent.ChangeSnoozeDuration(invalid))
            viewModel.state.value.snoozeMinutes shouldBe 30
        }
        viewModel.onEvent(AddEditAlarmEvent.ToggleSnooze(false))
        viewModel.resultEvents().test {
            viewModel.onEvent(AddEditAlarmEvent.OnTestClick)
            (awaitItem() as AlarmSettingsViewModel.UiEvent.TestAlarm).alarm.snooze shouldBe 0
            viewModel.onEvent(AddEditAlarmEvent.ToggleSnooze(true))
            viewModel.onEvent(AddEditAlarmEvent.OnTestClick)
            (awaitItem() as AlarmSettingsViewModel.UiEvent.TestAlarm).alarm.snooze shouldBe 30
        }
    }


    @Test
    fun `editing a duration above the new maximum clamps it to thirty minutes`() {
        viewModel.setAlarm(Alarm(alarmId = 905, alarmTone = "test_tone", snooze = 60))
        viewModel.state.value.snoozeMinutes shouldBe 30
    }

}

/** Assertions consume retained semantic outcomes; no production event-only channel exists. */
private fun AlarmSettingsViewModel.resultEvents() = kotlinx.coroutines.flow.flow {
    var lastId = state.value.results.lastOrNull()?.id ?: 0L
    state.collect { current ->
        for (result in current.results.filter { it.id > lastId }) {
            lastId = result.id
            emit(result.event)
        }
    }
}
