package com.timilehinaregbesola.mathalarm.usecases

import com.timilehinaregbesola.mathalarm.data.AlarmRepository
import com.timilehinaregbesola.mathalarm.domain.model.Alarm
import com.timilehinaregbesola.mathalarm.fake.AlarmInteractorFake
import com.timilehinaregbesola.mathalarm.fake.AlarmRepositoryFake
import com.timilehinaregbesola.mathalarm.fake.DateTimeProviderFake
import com.timilehinaregbesola.mathalarm.provider.AlarmTimeCalculatorImpl
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue
import kotlin.time.Instant

class SkipNextAlarmTest {
    private val repository = AlarmRepository(AlarmRepositoryFake())
    private val interactor = AlarmInteractorFake()
    private val clock = DateTimeProviderFake()
    private val calculator = AlarmTimeCalculatorImpl(clock) { TimeZone.currentSystemDefault() }
    private val rescheduler = RescheduleFutureAlarms(repository, interactor, calculator)
    private val subject = SkipNextAlarm(repository, calculator, rescheduler)

    @BeforeTest
    fun setUp() = runTest {
        repository.clear()
        interactor.clear()
        clock.setFixedDateTime(LocalDateTime(2025, 1, 6, 6, 0))
    }

    @Test
    fun `skip persists the next date and schedules that weekday one week later`() = runTest {
        repository.addAlarm(repeatingAlarm())

        val skipped = subject(41)

        assertEquals("2025-01-06", repository.findAlarm(41)?.skippedDate)
        assertEquals("2025-01-06", skipped)
        assertEquals(
            listOf("2025-01-08", "2025-01-13"),
            repository.findAlarm(41)!!.pendingTimes.map { it.toDate() }.sorted(),
        )
    }

    @Test
    fun `undo restores the skipped occurrence`() = runTest {
        repository.addAlarm(repeatingAlarm())
        subject(41)

        assertTrue(subject.undo(41))

        val restored = repository.findAlarm(41)!!
        assertNull(restored.skippedDate)
        assertEquals(
            listOf("2025-01-06", "2025-01-08"),
            restored.pendingTimes.map { it.toDate() }.sorted(),
        )
    }

    @Test
    fun `recovery keeps the skipped occurrence omitted`() = runTest {
        repository.addAlarm(repeatingAlarm())
        subject(41)
        interactor.clear()

        rescheduler()

        assertEquals("2025-01-06", repository.findAlarm(41)?.skippedDate)
        assertEquals(
            listOf("2025-01-08", "2025-01-13"),
            interactor.getScheduledAlarms().getValue(41).map { it.timeInMillis.toDate() }.sorted(),
        )
    }

    @Test
    fun `disabled and one-time alarms cannot be skipped`() = runTest {
        repository.addAlarm(repeatingAlarm().copy(alarmId = 42, isOn = false))
        repository.addAlarm(repeatingAlarm().copy(alarmId = 43, repeat = false))

        assertNull(subject(42))
        assertNull(subject(43))
        assertFalse(subject.undo(42))
    }

    private fun repeatingAlarm() = Alarm(
        alarmId = 41,
        hour = 7,
        minute = 0,
        repeat = true,
        repeatDays = "FTFTFFF",
        isOn = true,
        isSaved = true,
    )

    private fun Long.toDate(): String = Instant.fromEpochMilliseconds(this)
        .toLocalDateTime(TimeZone.currentSystemDefault())
        .date
        .toString()
}
