package com.timilehinaregbesola.mathalarm.usecases

import com.timilehinaregbesola.mathalarm.data.AlarmRepository
import com.timilehinaregbesola.mathalarm.domain.model.Alarm
import com.timilehinaregbesola.mathalarm.fake.NotificationInteractorFake
import com.timilehinaregbesola.mathalarm.fake.AlarmInteractorFake
import com.timilehinaregbesola.mathalarm.fake.AlarmRepositoryFake
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertNull

@OptIn(ExperimentalCoroutinesApi::class)
class DeleteAlarmTest {
    private val dataSource = AlarmRepositoryFake()

    private val alarmRepository = AlarmRepository(dataSource)

    private val alarmInteractor = AlarmInteractorFake()
    private val notifications = NotificationInteractorFake()

    private val deleteAlarmUseCase = DeleteAlarm(alarmRepository, alarmInteractor, notifications)

    private val addAlarmUseCase = AddAlarm(alarmRepository)

    private val findAlarmUseCase = FindAlarm(alarmRepository)

    private val baseAlarm = Alarm(alarmId = 11, isOn = true, title = "Delete me!!")

    @BeforeTest
    fun setup() = runTest {
        alarmRepository.clear()
        alarmInteractor.clear()
        addAlarmUseCase(baseAlarm)
    }

    @Test
    fun `deleted regular and snoozed occurrences cannot ring after late delivery or recovery`() = runTest {
        for (repeat in listOf(false, true)) {
            for (active in listOf(false, true)) {
                val alarm = baseAlarm.copy(repeat = repeat, scheduleInitialized = true,
                    pendingTimes = listOf(1_000L), snoozedUntil = 2_000L,
                    activeAt = if (active) 500L else null)
                alarmRepository.addAlarm(alarm)
                alarmInteractor.schedule(alarm, 1_000L)
                alarmInteractor.scheduleSnooze(alarm, 2_000L)
                if (active) notifications.show(alarm)
                deleteAlarmUseCase(alarm.alarmId)
                val calculator = com.timilehinaregbesola.mathalarm.fake.AlarmTimeCalculatorFake()
                val show = ShowAlarm(alarmRepository, notifications, ScheduleNextAlarm(alarmInteractor, calculator))
                show(alarm.alarmId, 1_000L)
                show(alarm.alarmId, 2_000L, snoozed = true)
                show(alarm.alarmId) // Pre-migration delivery also cannot revive a deleted row.
                RescheduleFutureAlarms(alarmRepository, alarmInteractor, calculator)()
                assertNull(alarmRepository.findAlarm(alarm.alarmId))
                assertFalse(alarmInteractor.isAlarmScheduled(alarm))
                assertFalse(notifications.isNotificationShown(alarm.alarmId))
            }
        }
    }

    @Test
    fun `test if alarm is deleted`() = runTest {
        deleteAlarmUseCase(baseAlarm)

        val assertAlarm = findAlarmUseCase(baseAlarm.alarmId)

        assertNull(assertAlarm)
    }

    @Test
    fun `test if deleted alarm is cancelled`() = runTest {
        deleteAlarmUseCase(baseAlarm)

        assertFalse(alarmInteractor.isAlarmScheduled(baseAlarm))
    }

    @Test
    fun `test if alarm is deleted with id`() = runTest {
        addAlarmUseCase(baseAlarm)
        deleteAlarmUseCase(baseAlarm.alarmId)

        val assertAlarm = findAlarmUseCase(baseAlarm.alarmId)

        assertNull(assertAlarm)
    }

    @Test
    fun `test if deleted alarm by id is cancelled`() = runTest {
        deleteAlarmUseCase(baseAlarm.alarmId)

        assertFalse(alarmInteractor.isAlarmScheduled(baseAlarm))
    }
}
