package com.timilehinaregbesola.mathalarm.usecases

import com.timilehinaregbesola.mathalarm.data.AlarmRepository
import com.timilehinaregbesola.mathalarm.domain.model.Alarm
import com.timilehinaregbesola.mathalarm.fake.AlarmInteractorFake
import com.timilehinaregbesola.mathalarm.fake.AlarmRepositoryFake
import com.timilehinaregbesola.mathalarm.fake.DateTimeProviderFake
import com.timilehinaregbesola.mathalarm.provider.AlarmTimeCalculatorImpl
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.DayOfWeek
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.time.Instant

/**
 * Integration-style tests that verify alarms are correctly rescheduled after
 * boot/reboot scenarios. Uses real AlarmTimeCalculatorImpl with a fake time provider
 * to simulate realistic scenarios.
 */
@ExperimentalCoroutinesApi
class BootPersistenceTest {
    private lateinit var dataSource: AlarmRepositoryFake
    private lateinit var alarmRepository: AlarmRepository
    private lateinit var alarmInteractor: AlarmInteractorFake
    private lateinit var dateTimeProvider: DateTimeProviderFake
    private lateinit var alarmTimeCalculator: AlarmTimeCalculatorImpl
    private lateinit var scheduleNextAlarm: ScheduleNextAlarm
    private lateinit var rescheduleFutureAlarms: RescheduleFutureAlarms
    private lateinit var addAlarm: AddAlarm

    @BeforeTest
    fun setup() = runTest {
        dataSource = AlarmRepositoryFake()
        alarmRepository = AlarmRepository(dataSource)
        alarmInteractor = AlarmInteractorFake()
        dateTimeProvider = DateTimeProviderFake()
        alarmTimeCalculator = AlarmTimeCalculatorImpl(dateTimeProvider)
        scheduleNextAlarm = ScheduleNextAlarm(alarmInteractor, alarmTimeCalculator)
        rescheduleFutureAlarms = RescheduleFutureAlarms(
            alarmRepository,
            alarmInteractor,
            alarmTimeCalculator,
            scheduleNextAlarm
        )
        addAlarm = AddAlarm(alarmRepository)
        
        alarmRepository.clear()
        alarmInteractor.clear()
    }

    @Test
    fun `after reboot - future one-time alarms are rescheduled`() = runTest {
        // Scenario: User has a one-time alarm set for 7:00 AM tomorrow
        // Device reboots at 10:00 PM today
        // After reboot, the alarm should be rescheduled
        
        // Set current time to Wednesday 10:00 PM
        dateTimeProvider.setFixedDateTime(2025, 1, 8, 22, 0)
        
        // Alarm for 7:00 AM (tomorrow since time is 10 PM)
        val alarm = Alarm(
            alarmId = 1,
            hour = 7,
            minute = 0,
            repeatDays = "FFFFFFF", // Non-repeating
            isOn = true,
            isSaved = true,
            title = "Wake up"
        )
        addAlarm(alarm)
        
        // Simulate boot - reschedule alarms
        rescheduleFutureAlarms()
        
        // Verify alarm is scheduled
        alarmInteractor.isAlarmScheduled(alarm) shouldBe true
        
        // Verify it's scheduled for tomorrow (Thursday)
        val scheduledTime = alarmInteractor.getAlarmTimeMillis(alarm.alarmId)
        val scheduledDateTime = instantToLocalDateTime(scheduledTime!!)
        scheduledDateTime.day shouldBe 9 // Thursday
        scheduledDateTime.hour shouldBe 7
        scheduledDateTime.minute shouldBe 0
    }

    @Test
    fun `after reboot - repeating weekday alarms are rescheduled`() = runTest {
        // Set current time to Sunday 8:00 AM
        dateTimeProvider.setFixedDateTime(2025, 1, 5, 8, 0)
        
        // Weekday alarm at 6:30 AM (Mon-Fri)
        val alarm = Alarm(
            alarmId = 2,
            hour = 6,
            minute = 30,
            repeat = true,
            repeatDays = "FTTTTTF", // Mon-Fri (indices 1-5)
            isOn = true,
            isSaved = true,
            title = "Workday alarm"
        )
        addAlarm(alarm)
        
        // Simulate boot
        rescheduleFutureAlarms()
        
        // Alarm should be scheduled (the actual scheduling creates entries for all enabled days,
        // but the fake only stores the last scheduled time. The important thing is that
        // the alarm IS scheduled)
        alarmInteractor.isAlarmScheduled(alarm) shouldBe true
        
        // Verify scheduled time is at the correct hour/minute
        val scheduledTime = alarmInteractor.getAlarmTimeMillis(alarm.alarmId)
        val scheduledDateTime = instantToLocalDateTime(scheduledTime!!)
        scheduledDateTime.hour shouldBe 6
        scheduledDateTime.minute shouldBe 30
    }

    @Test
    fun `after reboot - disabled alarms are NOT rescheduled`() = runTest {
        dateTimeProvider.setFixedDateTime(2025, 1, 8, 8, 0)
        
        val enabledAlarm = Alarm(
            alarmId = 1,
            hour = 7,
            minute = 0,
            isOn = true,
            isSaved = true
        )
        val disabledAlarm = Alarm(
            alarmId = 2,
            hour = 8,
            minute = 0,
            isOn = false, // Disabled
            isSaved = true
        )
        addAlarm(enabledAlarm)
        addAlarm(disabledAlarm)
        
        rescheduleFutureAlarms()
        
        alarmInteractor.isAlarmScheduled(enabledAlarm) shouldBe true
        alarmInteractor.isAlarmScheduled(disabledAlarm) shouldBe false
    }

    @Test
    fun `after reboot - daily repeating alarm is rescheduled`() = runTest {
        // Scenario: User had a daily 7:00 AM alarm
        // Device reboots at 9:00 AM (after the alarm time passed)
        // The alarm should be rescheduled for future days
        
        dateTimeProvider.setFixedDateTime(2025, 1, 8, 9, 0) // Wednesday 9 AM
        
        val repeatingAlarm = Alarm(
            alarmId = 3,
            hour = 7,
            minute = 0,
            repeat = true,
            repeatDays = "TTTTTTT", // Daily
            isOn = true,
            isSaved = true
        )
        addAlarm(repeatingAlarm)
        
        rescheduleFutureAlarms()
        
        // Should be scheduled
        alarmInteractor.isAlarmScheduled(repeatingAlarm) shouldBe true
        
        // Verify time is correct (7:00 AM)
        val scheduledTime = alarmInteractor.getAlarmTimeMillis(repeatingAlarm.alarmId)
        val scheduledDateTime = instantToLocalDateTime(scheduledTime!!)
        scheduledDateTime.hour shouldBe 7
        scheduledDateTime.minute shouldBe 0
    }

    @Test
    fun `after reboot with multiple alarms - all active alarms rescheduled correctly`() = runTest {
        dateTimeProvider.setFixedDateTime(2025, 1, 8, 12, 0) // Wednesday noon
        
        val morningAlarm = Alarm(alarmId = 1, hour = 6, minute = 0, isOn = true, isSaved = true)
        val eveningAlarm = Alarm(alarmId = 2, hour = 18, minute = 0, isOn = true, isSaved = true)
        val disabledAlarm = Alarm(alarmId = 3, hour = 8, minute = 0, isOn = false, isSaved = true)
        val weekendAlarm = Alarm(
            alarmId = 4, 
            hour = 10, 
            minute = 0, 
            repeat = true, 
            repeatDays = "TFFFFFT", // Sat & Sun
            isOn = true, 
            isSaved = true
        )
        
        addAlarm(morningAlarm)
        addAlarm(eveningAlarm)
        addAlarm(disabledAlarm)
        addAlarm(weekendAlarm)
        
        rescheduleFutureAlarms()
        
        // Morning alarm: 6 AM already passed today, should be tomorrow
        alarmInteractor.isAlarmScheduled(morningAlarm) shouldBe true
        val morningTime = alarmInteractor.getAlarmTimeMillis(morningAlarm.alarmId)
        instantToLocalDateTime(morningTime!!).dayOfMonth shouldBe 9
        
        // Evening alarm: 6 PM is in the future, should be today
        alarmInteractor.isAlarmScheduled(eveningAlarm) shouldBe true
        val eveningTime = alarmInteractor.getAlarmTimeMillis(eveningAlarm.alarmId)
        instantToLocalDateTime(eveningTime!!).dayOfMonth shouldBe 8
        instantToLocalDateTime(eveningTime).hour shouldBe 18
        
        // Disabled alarm should not be scheduled
        alarmInteractor.isAlarmScheduled(disabledAlarm) shouldBe false
        
        // Weekend alarm should be scheduled for Saturday (Jan 11)
        alarmInteractor.isAlarmScheduled(weekendAlarm) shouldBe true
        val weekendTime = alarmInteractor.getAlarmTimeMillis(weekendAlarm.alarmId)
        instantToLocalDateTime(weekendTime!!).dayOfWeek shouldBe DayOfWeek.SATURDAY
    }

    @Test
    fun `package replaced event - alarms are rescheduled same as boot`() = runTest {
        // This tests the same flow as ACTION_MY_PACKAGE_REPLACED
        dateTimeProvider.setFixedDateTime(2025, 1, 8, 10, 0)
        
        val alarm = Alarm(
            alarmId = 1,
            hour = 15,
            minute = 30,
            isOn = true,
            isSaved = true
        )
        addAlarm(alarm)
        
        // Simulate package replacement (app updated)
        rescheduleFutureAlarms()
        
        alarmInteractor.isAlarmScheduled(alarm) shouldBe true
        val scheduledTime = alarmInteractor.getAlarmTimeMillis(alarm.alarmId)
        instantToLocalDateTime(scheduledTime!!).hour shouldBe 15
        instantToLocalDateTime(scheduledTime).minute shouldBe 30
    }


    @Test
    fun `reboot at midnight - correctly handles day transition`() = runTest {
        // Reboot exactly at midnight
        dateTimeProvider.setFixedDateTime(2025, 1, 9, 0, 0)
        
        // Alarm at 00:30 (30 minutes after midnight)
        val alarm = Alarm(
            alarmId = 1,
            hour = 0,
            minute = 30,
            isOn = true,
            isSaved = true
        )
        addAlarm(alarm)
        
        rescheduleFutureAlarms()
        
        alarmInteractor.isAlarmScheduled(alarm) shouldBe true
        val scheduledTime = alarmInteractor.getAlarmTimeMillis(alarm.alarmId)
        val scheduledDateTime = instantToLocalDateTime(scheduledTime!!)
        scheduledDateTime.day shouldBe 9 // Same day (today)
        scheduledDateTime.hour shouldBe 0
        scheduledDateTime.minute shouldBe 30
    }

    @Test
    fun `no saved alarms - reschedule completes without error`() = runTest {
        dateTimeProvider.setFixedDateTime(2025, 1, 8, 10, 0)
        
        // No alarms added
        
        rescheduleFutureAlarms()
        
        // Should complete without error, no alarms scheduled
        alarmInteractor.getScheduledAlarms().size shouldBe 0
    }


    private fun instantToLocalDateTime(epochMillis: Long): LocalDateTime {
        return Instant.fromEpochMilliseconds(epochMillis)
            .toLocalDateTime(TimeZone.currentSystemDefault())
    }
}
