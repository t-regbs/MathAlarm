package com.timilehinaregbesola.mathalarm.usecases

import com.timilehinaregbesola.mathalarm.data.AlarmRepository
import com.timilehinaregbesola.mathalarm.domain.model.Alarm
import com.timilehinaregbesola.mathalarm.fake.AlarmInteractorFake
import com.timilehinaregbesola.mathalarm.fake.AlarmRepositoryFake
import com.timilehinaregbesola.mathalarm.fake.AlarmTimeCalculatorFake
import com.timilehinaregbesola.mathalarm.fake.NotificationInteractorFake
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.BeforeTest
import kotlin.test.assertFalse
import kotlin.test.assertNotNull

@ExperimentalCoroutinesApi
class CompleteAlarmTest {
    private val dataSource = AlarmRepositoryFake()

    private val alarmRepository = AlarmRepository(dataSource)

    private val alarmInteractor = AlarmInteractorFake()

    private val notificationInteractor = NotificationInteractorFake()
    
    private val alarmTimeCalculator = AlarmTimeCalculatorFake()
    
    private val scheduleNextAlarm = ScheduleNextAlarm(alarmInteractor, alarmTimeCalculator)

    private val completeAlarmUseCase = CompleteAlarm(alarmRepository, alarmInteractor, notificationInteractor, scheduleNextAlarm)

    private val addAlarmUseCase = AddAlarm(alarmRepository)

    private val findAlarmUseCase = FindAlarm(alarmRepository)

    private val baseAlarm = Alarm(alarmId = 6, title = "turn me off", isOn = true)

    @BeforeTest
    fun setup() = runTest {
        alarmRepository.clear()
        alarmInteractor.clear()
        addAlarmUseCase(baseAlarm)
    }

    @Test
    fun `test if an alarm is turned off after completion`() = runTest {
        completeAlarmUseCase(baseAlarm)

        val result = findAlarmUseCase(baseAlarm.alarmId)

        assertNotNull(result)
        assertEquals(result.isOn, false)
    }

    @Test
    fun `test if an alarm is turned off after completion by id`() = runTest {
        completeAlarmUseCase(baseAlarm.alarmId)

        val result = findAlarmUseCase(baseAlarm.alarmId)

        assertNotNull(result)
        assertEquals(result.isOn, false)
    }

    @Test
    fun `test if an alarm is cancelled after completion`() = runTest {
        completeAlarmUseCase(baseAlarm)

        assertFalse(alarmInteractor.isAlarmScheduled(baseAlarm))
    }

    @Test
    fun `test if an alarm is cancelled after completion by id`() = runTest {
        completeAlarmUseCase(baseAlarm.alarmId)

        assertFalse(alarmInteractor.isAlarmScheduled(baseAlarm))
    }

    @Test
    fun `test if notification is dismissed after alarm is completed`() = runTest {
        completeAlarmUseCase(baseAlarm)

        assertFalse(notificationInteractor.isNotificationShown(baseAlarm.alarmId))
    }

    @Test
    fun `test if notification is dismissed after alarm is completed by id`() = runTest {
        completeAlarmUseCase(baseAlarm.alarmId)

        assertFalse(notificationInteractor.isNotificationShown(baseAlarm.alarmId))
    }

    @Test
    fun `single day alarm without repeat flag should turn off after completion`() = runTest {
        // Alarm set for only Tuesday, repeat = false
        val alarm = Alarm(
            alarmId = 10,
            hour = 7,
            minute = 0,
            repeat = false,
            repeatDays = "FFTFFFF", // Only Tuesday enabled
            isOn = true,
            isSaved = true
        )
        addAlarmUseCase(alarm)

        completeAlarmUseCase(alarm)

        val result = findAlarmUseCase(alarm.alarmId)
        assertNotNull(result)
        assertEquals(false, result.isOn, "Single day alarm should turn off after completion")
        assertFalse(alarmInteractor.isAlarmScheduled(alarm), "Alarm should be cancelled")
        assertFalse(notificationInteractor.isNotificationShown(alarm.alarmId), "Notification should be dismissed")
    }

    @Test
    fun `single day alarm with repeat flag should stay on after completion`() = runTest {
        // Alarm set for only Tuesday, repeat = true (weekly repeat on same day)
        val alarm = Alarm(
            alarmId = 11,
            hour = 7,
            minute = 0,
            repeat = true,
            repeatDays = "FFTFFFF", // Only Tuesday enabled
            isOn = true,
            isSaved = true
        )
        addAlarmUseCase(alarm)

        completeAlarmUseCase(alarm)

        val result = findAlarmUseCase(alarm.alarmId)
        assertNotNull(result)
        assertEquals(true, result.isOn, "Single day alarm with repeat flag should stay on (repeats weekly)")
        // Alarm should NOT be cancelled for repeating alarms
        assertFalse(notificationInteractor.isNotificationShown(alarm.alarmId), "Notification should be dismissed")
    }

    @Test
    fun `multiple day alarm without repeat flag should stay on after completion`() = runTest {
        // Alarm set for Tuesday and Friday, repeat = false
        val alarm = Alarm(
            alarmId = 12,
            hour = 7,
            minute = 0,
            repeat = false,
            repeatDays = "FFTFTFF", // Tuesday and Friday enabled
            isOn = true,
            isSaved = true
        )
        addAlarmUseCase(alarm)

        completeAlarmUseCase(alarm)

        val result = findAlarmUseCase(alarm.alarmId)
        assertNotNull(result)
        assertEquals(true, result.isOn, "Multiple day alarm should stay on after completion")
        // Alarm should NOT be cancelled for repeating alarms
        assertFalse(notificationInteractor.isNotificationShown(alarm.alarmId), "Notification should be dismissed")
    }

    @Test
    fun `multiple day alarm with repeat flag should stay on after completion`() = runTest {
        // Alarm set for Tuesday and Friday, repeat = true
        val alarm = Alarm(
            alarmId = 13,
            hour = 7,
            minute = 0,
            repeat = true,
            repeatDays = "FFTFTFF", // Tuesday and Friday enabled
            isOn = true,
            isSaved = true
        )
        addAlarmUseCase(alarm)

        completeAlarmUseCase(alarm)

        val result = findAlarmUseCase(alarm.alarmId)
        assertNotNull(result)
        assertEquals(true, result.isOn, "Multiple day alarm with repeat should stay on after completion")
        // Alarm should NOT be cancelled for repeating alarms
        assertFalse(notificationInteractor.isNotificationShown(alarm.alarmId), "Notification should be dismissed")
    }

    @Test
    fun `alarm for all weekdays should stay on after completion`() = runTest {
        // Alarm set for Monday through Friday
        val alarm = Alarm(
            alarmId = 14,
            hour = 6,
            minute = 30,
            repeat = true,
            repeatDays = "FTTTTTF", // Mon-Fri enabled
            isOn = true,
            isSaved = true
        )
        addAlarmUseCase(alarm)

        completeAlarmUseCase(alarm)

        val result = findAlarmUseCase(alarm.alarmId)
        assertNotNull(result)
        assertEquals(true, result.isOn, "Weekday alarm should stay on after completion")
        assertFalse(notificationInteractor.isNotificationShown(alarm.alarmId), "Notification should be dismissed")
    }

    @Test
    fun `alarm for all seven days should stay on after completion`() = runTest {
        // Alarm set for every day of the week
        val alarm = Alarm(
            alarmId = 15,
            hour = 8,
            minute = 0,
            repeat = true,
            repeatDays = "TTTTTTT", // All days enabled
            isOn = true,
            isSaved = true
        )
        addAlarmUseCase(alarm)

        completeAlarmUseCase(alarm)

        val result = findAlarmUseCase(alarm.alarmId)
        assertNotNull(result)
        assertEquals(true, result.isOn, "Daily alarm should stay on after completion")
        assertFalse(notificationInteractor.isNotificationShown(alarm.alarmId), "Notification should be dismissed")
    }

    @Test
    fun `alarm with no days enabled should turn off after completion`() = runTest {
        // Edge case: alarm with no days enabled (shouldn't normally happen, but test the behavior)
        val alarm = Alarm(
            alarmId = 16,
            hour = 9,
            minute = 0,
            repeat = false,
            repeatDays = "FFFFFFF", // No days enabled
            isOn = true,
            isSaved = true
        )
        addAlarmUseCase(alarm)

        completeAlarmUseCase(alarm)

        val result = findAlarmUseCase(alarm.alarmId)
        assertNotNull(result)
        assertEquals(false, result.isOn, "Alarm with no days should turn off after completion")
        assertFalse(alarmInteractor.isAlarmScheduled(alarm), "Alarm should be cancelled")
        assertFalse(notificationInteractor.isNotificationShown(alarm.alarmId), "Notification should be dismissed")
    }

    @Test
    fun `completing alarm by id should work for multi-day alarms`() = runTest {
        // Test completion by alarm ID for multi-day alarm
        val alarm = Alarm(
            alarmId = 17,
            hour = 7,
            minute = 30,
            repeat = true,
            repeatDays = "TFFFFFT", // Sunday and Saturday
            isOn = true,
            isSaved = true
        )
        addAlarmUseCase(alarm)

        completeAlarmUseCase(alarm.alarmId)

        val result = findAlarmUseCase(alarm.alarmId)
        assertNotNull(result)
        assertEquals(true, result.isOn, "Multi-day alarm completed by ID should stay on")
        assertFalse(notificationInteractor.isNotificationShown(alarm.alarmId), "Notification should be dismissed")
    }
}
