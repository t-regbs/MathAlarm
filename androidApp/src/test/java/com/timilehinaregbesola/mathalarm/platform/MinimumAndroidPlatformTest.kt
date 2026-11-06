package com.timilehinaregbesola.mathalarm.platform

import android.app.Application
import android.content.Context
import android.content.Intent
import android.os.Vibrator
import android.provider.Settings
import org.junit.After
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [26], application = Application::class)
class MinimumAndroidPlatformTest {
    private lateinit var context: Application

    @Before
    fun setUp() {
        context = RuntimeEnvironment.getApplication()
        startKoin { modules(module { single<Context> { context } }) }
    }

    @After
    fun tearDown() {
        stopKoin()
    }

    @Test
    fun `notification settings opens this app's settings from application context`() {
        openNotificationSettings()

        val intent = shadowOf(context).nextStartedActivity
        assertEquals(Settings.ACTION_APP_NOTIFICATION_SETTINGS, intent.action)
        assertEquals(context.packageName, intent.getStringExtra(Settings.EXTRA_APP_PACKAGE))
        assertTrue(intent.flags and Intent.FLAG_ACTIVITY_NEW_TASK != 0)
    }

    @Test
    fun `preview vibration retains waveform repeat and cancellation on minimum supported Android`() {
        val vibrator = shadowOf(context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator)
        val preview = PlatformVibrator()
        val pattern = longArrayOf(0, 100, 200, 100)

        preview.startWaveform(pattern, 1)

        assertTrue(vibrator.isVibrating)
        assertArrayEquals(pattern, vibrator.pattern)
        assertEquals(1, vibrator.repeat)

        preview.cancel()

        assertTrue(vibrator.isCancelled)
    }
}
