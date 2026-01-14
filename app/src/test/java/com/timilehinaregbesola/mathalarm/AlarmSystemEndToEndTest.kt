package com.timilehinaregbesola.mathalarm

import android.app.AlarmManager
import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.test.core.app.ApplicationProvider
import co.touchlab.kermit.Logger
import com.timilehinaregbesola.mathalarm.data.AlarmRepository
import com.timilehinaregbesola.mathalarm.domain.model.Alarm
import com.timilehinaregbesola.mathalarm.framework.Usecases
import com.timilehinaregbesola.mathalarm.notification.AlarmNotificationScheduler
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.GlobalContext
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config
import org.robolectric.shadows.ShadowAlarmManager
import org.robolectric.shadows.ShadowLooper
import java.util.Calendar


@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.R], application = TestApplication::class)
class AlarmSystemEndToEndTest {

    private lateinit var usecases: Usecases
    private lateinit var alarmRepository: AlarmRepository
    
    private lateinit var context: Context
    private lateinit var alarmManager: AlarmManager
    private lateinit var shadowAlarmManager: ShadowAlarmManager
    private lateinit var alarmReceiver: AlarmReceiver

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext<Application>()
        alarmManager = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        shadowAlarmManager = shadowOf(alarmManager)
        alarmReceiver = AlarmReceiver()
        
        try {
            // Get Koin dependencies from the global context
            usecases = GlobalContext.get().get()
            alarmRepository = GlobalContext.get().get()
        } catch (e: Exception) {
            // If Koin is not initialized, skip these tests
            // In a production setup, ensure TestApplication properly initializes Koin
            throw IllegalStateException("Koin not initialized. These tests require proper Koin setup in TestApplication.", e)
        }
    }

    @Test
    fun `end-to-end multi-day alarm - schedule Tuesday and Friday, let alarms fire naturally`() = runBlocking {
        // Create alarm for Tuesday (index 2) and Friday (index 5) at 7:00 AM
        val alarm = Alarm(
            alarmId = 0L, // Will be auto-assigned
            hour = 7,
            minute = 0,
            repeat = false,
            repeatDays = "FFTFTFF", // Tuesday and Friday
            isOn = false,
            isSaved = true,
            title = "Tuesday & Friday Alarm"
        )
        
        // Step 1: Add and schedule the alarm through the normal flow
        usecases.addAlarm(alarm)
        val savedAlarm = alarmRepository.getLatestAlarm()!!
        usecases.scheduleAlarm(savedAlarm, reschedule = false)
        
        // Verify alarm is ON
        val scheduledAlarm = alarmRepository.findAlarm(savedAlarm.alarmId)!!
        assertTrue("Alarm should be ON after scheduling", scheduledAlarm.isOn)
        
        // Verify both days are scheduled in AlarmManager
        val scheduledAlarms = shadowAlarmManager.scheduledAlarms
        assertEquals("Should have 2 scheduled alarms (Tuesday and Friday)", 2, scheduledAlarms.size)
        
        // Step 2: Let the next scheduled alarm fire naturally
        val nextAlarm = shadowAlarmManager.nextScheduledAlarm
        if (nextAlarm == null) {
            // If there's no next alarm, fail with a helpful message
            val allAlarms = shadowAlarmManager.scheduledAlarms
            fail("Expected next alarm to be scheduled, but found ${allAlarms.size} total alarms")
        }
        
        // Fire the next alarm (Tuesday) - this simulates the system triggering it
        fireNextScheduledAlarm()
        processAsyncOperations()
        
        // Complete the alarm (user solves the math problem)
        usecases.completeAlarm(savedAlarm.alarmId)
        processAsyncOperations()
        
        // Step 3: Verify alarm is still ON (multi-day alarm should stay on)
        val alarmAfterTuesday = alarmRepository.findAlarm(savedAlarm.alarmId)
        assertNotNull("Alarm should still exist in database", alarmAfterTuesday)
        assertTrue("Multi-day alarm should still be ON after Tuesday rings", alarmAfterTuesday!!.isOn)
        
        // Step 4: Verify there are still alarms scheduled (Friday and/or next week's Tuesday)
        val allAlarmsAfter = shadowAlarmManager.scheduledAlarms
        val remainingAlarms = allAlarmsAfter.filter {
            val intent = shadowOf(it.operation).savedIntent
            intent.getLongExtra(AlarmReceiver.EXTRA_TASK, -1) == savedAlarm.alarmId
        }
        
        // Debug: Print what we found
        println("Total alarms after firing: ${allAlarmsAfter.size}")
        println("Alarms for alarm ID ${savedAlarm.alarmId}: ${remainingAlarms.size}")
        allAlarmsAfter.forEach {
            val intent = shadowOf(it.operation).savedIntent
            val alarmId = intent.getLongExtra(AlarmReceiver.EXTRA_TASK, -1)
            println("  - Alarm ID: $alarmId, Time: ${it.triggerAtTime}")
        }
        
        assertTrue("Should still have alarm(s) scheduled after Tuesday (e.g., Friday). " +
            "Found ${remainingAlarms.size} alarms for this alarm ID out of ${allAlarmsAfter.size} total", 
            remainingAlarms.isNotEmpty())
        
        // Step 5: If there's another alarm, let it fire and complete it
        if (shadowAlarmManager.nextScheduledAlarm != null) {
            fireNextScheduledAlarm()
            processAsyncOperations()
            
            // Complete the alarm again
            usecases.completeAlarm(savedAlarm.alarmId)
            processAsyncOperations()
            
            // Alarm should still be ON
            val alarmAfterSecond = alarmRepository.findAlarm(savedAlarm.alarmId)
            assertNotNull("Alarm should still exist after second fire", alarmAfterSecond)
            assertTrue("Multi-day alarm should still be ON after second day rings", alarmAfterSecond!!.isOn)
        }
    }
    
    @Test
    fun `end-to-end weekly repeating alarm - schedule, let it ring, verify next week scheduled`() = runBlocking {
        // Create alarm for only Tuesday with repeat flag (weekly repeat)
        val alarm = Alarm(
            alarmId = 0L,
            hour = 8,
            minute = 30,
            repeat = true, // Weekly repeat
            repeatDays = "FFTFFFF", // Only Tuesday
            isOn = false,
            isSaved = true,
            title = "Weekly Tuesday Alarm"
        )
        
        // Add and schedule
        usecases.addAlarm(alarm)
        val savedAlarm = alarmRepository.getLatestAlarm()!!
        usecases.scheduleAlarm(savedAlarm, reschedule = false)
        
        // Verify scheduled
        assertTrue("Alarm should be ON", alarmRepository.findAlarm(savedAlarm.alarmId)!!.isOn)
        assertEquals("Should have 1 scheduled alarm", 1, shadowAlarmManager.scheduledAlarms.size)
        
        // Let the alarm fire naturally
        fireNextScheduledAlarm()
        processAsyncOperations()
        
        // Complete the alarm (user solves the math)
        usecases.completeAlarm(savedAlarm.alarmId)
        processAsyncOperations()
        
        // Alarm should still be ON (weekly repeat)
        val alarmAfterRing = alarmRepository.findAlarm(savedAlarm.alarmId)!!
        assertTrue("Weekly repeating alarm should stay ON after ringing", alarmAfterRing.isOn)
        
        // Next week should be scheduled
        val remainingAlarms = shadowAlarmManager.scheduledAlarms
        assertTrue("Next week's alarm should be scheduled", remainingAlarms.isNotEmpty())
    }
    
    @Test
    fun `end-to-end one-time alarm - let it ring, complete, verify turned off`() = runBlocking {
        // Create one-time alarm for tomorrow
        val alarm = Alarm(
            alarmId = 0L,
            hour = 9,
            minute = 0,
            repeat = false,
            repeatDays = "FFTFFFF", // Only Tuesday, no repeat
            isOn = false,
            isSaved = true,
            title = "One-time Alarm"
        )
        
        // Add and schedule
        usecases.addAlarm(alarm)
        val savedAlarm = alarmRepository.getLatestAlarm()!!
        usecases.scheduleAlarm(savedAlarm, reschedule = false)
        
        // Verify scheduled
        assertTrue("Alarm should be ON", alarmRepository.findAlarm(savedAlarm.alarmId)!!.isOn)
        assertEquals("Should have 1 scheduled alarm", 1, shadowAlarmManager.scheduledAlarms.size)
        
        // Let the alarm fire and get the intent details
        val nextAlarm = shadowAlarmManager.nextScheduledAlarm!!
        val baseIntent = shadowOf(nextAlarm.operation).savedIntent
        
        // Simulate user completing the alarm after it rings
        val completeIntent = Intent(baseIntent).apply {
            action = AlarmReceiver.COMPLETE_ACTION
        }
        alarmReceiver.onReceive(context, completeIntent)
        processAsyncOperations()
        
        // One-time alarm should be OFF
        val alarmAfterComplete = alarmRepository.findAlarm(savedAlarm.alarmId)!!
        assertFalse("One-time alarm should be OFF after completion", alarmAfterComplete.isOn)
        
        // No future alarms should be scheduled for this alarm
        val remainingAlarms = shadowAlarmManager.scheduledAlarms
        assertTrue("No future alarms should be scheduled for one-time alarm", 
            remainingAlarms.isEmpty() || remainingAlarms.none { 
                val intent = shadowOf(it.operation).savedIntent
                intent.getLongExtra(AlarmReceiver.EXTRA_TASK, -1) == savedAlarm.alarmId
            })
    }
    
    @Test
    fun `end-to-end weekday alarm - let Monday fire, verify stays on for remaining days`() = runBlocking {
        // Create weekday alarm (Mon-Fri)
        val alarm = Alarm(
            alarmId = 0L,
            hour = 6,
            minute = 30,
            repeat = true,
            repeatDays = "FTTTTTF", // Monday through Friday
            isOn = false,
            isSaved = true,
            title = "Weekday Alarm"
        )
        
        // Add and schedule
        usecases.addAlarm(alarm)
        val savedAlarm = alarmRepository.getLatestAlarm()!!
        usecases.scheduleAlarm(savedAlarm, reschedule = false)
        
        // Verify 5 days are scheduled
        val scheduledAlarms = shadowAlarmManager.scheduledAlarms
        assertEquals("Should have 5 weekday alarms", 5, scheduledAlarms.size)
        
        // Verify alarm is ON
        assertTrue("Alarm should be ON", alarmRepository.findAlarm(savedAlarm.alarmId)!!.isOn)
        
        // Let the next alarm (Monday) fire and complete it
        val nextAlarm = shadowAlarmManager.nextScheduledAlarm!!
        val baseIntent = shadowOf(nextAlarm.operation).savedIntent
        val completeIntent = Intent(baseIntent).apply {
            action = AlarmReceiver.COMPLETE_ACTION
        }
        alarmReceiver.onReceive(context, completeIntent)
        processAsyncOperations()
        
        // After Monday, alarm should still be ON (Tue-Fri remaining)
        val alarmAfterMonday = alarmRepository.findAlarm(savedAlarm.alarmId)!!
        assertTrue("Weekday alarm should stay ON after Monday", alarmAfterMonday.isOn)
        
        // Should still have alarms scheduled for remaining days
        val remainingAlarms = shadowAlarmManager.scheduledAlarms.filter {
            val intent = shadowOf(it.operation).savedIntent
            intent.getLongExtra(AlarmReceiver.EXTRA_TASK, -1) == savedAlarm.alarmId
        }
        assertTrue("Should have remaining weekday alarms", remainingAlarms.isNotEmpty())
    }
    
    @Test
    fun `end-to-end multiple alarms - let first fire, verify second remains independent`() = runBlocking {
        // Create two independent alarms
        val alarm1 = Alarm(
            alarmId = 0L,
            hour = 7,
            minute = 0,
            repeat = false,
            repeatDays = "FFTFFFF", // Tuesday
            isOn = false,
            isSaved = true,
            title = "Alarm 1"
        )
        
        val alarm2 = Alarm(
            alarmId = 0L,
            hour = 8,
            minute = 30,
            repeat = false,
            repeatDays = "FFTFFFF", // Tuesday
            isOn = false,
            isSaved = true,
            title = "Alarm 2"
        )
        
        // Add and schedule both
        usecases.addAlarm(alarm1)
        val savedAlarm1 = alarmRepository.getLatestAlarm()!!
        usecases.scheduleAlarm(savedAlarm1, reschedule = false)
        
        usecases.addAlarm(alarm2)
        val savedAlarm2 = alarmRepository.getLatestAlarm()!!
        usecases.scheduleAlarm(savedAlarm2, reschedule = false)
        
        // Verify both are scheduled
        val scheduledAlarms = shadowAlarmManager.scheduledAlarms
        assertEquals("Should have 2 alarms scheduled", 2, scheduledAlarms.size)
        
        // Let the next alarm (alarm 1, earlier time) fire and complete it
        val nextAlarm = shadowAlarmManager.nextScheduledAlarm!!
        val baseIntent = shadowOf(nextAlarm.operation).savedIntent
        val savedAlarm1Id = baseIntent.getLongExtra(AlarmReceiver.EXTRA_TASK, -1)
        
        val completeIntent = Intent(baseIntent).apply {
            action = AlarmReceiver.COMPLETE_ACTION
        }
        alarmReceiver.onReceive(context, completeIntent)
        processAsyncOperations()
        
        // Verify alarm 1 is OFF
        val alarm1After = alarmRepository.findAlarm(savedAlarm1.alarmId)!!
        assertFalse("Alarm 1 should be OFF after completion", alarm1After.isOn)
        
        // Verify alarm 2 is still ON and scheduled
        val alarm2After = alarmRepository.findAlarm(savedAlarm2.alarmId)!!
        assertTrue("Alarm 2 should still be ON", alarm2After.isOn)
        
        // Alarm 2 should still be in scheduled alarms
        val alarm2Still = shadowAlarmManager.scheduledAlarms.find {
            val intent = shadowOf(it.operation).savedIntent
            intent.getLongExtra(AlarmReceiver.EXTRA_TASK, -1) == savedAlarm2.alarmId
        }
        assertNotNull("Alarm 2 should still be scheduled", alarm2Still)
    }
    
    @Test
    fun `end-to-end multi-day weekly repeating alarm - verify multiple weeks continue ringing`() = runBlocking {
        // Create alarm for Tuesday and Friday with weekly repeat
        val alarm = Alarm(
            alarmId = 0L,
            hour = 7,
            minute = 30,
            repeat = true, // Weekly repeat
            repeatDays = "FFTFTFF", // Tuesday and Friday
            isOn = false,
            isSaved = true,
            title = "Tuesday & Friday Weekly Alarm"
        )
        
        // Step 1: Schedule the alarm
        usecases.addAlarm(alarm)
        val savedAlarm = alarmRepository.getLatestAlarm()!!
        usecases.scheduleAlarm(savedAlarm, reschedule = false)
        
        // Verify scheduled
        assertTrue("Alarm should be ON", alarmRepository.findAlarm(savedAlarm.alarmId)!!.isOn)
        val initialAlarms = shadowAlarmManager.scheduledAlarms
        assertEquals("Should have 2 alarms (Tuesday and Friday)", 2, initialAlarms.size)
        
        // Step 2: Fire Week 1 Tuesday and complete it
        fireNextScheduledAlarm()
        processAsyncOperations()
        usecases.completeAlarm(savedAlarm.alarmId)
        processAsyncOperations()
        
        // Alarm should still be ON
        val afterWeek1Tuesday = alarmRepository.findAlarm(savedAlarm.alarmId)!!
        assertTrue("Alarm should stay ON after Week 1 Tuesday", afterWeek1Tuesday.isOn)
        
        // Should have alarms for Week 1 Friday + Week 2 Tuesday + Week 2 Friday
        val afterWeek1TuesdayAlarms = shadowAlarmManager.scheduledAlarms.filter {
            val intent = shadowOf(it.operation).savedIntent
            intent.getLongExtra(AlarmReceiver.EXTRA_TASK, -1) == savedAlarm.alarmId
        }
        assertTrue("Should have remaining alarms after Week 1 Tuesday", afterWeek1TuesdayAlarms.isNotEmpty())
        
        // Step 3: Fire Week 1 Friday and complete it
        fireNextScheduledAlarm()
        processAsyncOperations()
        usecases.completeAlarm(savedAlarm.alarmId)
        processAsyncOperations()
        
        // Alarm should still be ON
        val afterWeek1Friday = alarmRepository.findAlarm(savedAlarm.alarmId)!!
        assertTrue("Alarm should stay ON after Week 1 Friday", afterWeek1Friday.isOn)
        
        // Should have alarms for Week 2 Tuesday + Week 2 Friday
        val afterWeek1Alarms = shadowAlarmManager.scheduledAlarms.filter {
            val intent = shadowOf(it.operation).savedIntent
            intent.getLongExtra(AlarmReceiver.EXTRA_TASK, -1) == savedAlarm.alarmId
        }
        assertTrue("Should have Week 2 alarms scheduled", afterWeek1Alarms.isNotEmpty())
        
        // Step 4: Fire Week 2 Tuesday and complete it
        val week2StartingAlarms = shadowAlarmManager.scheduledAlarms.size
        fireNextScheduledAlarm()
        processAsyncOperations()
        usecases.completeAlarm(savedAlarm.alarmId)
        processAsyncOperations()
        
        // Alarm should STILL be ON after Week 2 Tuesday
        val afterWeek2Tuesday = alarmRepository.findAlarm(savedAlarm.alarmId)!!
        assertTrue("Alarm should stay ON after Week 2 Tuesday", afterWeek2Tuesday.isOn)
        
        // Should still have alarms (Week 2 Friday + Week 3 alarms)
        val afterWeek2TuesdayAlarms = shadowAlarmManager.scheduledAlarms.filter {
            val intent = shadowOf(it.operation).savedIntent
            intent.getLongExtra(AlarmReceiver.EXTRA_TASK, -1) == savedAlarm.alarmId
        }
        assertTrue("Should have Week 2 Friday and Week 3 alarms", afterWeek2TuesdayAlarms.isNotEmpty())
        
        // Step 5: Fire Week 2 Friday and complete it
        fireNextScheduledAlarm()
        processAsyncOperations()
        usecases.completeAlarm(savedAlarm.alarmId)
        processAsyncOperations()
        
        // Alarm should STILL be ON after completing Week 2
        val afterWeek2 = alarmRepository.findAlarm(savedAlarm.alarmId)!!
        assertTrue("Alarm should stay ON indefinitely for weekly repeating alarm", afterWeek2.isOn)
        
        // Should have Week 3 alarms scheduled
        val week3Alarms = shadowAlarmManager.scheduledAlarms.filter {
            val intent = shadowOf(it.operation).savedIntent
            intent.getLongExtra(AlarmReceiver.EXTRA_TASK, -1) == savedAlarm.alarmId
        }
        assertTrue("Should have Week 3 alarms scheduled", week3Alarms.isNotEmpty())
    }
    
    @Test
    fun `end-to-end boot receiver - system reschedules alarms automatically after reboot`() = runBlocking {
        // Create and schedule alarm before "reboot"
        val alarm = Alarm(
            alarmId = 0L,
            hour = 10,
            minute = 0,
            repeat = true,
            repeatDays = "TTTTTTT", // Every day
            isOn = true, // Already ON
            isSaved = true,
            title = "Daily Alarm"
        )
        
        usecases.addAlarm(alarm)
        val savedAlarm = alarmRepository.getLatestAlarm()!!
        
        // Simulate boot by clearing scheduled alarms
        shadowAlarmManager.scheduledAlarms.clear()
        
        // Verify no alarms scheduled (simulating fresh boot)
        assertEquals("No alarms after boot", 0, shadowAlarmManager.scheduledAlarms.size)
        
        // Trigger boot receiver - this simulates Android sending the BOOT_COMPLETED broadcast
        val bootIntent = Intent(Intent.ACTION_BOOT_COMPLETED)
        alarmReceiver.onReceive(context, bootIntent)
        processAsyncOperations()
        
        // Verify alarms are automatically rescheduled by the system
        val rescheduledAlarms = shadowAlarmManager.scheduledAlarms
        assertTrue("Alarms should be rescheduled after boot", rescheduledAlarms.isNotEmpty())
        
        // Verify the alarm is still ON
        val alarmAfterBoot = alarmRepository.findAlarm(savedAlarm.alarmId)!!
        assertTrue("Alarm should still be ON after boot", alarmAfterBoot.isOn)
    }
    
    /**
     * Fires the next scheduled alarm in the system.
     * This simulates the Android system triggering the alarm at its scheduled time.
     */
    private fun fireNextScheduledAlarm() {
        val nextAlarm = shadowAlarmManager.nextScheduledAlarm
        if (nextAlarm != null) {
            val pendingIntent = nextAlarm.operation
            val intent = shadowOf(pendingIntent).savedIntent
            
            // Deliver the intent to the broadcast receiver (simulates system behavior)
            alarmReceiver.onReceive(context, intent)
        }
    }
    
    /**
     * Process all async operations to completion.
     */
    private fun processAsyncOperations() {
        ShadowLooper.idleMainLooper()
        Thread.sleep(100) // Give coroutines time to complete
        ShadowLooper.idleMainLooper() // Process any resulting work
    }
}
