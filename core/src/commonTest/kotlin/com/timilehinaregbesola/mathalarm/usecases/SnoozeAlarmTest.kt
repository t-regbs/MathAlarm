package com.timilehinaregbesola.mathalarm.usecases

import com.timilehinaregbesola.mathalarm.data.AlarmRepository
import com.timilehinaregbesola.mathalarm.domain.model.Alarm
import com.timilehinaregbesola.mathalarm.fake.AlarmInteractorFake
import com.timilehinaregbesola.mathalarm.fake.AlarmRepositoryFake
import com.timilehinaregbesola.mathalarm.fake.DateTimeProviderFake
import com.timilehinaregbesola.mathalarm.fake.NotificationInteractorFake
import io.kotest.matchers.shouldBe
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFailsWith
import kotlin.test.assertFalse
import kotlin.time.Instant

@ExperimentalCoroutinesApi
class SnoozeAlarmTest {
    private lateinit var dataSource: AlarmRepositoryFake
    private lateinit var alarmRepository: AlarmRepository
    private lateinit var alarmInteractor: AlarmInteractorFake
    private lateinit var notificationInteractor: NotificationInteractorFake
    private lateinit var dateTimeProvider: DateTimeProviderFake
    private lateinit var addAlarmUseCase: AddAlarm
    private lateinit var snoozeAlarmUseCase: SnoozeAlarm

    private val baseAlarm = Alarm(alarmId = 216L, title = "snooze me", isOn = true)

    @BeforeTest
    fun setup() = runTest {
        dataSource = AlarmRepositoryFake()
        alarmRepository = AlarmRepository(dataSource)
        alarmInteractor = AlarmInteractorFake()
        notificationInteractor = NotificationInteractorFake()
        dateTimeProvider = DateTimeProviderFake()
        addAlarmUseCase = AddAlarm(alarmRepository)
        snoozeAlarmUseCase = SnoozeAlarm(
            dateTimeProvider, notificationInteractor, alarmInteractor, alarmRepository
        )
        
        alarmRepository.clear()
        alarmInteractor.clear()
        notificationInteractor.clear()
    }

    @Test
    fun `test if alarm is snoozed for correct time`() = runTest {
        // Set fixed time: 7:00 AM
        dateTimeProvider.setFixedDateTime(2025, 1, 8, 7, 0)
        addAlarmUseCase(baseAlarm)
        val snoozeMinutes = 5

        snoozeAlarmUseCase(baseAlarm.alarmId, snoozeMinutes)

        // Should be scheduled for 7:05 AM
        val scheduledTime = alarmInteractor.getAlarmTimeMillis(baseAlarm.alarmId)
        val scheduledDateTime = instantToLocalDateTime(scheduledTime!!)
        
        scheduledDateTime.hour shouldBe 7
        scheduledDateTime.minute shouldBe 5
    }

    @Test
    fun `test if snooze correctly handles hour rollover`() = runTest {
        // Set fixed time: 7:58 AM
        dateTimeProvider.setFixedDateTime(2025, 1, 8, 7, 58)
        addAlarmUseCase(baseAlarm)
        val snoozeMinutes = 5

        snoozeAlarmUseCase(baseAlarm.alarmId, snoozeMinutes)

        // Should be scheduled for 8:03 AM
        val scheduledTime = alarmInteractor.getAlarmTimeMillis(baseAlarm.alarmId)
        val scheduledDateTime = instantToLocalDateTime(scheduledTime!!)
        
        scheduledDateTime.hour shouldBe 8
        scheduledDateTime.minute shouldBe 3
    }

    @Test
    fun `test if snooze correctly handles day rollover`() = runTest {
        // Set fixed time: 11:58 PM
        dateTimeProvider.setFixedDateTime(2025, 1, 8, 23, 58)
        addAlarmUseCase(baseAlarm)
        val snoozeMinutes = 5

        snoozeAlarmUseCase(baseAlarm.alarmId, snoozeMinutes)

        // Should be scheduled for 12:03 AM next day
        val scheduledTime = alarmInteractor.getAlarmTimeMillis(baseAlarm.alarmId)
        val scheduledDateTime = instantToLocalDateTime(scheduledTime!!)

        scheduledDateTime.day shouldBe 9
        scheduledDateTime.hour shouldBe 0
        scheduledDateTime.minute shouldBe 3
    }

    @Test
    fun `test if snooze with custom duration works`() = runTest {
        dateTimeProvider.setFixedDateTime(2025, 1, 8, 9, 0)
        addAlarmUseCase(baseAlarm)
        val snoozeMinutes = 15

        snoozeAlarmUseCase(baseAlarm.alarmId, snoozeMinutes)

        val scheduledTime = alarmInteractor.getAlarmTimeMillis(baseAlarm.alarmId)
        val scheduledDateTime = instantToLocalDateTime(scheduledTime!!)
        
        scheduledDateTime.hour shouldBe 9
        scheduledDateTime.minute shouldBe 15
    }

    @Test
    fun `test if error is shown when snoozing with negative number`() = runTest {
        addAlarmUseCase(baseAlarm)

        assertFailsWith<IllegalArgumentException> {
            snoozeAlarmUseCase(baseAlarm.alarmId, -5)
        }
    }

    @Test
    fun `test if error is shown when snoozing with zero minutes`() = runTest {
        addAlarmUseCase(baseAlarm)

        assertFailsWith<IllegalArgumentException> {
            snoozeAlarmUseCase(baseAlarm.alarmId, 0)
        }
    }

    @Test
    fun `test default snooze does not schedule alarm when snooze is disabled`() = runTest {
        dateTimeProvider.setFixedDateTime(2025, 1, 8, 7, 0)
        val alarm = baseAlarm.copy(snooze = 0)
        addAlarmUseCase(alarm)

        snoozeAlarmUseCase(alarm.alarmId)

        alarmInteractor.getAlarmTimeMillis(alarm.alarmId) shouldBe null
    }

    @Test
    fun `test if notification is dismissed after snooze`() = runTest {
        dateTimeProvider.setFixedDateTime(2025, 1, 8, 7, 0)
        addAlarmUseCase(baseAlarm)
        notificationInteractor.show(baseAlarm)

        snoozeAlarmUseCase(baseAlarm.alarmId, 5)

        assertFalse(notificationInteractor.isNotificationShown(baseAlarm.alarmId))
    }

    @Test
    fun `test snooze with non-existent alarm does nothing`() = runTest {
        dateTimeProvider.setFixedDateTime(2025, 1, 8, 7, 0)
        // Don't add alarm to repository

        snoozeAlarmUseCase(999L, 5)

        // Should not throw, but alarm should not be scheduled
        alarmInteractor.getAlarmTimeMillis(999L) shouldBe null
    }

    private fun instantToLocalDateTime(epochMillis: Long): LocalDateTime {
        return Instant.fromEpochMilliseconds(epochMillis)
            .toLocalDateTime(TimeZone.currentSystemDefault())
    }

    @Test
    fun `limit is durable and rejects the fourth snooze without silencing the alarm`() = runTest {
        addAlarmUseCase(baseAlarm.copy(activeAt = 1000))
        repeat(3) { index ->
            snoozeAlarmUseCase(baseAlarm.alarmId) shouldBe true
            val saved = alarmRepository.findAlarm(baseAlarm.alarmId)!!
            saved.snoozeCount shouldBe index + 1
            // Next delivery retains the count, including across a recreated use case.
            alarmRepository.updateAlarm(saved.copy(activeAt = 2000L + index, snoozedUntil = null))
        }
        notificationInteractor.show(alarmRepository.findAlarm(baseAlarm.alarmId)!!)
        val recreated = SnoozeAlarm(dateTimeProvider, notificationInteractor, alarmInteractor, alarmRepository)
        recreated(baseAlarm.alarmId) shouldBe false
        alarmRepository.findAlarm(baseAlarm.alarmId)!!.snoozeCount shouldBe 3
        notificationInteractor.isNotificationShown(baseAlarm.alarmId) shouldBe true
    }

    @Test
    fun `unlimited snoozes remain available beyond three`() = runTest {
        addAlarmUseCase(baseAlarm.copy(maxSnoozes = 0, snoozeCount = 20))
        snoozeAlarmUseCase(baseAlarm.alarmId) shouldBe true
        alarmRepository.findAlarm(baseAlarm.alarmId)!!.snoozeCount shouldBe 21
    }

    @Test
    fun `duration override cannot bypass disabled snoozes or a stale occurrence`() = runTest {
        addAlarmUseCase(baseAlarm.copy(snooze = 0))
        snoozeAlarmUseCase(baseAlarm.alarmId, 5) shouldBe false
        alarmRepository.updateAlarm(baseAlarm.copy(activeAt = 1000))
        snoozeAlarmUseCase(baseAlarm.alarmId, expectedActiveAt = 999) shouldBe false
        alarmRepository.findAlarm(baseAlarm.alarmId)!!.snoozeCount shouldBe 0
        snoozeAlarmUseCase(baseAlarm.alarmId, expectedActiveAt = 1000) shouldBe true
        alarmRepository.findAlarm(baseAlarm.alarmId)!!.snoozeCount shouldBe 1
    }

    @Test
    fun `duplicate snooze requests consume only one allowance`() = runTest {
        addAlarmUseCase(baseAlarm.copy(activeAt = 1000))
        snoozeAlarmUseCase(baseAlarm.alarmId) shouldBe true
        snoozeAlarmUseCase(baseAlarm.alarmId) shouldBe false
        alarmRepository.findAlarm(baseAlarm.alarmId)!!.snoozeCount shouldBe 1
    }

    @Test
    fun `failed scheduling does not consume allowance or stop ringing`() = runTest {
        val failing = object : com.timilehinaregbesola.mathalarm.interactors.AlarmInteractor by alarmInteractor {
            override suspend fun scheduleSnooze(alarm: Alarm, timeInMillis: Long) { error("Scheduling failed") }
        }
        addAlarmUseCase(baseAlarm)
        notificationInteractor.show(baseAlarm)
        assertFailsWith<IllegalStateException> {
            SnoozeAlarm(dateTimeProvider, notificationInteractor, failing, alarmRepository)(baseAlarm.alarmId)
        }
        alarmRepository.findAlarm(baseAlarm.alarmId)!!.snoozeCount shouldBe 0
        notificationInteractor.isNotificationShown(baseAlarm.alarmId) shouldBe true
    }


    @Test
    fun `scheduled metadata includes the accepted snooze count`() = runTest {
        var scheduled: Alarm? = null
        val backend = object : com.timilehinaregbesola.mathalarm.interactors.AlarmInteractor by alarmInteractor {
            override suspend fun scheduleSnooze(alarm: Alarm, timeInMillis: Long) { scheduled = alarm }
        }
        addAlarmUseCase(baseAlarm.copy(snoozeCount = 2, activeAt = 1000))
        SnoozeAlarm(dateTimeProvider, notificationInteractor, backend, alarmRepository)(baseAlarm.alarmId) shouldBe true
        scheduled!!.snoozeCount shouldBe 3
        scheduled!!.canSnooze shouldBe false
        scheduled shouldBe alarmRepository.findAlarm(baseAlarm.alarmId)
    }


    @Test
    fun `snooze uses the saved custom duration without an override`() = runTest {
        dateTimeProvider.setFixedDateTime(2025, 1, 8, 7, 55)
        addAlarmUseCase(baseAlarm.copy(snooze = 12))
        snoozeAlarmUseCase(baseAlarm.alarmId) shouldBe true
        val scheduled = instantToLocalDateTime(alarmInteractor.getAlarmTimeMillis(baseAlarm.alarmId)!!)
        scheduled.hour shouldBe 8
        scheduled.minute shouldBe 7
    }

}
