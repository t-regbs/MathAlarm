package com.timilehinaregbesola.mathalarm.presentation.appsettings

import co.touchlab.kermit.Logger
import com.russhwolf.settings.MapSettings
import com.russhwolf.settings.Settings
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse

@OptIn(ExperimentalCoroutinesApi::class)
class AppSettingsViewModelTest {
    @Test
    fun `failed announcement bootstrap still creates an owner and refresh retries bookkeeping`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val persisted = MapSettings()
        var reject = true
        val storage = object : Settings by persisted {
            override fun putString(key: String, value: String) {
                if (reject) error("announcement bookkeeping unavailable")
                persisted.putString(key, value)
            }
        }
        val preferences = AlarmPreferencesImpl(AppThemeOptionsMapper(), Logger.withTag("batchRetry"), storage)
        val model = AppSettingsViewModel(preferences, listOf("math-challenges-v1", "snooze-settings-v1"))
        try {
            val failure = model.state.value.failures.single()
            assertEquals(AppSettingsOperation.ANNOUNCEMENT, failure.operation)
            assertEquals(listOf("math-challenges-v1", "snooze-settings-v1"), model.state.value.supportedAnnouncementIds)
            reject = false
            model.refreshAnnouncements()
            assertEquals(model.state.value.announcementIds,
                preferences.latestAnnouncementBatch(model.state.value.supportedAnnouncementIds))
            assertEquals(listOf(failure), model.state.value.failures)
            model.acknowledgeFailure(failure.id)
            assertEquals(emptyList(), model.state.value.failures)
        } finally {
            model.close()
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `supported announcement outside restored batch retains failed acknowledgement and retries`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val persisted = MapSettings()
        var failSeenWrite = false
        val storage = object : Settings by persisted {
            override fun putBoolean(key: String, value: Boolean) {
                if (failSeenWrite) error("storage unavailable")
                persisted.putBoolean(key, value)
            }
        }
        // A frozen manual batch and per-feature seen records are separate persisted formats.
        persisted.putString("mathalarm_announcement_catalog", "math-challenges-v1\nsnooze-settings-v1")
        persisted.putString("mathalarm_announcement_batch", "snooze-settings-v1")
        val preferences = AlarmPreferencesImpl(AppThemeOptionsMapper(), Logger.withTag("announcement"), storage)
        val model = AppSettingsViewModel(preferences, listOf("math-challenges-v1", "snooze-settings-v1"))
        try {
            assertEquals(listOf("snooze-settings-v1"), model.state.value.announcementIds)
            assertEquals(listOf("math-challenges-v1", "snooze-settings-v1"), model.state.value.unseenAnnouncementIds)
            failSeenWrite = true
            model.acknowledgeAnnouncement("math-challenges-v1")
            val failure = model.state.value.failures.single()
            assertEquals(AppSettingsOperation.ANNOUNCEMENT, failure.operation)
            assertFalse(preferences.hasSeenAnnouncement("math-challenges-v1"))
            failSeenWrite = false
            model.acknowledgeAnnouncement("math-challenges-v1")
            assertEquals(true, preferences.hasSeenAnnouncement("math-challenges-v1"))
            assertEquals(listOf("snooze-settings-v1"), model.state.value.unseenAnnouncementIds)
            assertEquals(listOf(failure), model.state.value.failures)
            model.acknowledgeFailure(failure.id)
            assertEquals(emptyList(), model.state.value.failures)
        } finally {
            model.close()
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `failed preference write is retained across a successful retry`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        var failWrite = true
        val persisted = MapSettings()
        val storage = object : Settings by persisted {
            override fun putInt(key: String, value: Int) {
                if (failWrite) error("storage unavailable")
                persisted.putInt(key, value)
            }
        }
        val preferences = AlarmPreferencesImpl(AppThemeOptionsMapper(), Logger.withTag("test"), storage)
        val model = AppSettingsViewModel(preferences)
        try {
            model.selectTheme(AlarmPreferences.Theme.DARK)
            assertEquals(AlarmPreferences.Theme.SYSTEM, model.state.value.theme)
            val failure = model.state.value.failures.single()
            assertEquals(AppSettingsOperation.THEME, failure.operation)
            failWrite = false
            model.selectTheme(AlarmPreferences.Theme.DARK)
            advanceUntilIdle()
            assertEquals(AlarmPreferences.Theme.DARK, model.state.value.theme)
            assertEquals(AlarmPreferences.Theme.DARK, AlarmPreferencesImpl(AppThemeOptionsMapper(), Logger.withTag("restored"), persisted).loadAppTheme())
            assertEquals(listOf(failure), model.state.value.failures)
            model.acknowledgeFailure(failure.id)
            assertEquals(emptyList(), model.state.value.failures)
        } finally {
            model.close()
            Dispatchers.resetMain()
        }
    }

    @Test
    fun `settings observe preference changes and acknowledgement survives recreation`() = runTest {
        Dispatchers.setMain(StandardTestDispatcher(testScheduler))
        val storage = MapSettings()
        val preferences = AlarmPreferencesImpl(AppThemeOptionsMapper(), Logger.withTag("test"), storage)
        val model = AppSettingsViewModel(preferences, listOf("math-challenges-v1", "snooze-settings-v1"))
        try {
            preferences.updateAlarmSortOrder(AlarmPreferences.AlarmSortOrder.TIME)
            preferences.updateAppTheme(AlarmPreferences.Theme.DARK)
            advanceUntilIdle()
            assertEquals(AlarmPreferences.AlarmSortOrder.TIME, model.state.value.sortOrder)
            assertEquals(AlarmPreferences.Theme.DARK, model.state.value.theme)
            model.acknowledgeAnnouncement("math-challenges-v1")
            assertFalse("math-challenges-v1" in model.state.value.unseenAnnouncementIds)
            val restored = AlarmPreferencesImpl(AppThemeOptionsMapper(), Logger.withTag("test"), storage)
            assertEquals(true, restored.hasSeenAnnouncement("math-challenges-v1"))
            assertEquals(AlarmPreferences.Theme.DARK, restored.themeState.value)
            assertEquals(AlarmPreferences.AlarmSortOrder.TIME, restored.alarmSortOrderState.value)
            model.close()
            model.selectTheme(AlarmPreferences.Theme.LIGHT)
            assertEquals(AlarmPreferences.Theme.DARK, preferences.themeState.value)
            preferences.updateAppTheme(AlarmPreferences.Theme.LIGHT)
            advanceUntilIdle()
            // Closed owners stop observing; durable preferences remain independently writable.
            assertEquals(AlarmPreferences.Theme.DARK, model.state.value.theme)
            assertEquals(AlarmPreferences.Theme.LIGHT, preferences.themeState.value)
        } finally {
            model.close()
            Dispatchers.resetMain()
        }
    }
}
