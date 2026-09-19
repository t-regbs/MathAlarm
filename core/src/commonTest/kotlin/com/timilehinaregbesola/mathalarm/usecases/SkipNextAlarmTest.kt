package com.timilehinaregbesola.mathalarm.usecases

import com.timilehinaregbesola.mathalarm.data.AlarmRepository
import com.timilehinaregbesola.mathalarm.domain.model.Alarm
import com.timilehinaregbesola.mathalarm.fake.AlarmInteractorFake
import com.timilehinaregbesola.mathalarm.fake.AlarmRepositoryFake
import com.timilehinaregbesola.mathalarm.fake.NotificationInteractorFake
import com.timilehinaregbesola.mathalarm.interactors.AlarmInteractor
import com.timilehinaregbesola.mathalarm.fake.DateTimeProviderFake
import com.timilehinaregbesola.mathalarm.provider.AlarmTimeCalculatorImpl
import kotlinx.coroutines.test.runTest
import kotlinx.datetime.DatePeriod
import kotlinx.datetime.plus
import kotlinx.datetime.LocalDateTime
import kotlinx.datetime.TimeZone
import kotlinx.datetime.toLocalDateTime
import kotlinx.datetime.toInstant
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith
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
    fun `disabled alarms cannot be skipped`() = runTest {
        repository.addAlarm(repeatingAlarm().copy(alarmId = 42, isOn = false))

        assertNull(subject(42))
        assertFalse(subject.undo(42))
    }

    @Test
    fun `single-day one-time alarm cannot be skipped`() = runTest {
        repository.addAlarm(
            repeatingAlarm().copy(
                alarmId = 45,
                repeat = false,
                repeatDays = "FTFFFFF",
            )
        )

        assertNull(subject(45))

        val unchanged = repository.findAlarm(45)!!
        assertTrue(unchanged.isOn)
        assertNull(unchanged.skippedDate)
    }

    @Test
    fun `skip removes only the next occurrence from a one-time sequence`() = runTest {
        repository.addAlarm(repeatingAlarm().copy(alarmId = 43, repeat = false))

        val skipped = subject(43)

        val updated = repository.findAlarm(43)!!
        assertEquals("2025-01-06", skipped)
        assertEquals("2025-01-06", updated.skippedDate)
        assertTrue(updated.isOn)
        assertEquals(listOf("2025-01-08"), updated.pendingTimes.map { it.toDate() })

        assertTrue(subject.undo(43))

        val restored = repository.findAlarm(43)!!
        assertNull(restored.skippedDate)
        assertTrue(restored.isOn)
        assertEquals(
            listOf("2025-01-06", "2025-01-08"),
            restored.pendingTimes.map { it.toDate() }.sorted(),
        )
    }

    @Test
    fun `skipping the final one-time occurrence turns alarm off and undo restores it`() = runTest {
        val alarm = repeatingAlarm().copy(alarmId = 44, repeat = false)
        val finalOccurrence = calculator.calculateAlarmTimes(alarm).max()
        repository.addAlarm(
            alarm.copy(
                scheduleInitialized = true,
                pendingTimes = listOf(finalOccurrence),
                scheduleTimeZone = TimeZone.currentSystemDefault().id,
            )
        )

        assertEquals("2025-01-08", subject(44))

        val skipped = repository.findAlarm(44)!!
        assertFalse(skipped.isOn)
        assertTrue(skipped.pendingTimes.isEmpty())
        assertEquals("2025-01-08", skipped.skippedDate)

        assertTrue(subject.undo(44))

        val restored = repository.findAlarm(44)!!
        assertTrue(restored.isOn)
        assertNull(restored.skippedDate)
        assertEquals(listOf("2025-01-08"), restored.pendingTimes.map { it.toDate() })
    }

    @Test
    fun `recovery expires a disabled final skip without rearming the alarm`() = runTest {
        repository.addAlarm(repeatingAlarm().copy(
            repeat = false, isOn = false, skippedDate = "2025-01-06",
            scheduleInitialized = true, pendingTimes = emptyList(),
        ))
        clock.setFixedDateTime(LocalDateTime(2025, 1, 6, 8, 0))
        rescheduler()
        val saved = repository.findAlarm(41)!!
        assertNull(saved.skippedDate)
        assertFalse(saved.isOn)
        assertTrue(saved.pendingTimes.isEmpty())
        assertTrue(interactor.getScheduledAlarms().isEmpty())
    }

    @Test
    fun `expired repeating skip can be replaced without reopening app`() = runTest {
        repository.addAlarm(repeatingAlarm())
        subject(41)
        clock.setFixedDateTime(LocalDateTime(2025, 1, 6, 8, 0))
        assertEquals("2025-01-08", subject(41))
        assertEquals("2025-01-08", repository.findAlarm(41)!!.skippedDate)
        assertEquals(repository.findAlarm(41)!!.pendingTimes,
            interactor.getScheduledAlarms().getValue(41).map { it.timeInMillis }.sorted())
        assertEquals(listOf("2025-01-13", "2025-01-15"),
            repository.findAlarm(41)!!.pendingTimes.map { it.toDate() })
    }

    @Test
    fun `undo after travelling west uses new local time even when old-zone time passed`() = runTest {
        val zone = TimeZone.of("America/New_York")
        val localCalculator = AlarmTimeCalculatorImpl(clock) { zone }
        val restore = RescheduleFutureAlarms(repository, interactor, localCalculator) { zone }
        val skip = SkipNextAlarm(repository, localCalculator, restore) { zone }
        repository.addAlarm(repeatingAlarm().copy(
            repeat = false, isOn = false, skippedDate = "2025-01-06",
            scheduleInitialized = true, scheduleTimeZone = "Europe/London",
        ))
        assertTrue(skip.undo(41))
        val expected = LocalDateTime(2025, 1, 6, 7, 0).toInstant(zone).toEpochMilliseconds()
        assertEquals(listOf(expected), repository.findAlarm(41)!!.pendingTimes)
        assertNull(repository.findAlarm(41)!!.skippedDate)
        assertEquals(listOf(expected), interactor.getScheduledAlarms().getValue(41).map { it.timeInMillis })
    }

    @Test
    fun `turning on an expired skipped alarm starts a fresh schedule in one operation`() = runTest {
        repository.addAlarm(repeatingAlarm().copy(
            repeat = false, isOn = false, skippedDate = "2025-01-06",
            scheduleInitialized = true,
        ))
        clock.setFixedDateTime(LocalDateTime(2025, 1, 6, 8, 0))
        ScheduleAlarm(repository, interactor, calculator)(repository.findAlarm(41)!!, true)
        val saved = repository.findAlarm(41)!!
        assertTrue(saved.isOn)
        assertNull(saved.skippedDate)
        assertEquals(listOf("2025-01-08", "2025-01-13"), saved.pendingTimes.map { it.toDate() })
        assertEquals(saved.pendingTimes, interactor.getScheduledAlarms().getValue(41).map { it.timeInMillis })
    }

    @Test
    fun `every weekday selection skips exactly one occurrence and undo restores it`() = runTest {
        for (repeat in listOf(false, true)) {
            for (mask in 1..127) {
                val days = (0..6).map { if (mask and (1 shl it) != 0) 'T' else 'F' }.joinToString("")
                if (!repeat && days.count { it == 'T' } == 1) continue
                val alarm = repeatingAlarm().copy(repeat = repeat, repeatDays = days)
                repository.clear()
                interactor.clear()
                repository.addAlarm(alarm)
                val original = calculator.calculateAlarmTimes(alarm).sorted()
                val skippedDate = subject(41)
                assertEquals(original.first().toDate(), skippedDate, "repeat=$repeat days=$days")
                val expected = if (repeat) {
                    val zone = TimeZone.currentSystemDefault()
                    val first = Instant.fromEpochMilliseconds(original.first()).toLocalDateTime(zone)
                    val nextWeek = LocalDateTime(first.date + DatePeriod(days = 7), first.time)
                        .toInstant(zone).toEpochMilliseconds()
                    (original.drop(1) + nextWeek).sorted()
                } else original.drop(1)
                assertEquals(expected, repository.findAlarm(41)!!.pendingTimes)
                assertEquals(expected, interactor.getScheduledAlarms().getValue(41).map { it.timeInMillis }.sorted())
                assertTrue(subject.undo(41, skippedDate))
                assertEquals(original, repository.findAlarm(41)!!.pendingTimes)
                assertEquals(original, interactor.getScheduledAlarms().getValue(41).map { it.timeInMillis }.sorted())
            }
        }
    }

    @Test
    fun `skipping one alarm leaves other weekly and finite alarms unchanged`() = runTest {
        for (repeat in listOf(false, true)) {
            repository.clear()
            interactor.clear()
            val first = repeatingAlarm().copy(repeat = repeat)
            val second = first.copy(alarmId = 42)
            val third = first.copy(alarmId = 43, repeat = !repeat, hour = 8)
            for (alarm in listOf(first, second, third)) {
                repository.addAlarm(alarm)
                rescheduler.restoreAlarm(alarm)
            }
            val otherAlarms = listOf(repository.findAlarm(42), repository.findAlarm(43))
            val otherSchedules = interactor.getScheduledAlarms().filterKeys { it != 41L }
            subject(41)
            assertEquals(otherAlarms, listOf(repository.findAlarm(42), repository.findAlarm(43)))
            assertEquals(otherSchedules, interactor.getScheduledAlarms().filterKeys { it != 41L })
            subject.undo(41)
            assertEquals(otherAlarms, listOf(repository.findAlarm(42), repository.findAlarm(43)))
            assertEquals(otherSchedules, interactor.getScheduledAlarms().filterKeys { it != 41L })
        }
    }

    @Test
    fun `stale undo cannot restore a different skipped date`() = runTest {
        repository.addAlarm(repeatingAlarm())
        val firstSkip = subject(41)
        clock.setFixedDateTime(LocalDateTime(2025, 1, 6, 8, 0))
        subject(41)
        val saved = repository.findAlarm(41)
        val scheduled = interactor.getScheduledAlarms()

        assertFalse(subject.undo(41, firstSkip))
        assertEquals(saved, repository.findAlarm(41))
        assertEquals(scheduled, interactor.getScheduledAlarms())
    }

    @Test
    fun `second skip before skipped time does not skip another occurrence`() = runTest {
        repository.addAlarm(repeatingAlarm())
        subject(41)
        val saved = repository.findAlarm(41)
        val scheduled = interactor.getScheduledAlarms()
        assertNull(subject(41))
        assertEquals(saved, repository.findAlarm(41))
        assertEquals(scheduled, interactor.getScheduledAlarms())
    }

    @Test
    fun `finite schedule does not regenerate skipped or completed dates after recovery`() = runTest {
        repository.addAlarm(repeatingAlarm().copy(repeat = false))
        subject(41)
        clock.setFixedDateTime(LocalDateTime(2025, 1, 6, 7, 0))
        assertFalse(subject.undo(41))
        rescheduler()
        assertEquals(listOf("2025-01-08"), repository.findAlarm(41)!!.pendingTimes.map { it.toDate() })
        assertEquals(listOf("2025-01-08"), interactor.getScheduledAlarms().getValue(41).map { it.timeInMillis.toDate() })
        clock.setFixedDateTime(LocalDateTime(2025, 1, 8, 8, 0))
        rescheduler()
        assertFalse(repository.findAlarm(41)!!.isOn)
        assertTrue(repository.findAlarm(41)!!.pendingTimes.isEmpty())
        assertTrue(interactor.getScheduledAlarms().isEmpty())
    }

    @Test
    fun `skip and undo preserve a separate snooze and active occurrence`() = runTest {
        val zone = TimeZone.currentSystemDefault()
        val snooze = LocalDateTime(2025, 1, 6, 6, 5).toInstant(zone).toEpochMilliseconds()
        val active = snooze - 10 * 60 * 1_000L
        val alarm = repeatingAlarm().copy(snoozedUntil = snooze, activeAt = active)
        repository.addAlarm(alarm)
        interactor.scheduleSnooze(alarm, snooze)
        subject(41)
        for (undo in listOf(false, true)) {
            if (undo) assertTrue(subject.undo(41))
            val saved = repository.findAlarm(41)!!
            assertEquals(snooze, saved.snoozedUntil)
            assertEquals(active, saved.activeAt)
            assertEquals((saved.pendingTimes + snooze).sorted(),
                interactor.getScheduledAlarms().getValue(41).map { it.timeInMillis }.sorted())
        }
    }

    @Test
    fun `a stale skipped delivery stays silent after recovery and valid occurrences still ring`() = runTest {
        for ((repeat, days) in listOf(true to "FTFFFFF", true to "FTFTFFF", false to "FTFTFFF")) {
            for (recover in listOf(false, true)) {
                repository.clear()
                interactor.clear()
                val alarm = repeatingAlarm().copy(repeat = repeat, repeatDays = days)
                repository.addAlarm(alarm)
                val originalNext = calculator.calculateNextAlarmTime(alarm)!!
                subject(41)
                clock.setFixedDateTime(LocalDateTime(2025, 1, 6, 7, 0))
                if (recover) rescheduler()
                val saved = repository.findAlarm(41)
                val scheduled = interactor.getScheduledAlarms()
                val notifications = NotificationInteractorFake()
                val show = ShowAlarm(repository, notifications, ScheduleNextAlarm(interactor, calculator))
                show(41, originalNext)
                assertFalse(notifications.isNotificationShown(41))
                assertEquals(saved, repository.findAlarm(41))
                assertEquals(scheduled, interactor.getScheduledAlarms())
                val valid = saved!!.pendingTimes.first()
                clock.setFixedDateTime(Instant.fromEpochMilliseconds(valid).toLocalDateTime(TimeZone.currentSystemDefault()))
                show(41, valid)
                assertTrue(notifications.isNotificationShown(41))
                clock.setFixedDateTime(LocalDateTime(2025, 1, 6, 6, 0))
            }
        }
    }

    @Test
    fun `failed skip scheduling is persisted and recovered without resurrecting the skipped date`() = runTest {
        val failingInteractor = object : AlarmInteractor by interactor {
            override suspend fun scheduleRepeating(alarm: Alarm, times: List<Long>) {
                throw IllegalStateException("Scheduling unavailable")
            }
        }
        val failingRestore = RescheduleFutureAlarms(repository, failingInteractor, calculator)
        val failingSkip = SkipNextAlarm(repository, calculator, failingRestore)
        repository.addAlarm(repeatingAlarm())
        assertFailsWith<IllegalStateException> { failingSkip(41) }
        assertEquals("2025-01-06", repository.findAlarm(41)!!.skippedDate)
        assertEquals("Scheduling unavailable", repository.findAlarm(41)!!.scheduleError)
        assertTrue(interactor.getScheduledAlarms().isEmpty())

        rescheduler()
        val saved = repository.findAlarm(41)!!
        assertNull(saved.scheduleError)
        assertEquals(listOf("2025-01-08", "2025-01-13"), saved.pendingTimes.map { it.toDate() })
        assertEquals(saved.pendingTimes, interactor.getScheduledAlarms().getValue(41).map { it.timeInMillis }.sorted())
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
