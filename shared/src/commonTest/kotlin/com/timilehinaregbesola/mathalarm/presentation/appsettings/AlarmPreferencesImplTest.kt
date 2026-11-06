package com.timilehinaregbesola.mathalarm.presentation.appsettings

import com.russhwolf.settings.MapSettings
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class AlarmPreferencesImplTest {

    @Test
    fun `theme encoding keeps released storage IDs and invalid values use system`() {
        val released = listOf(0 to AlarmPreferences.Theme.LIGHT,
            1 to AlarmPreferences.Theme.DARK, 2 to AlarmPreferences.Theme.SYSTEM)
        for ((id, expected) in released) {
            val settings = MapSettings()
            settings.putInt(AlarmPreferencesImpl.APP_THEME_OPTION, id)
            val preferences = AlarmPreferencesImpl(settings)
            preferences.loadAppTheme() shouldBe expected
            for ((writtenId, theme) in released) {
                preferences.updateAppTheme(theme)
                settings.getInt(AlarmPreferencesImpl.APP_THEME_OPTION, -1) shouldBe writtenId
                AlarmPreferencesImpl(settings)
                    .loadAppTheme() shouldBe theme
            }
        }
        for (invalid in listOf(-1, 3, Int.MAX_VALUE)) {
            val settings = MapSettings()
            settings.putInt(AlarmPreferencesImpl.APP_THEME_OPTION, invalid)
            AlarmPreferencesImpl(settings)
                .loadAppTheme() shouldBe AlarmPreferences.Theme.SYSTEM
        }
        AlarmPreferencesImpl(MapSettings())
            .loadAppTheme() shouldBe AlarmPreferences.Theme.SYSTEM
    }

    @Test
    fun `alarm sort order persists across preference recreation`() {
        val settings = MapSettings()

        AlarmPreferencesImpl(settings).updateAlarmSortOrder(AlarmPreferences.AlarmSortOrder.TIME)

        val recreatedPreferences = AlarmPreferencesImpl(settings)

        recreatedPreferences.loadAlarmSortOrder() shouldBe AlarmPreferences.AlarmSortOrder.TIME
    }

    @Test
    fun `acknowledged announcement stays dismissed after restart`() {
        val settings = MapSettings()
        val preferences = AlarmPreferencesImpl(settings)
        preferences.hasSeenAnnouncement("math-challenges-v1") shouldBe false
        preferences.markAnnouncementSeen("math-challenges-v1")
        preferences.hasSeenAnnouncement("math-challenges-v1") shouldBe true

        val recreated = AlarmPreferencesImpl(settings)
        recreated.hasSeenAnnouncement("math-challenges-v1") shouldBe true
    }

    @Test
    fun `new feature release is eligible after previous announcement was acknowledged`() {
        val preferences = AlarmPreferencesImpl(MapSettings())
        preferences.markAnnouncementSeen("math-challenges-v1")
        preferences.hasSeenAnnouncement("next-feature-v1") shouldBe false
        preferences.markAnnouncementSeen("next-feature-v1")
        preferences.hasSeenAnnouncement("next-feature-v1") shouldBe true
    }
    @Test
    fun `seen features accumulate and survive recreation`() {
        val settings = MapSettings()
        val preferences = AlarmPreferencesImpl(settings)
        preferences.markAnnouncementSeen("math-challenges-v1")
        preferences.markAnnouncementSeen("skip-next-alarm-v1")
        preferences.markAnnouncementSeen("math-challenges-v1")

        val recreated = AlarmPreferencesImpl(settings)
        recreated.hasSeenAnnouncement("math-challenges-v1") shouldBe true
        recreated.hasSeenAnnouncement("skip-next-alarm-v1") shouldBe true
        recreated.hasSeenAnnouncement("future-feature") shouldBe false
    }

    @Test
    fun `sequential updates replace replay batch with only new features`() {
        val settings = MapSettings()
        val prefs = AlarmPreferencesImpl(settings)
        prefs.latestAnnouncementBatch(listOf("math")) shouldBe listOf("math")
        prefs.markAnnouncementSeen("math")
        prefs.latestAnnouncementBatch(listOf("math", "skip")) shouldBe listOf("skip")
        prefs.markAnnouncementSeen("skip")
        prefs.latestAnnouncementBatch(listOf("math", "skip", "future")) shouldBe listOf("future")
    }

    @Test
    fun `skipped releases include all missed features but exclude older acknowledged ones`() {
        val prefs = AlarmPreferencesImpl(MapSettings())
        prefs.markAnnouncementSeen("old")
        prefs.latestAnnouncementBatch(listOf("old", "math", "skip")) shouldBe listOf("math", "skip")
    }

    @Test
    fun `update replay survives partial acknowledgement completion and restart`() {
        val settings = MapSettings()
        val catalog = listOf("math", "skip")
        val prefs = AlarmPreferencesImpl(settings)
        prefs.latestAnnouncementBatch(catalog) shouldBe catalog
        prefs.markAnnouncementSeen("math")
        val recreated = AlarmPreferencesImpl(settings)
        recreated.latestAnnouncementBatch(catalog) shouldBe catalog
        recreated.markAnnouncementSeen("skip")
        recreated.latestAnnouncementBatch(catalog) shouldBe catalog
        val restarted = AlarmPreferencesImpl(settings)
        restarted.latestAnnouncementBatch(catalog) shouldBe catalog
    }

    @Test
    fun `catalog without batch history and all features acknowledged replays only the latest feature`() {
        val prefs = AlarmPreferencesImpl(MapSettings())
        prefs.markAnnouncementSeen("math")
        prefs.markAnnouncementSeen("skip")
        prefs.latestAnnouncementBatch(listOf("math", "skip")) shouldBe listOf("skip")
    }

    @Test
    fun `later update keeps still unread older features in its batch`() {
        val prefs = AlarmPreferencesImpl(MapSettings())
        prefs.latestAnnouncementBatch(listOf("math", "skip")) shouldBe listOf("math", "skip")
        prefs.markAnnouncementSeen("math")
        prefs.latestAnnouncementBatch(listOf("math", "skip", "future")) shouldBe listOf("skip", "future")
    }

}
