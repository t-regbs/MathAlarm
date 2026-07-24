package com.timilehinaregbesola.mathalarm.presentation.alarmlist

import app.cash.turbine.test
import co.touchlab.kermit.Logger
import com.russhwolf.settings.MapSettings
import com.timilehinaregbesola.mathalarm.data.AlarmRepository
import com.timilehinaregbesola.mathalarm.domain.model.Alarm
import com.timilehinaregbesola.mathalarm.fake.*
import com.timilehinaregbesola.mathalarm.framework.Usecases
import com.timilehinaregbesola.mathalarm.presentation.appsettings.AlarmPreferences
import com.timilehinaregbesola.mathalarm.presentation.appsettings.AlarmPreferencesImpl
import com.timilehinaregbesola.mathalarm.presentation.appsettings.AppThemeOptionsMapper
import com.timilehinaregbesola.mathalarm.usecases.*
import com.timilehinaregbesola.mathalarm.utils.UiEvent
import io.kotest.matchers.shouldBe
import io.kotest.matchers.types.shouldBeInstanceOf
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.*
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AlarmListViewModelTest {

    private lateinit var viewModel: AlarmListViewModel
    private lateinit var dataSource: AlarmRepositoryFake
    private lateinit var repository: AlarmRepository
    private lateinit var alarmInteractor: AlarmInteractorFake
    private lateinit var notificationInteractor: NotificationInteractorFake
    private lateinit var dateTimeProvider: DateTimeProviderFake
    private lateinit var usecases: Usecases
    private lateinit var permission: AlarmPermissionFake
    private lateinit var preferences: AlarmPreferencesImpl
    private val testDispatcher = StandardTestDispatcher()

    @BeforeTest
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        
        dataSource = AlarmRepositoryFake()
        repository = AlarmRepository(dataSource)
        alarmInteractor = AlarmInteractorFake()
        notificationInteractor = NotificationInteractorFake()
        dateTimeProvider = DateTimeProviderFake()
        permission = AlarmPermissionFake()
        preferences = AlarmPreferencesImpl(
            mapper = AppThemeOptionsMapper(),
            logger = Logger.withTag("AlarmPreferencesImplTest"),
            settings = MapSettings()
        )
        
        val alarmTimeCalculator = AlarmTimeCalculatorFake()
        val scheduleNextAlarm = ScheduleNextAlarm(alarmInteractor, alarmTimeCalculator)
        
        usecases = Usecases(
            addAlarm = AddAlarm(repository),
            findAlarm = FindAlarm(repository),
            deleteAlarm = DeleteAlarm(repository, alarmInteractor),
            getSavedAlarms = GetSavedAlarms(repository),
            scheduleAlarm = ScheduleAlarm(repository, alarmInteractor, alarmTimeCalculator),
            showAlarm = ShowAlarm(repository, notificationInteractor, scheduleNextAlarm),
            completeAlarm = CompleteAlarm(repository, alarmInteractor, notificationInteractor, dateTimeProvider),
            updateAlarm = UpdateAlarm(repository),
            cancelAlarm = CancelAlarm(alarmInteractor),
            clearAlarms = ClearAlarms(repository, alarmInteractor),
            scheduleNextAlarm = scheduleNextAlarm,
            rescheduleFutureAlarms = RescheduleFutureAlarms(repository, alarmInteractor, alarmTimeCalculator, scheduleNextAlarm),
            snoozeAlarm = SnoozeAlarm(dateTimeProvider, notificationInteractor, alarmInteractor, repository)
        )
        
        viewModel = AlarmListViewModel(
            usecases = usecases,
            permission = permission,
            preferences = preferences,
            logger = Logger.withTag("AlarmListViewModelTest")
        )
    }

    @AfterTest
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state should have empty alarm list`() = runTest {
        val alarms = viewModel.alarms.first()
        alarms shouldBe emptyList()
    }

    @Test
    fun `onEvent OnAddAlarmClick should navigate with new alarm`() = runTest {
        viewModel.uiEvent.test {
            viewModel.onEvent(AlarmListEvent.OnAddAlarmClick)
            
            val event = awaitItem()
            event.shouldBeInstanceOf<UiEvent.Navigate>()
            val alarm = event.alarm
            alarm.alarmId shouldBe 0L
        }
    }

    @Test
    fun `onEvent OnEditAlarmClick should navigate with selected alarm`() = runTest {
        val testAlarm = Alarm(alarmId = 123, hour = 8, minute = 30)
        
        viewModel.uiEvent.test {
            viewModel.onEvent(AlarmListEvent.OnEditAlarmClick(testAlarm))
            
            val event = awaitItem()
            event.shouldBeInstanceOf<UiEvent.Navigate>()
            event.alarm shouldBe testAlarm
        }
    }

    @Test
    fun `onEvent OnDeleteAlarmClick should delete alarm and show snackbar`() = runTest {
        val testAlarm = Alarm(alarmId = 456, hour = 9, minute = 0, isSaved = true)
        usecases.addAlarm(testAlarm)
        advanceUntilIdle()
        
        viewModel.uiEvent.test {
            viewModel.onEvent(AlarmListEvent.OnDeleteAlarmClick(testAlarm))
            advanceUntilIdle()
            
            val event = awaitItem()
            event.shouldBeInstanceOf<UiEvent.ShowSnackbar>()
            val snackbarEvent = event as UiEvent.ShowSnackbar
            snackbarEvent.message shouldBe "Alarm Deleted"
            snackbarEvent.action shouldBe "Undo"
            
            val alarms = viewModel.alarms.first()
            alarms.none { it.alarmId == testAlarm.alarmId } shouldBe true
        }
    }

    @Test
    fun `onEvent OnUndoDeleteClick should restore deleted alarm`() = runTest {
        val testAlarm = Alarm(alarmId = 789, hour = 10, minute = 30, isSaved = true)
        usecases.addAlarm(testAlarm)
        advanceUntilIdle()
        
        val alarmsAfterAdd = usecases.getSavedAlarms().first()
        alarmsAfterAdd.any { it.alarmId == testAlarm.alarmId } shouldBe true
        
        viewModel.onEvent(AlarmListEvent.OnDeleteAlarmClick(testAlarm))
        advanceUntilIdle()
        
        viewModel.onEvent(AlarmListEvent.OnUndoDeleteClick)
        advanceUntilIdle()
        
        val alarmsAfterUndo = usecases.getSavedAlarms().first()
        alarmsAfterUndo.any { it.alarmId == testAlarm.alarmId } shouldBe true
    }

    @Test
    fun `onEvent OnAlarmOnChange with isOn true should enable alarm`() = runTest {
        val testAlarm = Alarm(alarmId = 111, hour = 7, minute = 0, isOn = false, isSaved = true)
        usecases.addAlarm(testAlarm)
        advanceUntilIdle()
        
        viewModel.onEvent(AlarmListEvent.OnAlarmOnChange(testAlarm, true))
        advanceUntilIdle()
        
        val updatedAlarm = usecases.findAlarm(testAlarm.alarmId)
        updatedAlarm?.isOn shouldBe true
    }

    @Test
    fun `onEvent OnAlarmOnChange with isOn false should disable alarm`() = runTest {
        val testAlarm = Alarm(alarmId = 222, hour = 8, minute = 15, isOn = true, isSaved = true)
        usecases.addAlarm(testAlarm)
        advanceUntilIdle()
        
        viewModel.onEvent(AlarmListEvent.OnAlarmOnChange(testAlarm, false))
        advanceUntilIdle()
        
        val updatedAlarm = usecases.findAlarm(testAlarm.alarmId)
        updatedAlarm?.isOn shouldBe false
    }

    @Test
    fun `onEvent DeleteTestAlarm should delete alarm by ID`() = runTest {
        val testAlarm = Alarm(alarmId = 333, hour = 6, minute = 45, isSaved = true)
        usecases.addAlarm(testAlarm)
        advanceUntilIdle()
        
        viewModel.onEvent(AlarmListEvent.DeleteTestAlarm(testAlarm.alarmId))
        advanceUntilIdle()
        
        val deletedAlarm = usecases.findAlarm(testAlarm.alarmId)
        deletedAlarm shouldBe null
    }

    @Test
    fun `onEvent OnClearAlarmsClick should clear all alarms`() = runTest {
        usecases.addAlarm(Alarm(alarmId = 1, hour = 7, minute = 0, isSaved = true))
        usecases.addAlarm(Alarm(alarmId = 2, hour = 8, minute = 0, isSaved = true))
        usecases.addAlarm(Alarm(alarmId = 3, hour = 9, minute = 0, isSaved = true))
        advanceUntilIdle()
        
        viewModel.onEvent(AlarmListEvent.OnClearAlarmsClick)
        advanceUntilIdle()
        
        val alarms = viewModel.alarms.first()
        alarms shouldBe emptyList()
    }

    @Test
    fun `onEvent OnClearEmptyAlarmsClick should show appropriate message`() = runTest {
        viewModel.uiEvent.test {
            viewModel.onEvent(AlarmListEvent.OnClearEmptyAlarmsClick)
            
            val event = awaitItem()
            event.shouldBeInstanceOf<UiEvent.ShowSnackbar>()
            event.message shouldBe "There are no alarms to clear"
        }
    }

    @Test
    fun `onUpdate should update alarm in repository`() = runTest {
        val originalAlarm = Alarm(alarmId = 555, hour = 10, minute = 0, title = "Original", isSaved = true)
        usecases.addAlarm(originalAlarm)
        advanceUntilIdle()
        
        val updatedAlarm = originalAlarm.copy(title = "Updated")
        viewModel.onUpdate(updatedAlarm)
        advanceUntilIdle()
        
        val alarm = usecases.findAlarm(originalAlarm.alarmId)
        alarm?.title shouldBe "Updated"
    }

    @Test
    fun `scheduleAlarm should schedule alarm and show snackbar`() = runTest {
        val testAlarm = Alarm(alarmId = 666, hour = 9, minute = 30)
        val message = "Alarm scheduled"
        
        viewModel.uiEvent.test {
            viewModel.scheduleAlarm(testAlarm, reschedule = false, message = message)
            advanceUntilIdle()
            
            val event = awaitItem()
            event.shouldBeInstanceOf<UiEvent.ShowSnackbar>()
            event.message shouldBe message
        }
    }

    @Test
    fun `cancelAlarm should cancel scheduled alarm`() = runTest {
        val testAlarm = Alarm(alarmId = 777, hour = 11, minute = 15, isOn = true)
        
        viewModel.cancelAlarm(testAlarm)
        advanceUntilIdle()
        
        // Then - Should complete without errors
        // In a real scenario, this would interact with AlarmManager
    }

    @Test
    fun `permission hasExactAlarmPermission should return correct value`() {
        permission.setPermission(true)
        
        viewModel.permission.hasExactAlarmPermission() shouldBe true
        
        permission.setPermission(false)
        
        viewModel.permission.hasExactAlarmPermission() shouldBe false
    }

    @Test
    fun `undo delete without previous delete should do nothing`() = runTest {
        viewModel.onEvent(AlarmListEvent.OnUndoDeleteClick)
        advanceUntilIdle()
        
        val alarms = viewModel.alarms.first()
        alarms shouldBe emptyList()
    }

    @Test
    fun `multiple alarms should be managed correctly`() = runTest {
        val alarm1 = Alarm(alarmId = 1, hour = 6, minute = 0, isSaved = true)
        val alarm2 = Alarm(alarmId = 2, hour = 7, minute = 30, isSaved = true)
        val alarm3 = Alarm(alarmId = 3, hour = 9, minute = 15, isSaved = true)
        
        usecases.addAlarm(alarm1)
        usecases.addAlarm(alarm2)
        usecases.addAlarm(alarm3)
        advanceUntilIdle()
        
        val alarms = usecases.getSavedAlarms().first()
        alarms.size shouldBe 3
        
        viewModel.onEvent(AlarmListEvent.OnDeleteAlarmClick(alarm2))
        advanceUntilIdle()
        
        val remainingAlarms = usecases.getSavedAlarms().first()
        remainingAlarms.size shouldBe 2
        remainingAlarms.any { it.alarmId == alarm2.alarmId } shouldBe false
    }

    @Test
    fun `alarms keep creation order by default`() = runTest {
        usecases.apply {
            addAlarm(Alarm(alarmId = 1, hour = 7, minute = 50, isSaved = true))
            addAlarm(Alarm(alarmId = 2, hour = 8, minute = 0, isSaved = true))
            addAlarm(Alarm(alarmId = 3, hour = 7, minute = 55, isSaved = true))
        }
        advanceUntilIdle()

        val alarms = viewModel.alarms.first()

        alarms.map { it.alarmId } shouldBe listOf(3L, 2L, 1L)
    }

    @Test
    fun `alarms sort by time when time sort preference is enabled`() = runTest {
        usecases.apply {
            addAlarm(Alarm(alarmId = 1, hour = 8, minute = 0, isSaved = true))
            addAlarm(Alarm(alarmId = 2, hour = 7, minute = 50, isSaved = true))
            addAlarm(Alarm(alarmId = 3, hour = 7, minute = 55, isSaved = true))
            addAlarm(Alarm(alarmId = 4, hour = 7, minute = 55, isSaved = true))
        }
        advanceUntilIdle()

        preferences.updateAlarmSortOrder(AlarmPreferences.AlarmSortOrder.TIME)
        advanceUntilIdle()

        val alarms = viewModel.alarms.first()

        alarms.map { it.alarmId } shouldBe listOf(2L, 4L, 3L, 1L)
    }
}
