package com.timilehinaregbesola.mathalarm.presentation.appsettings

import co.touchlab.kermit.Logger
import com.russhwolf.settings.MapSettings
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class AlarmPreferencesImplTest {

    @Test
    fun `alarm sort order persists across preference recreation`() {
        val settings = MapSettings()
        val logger = Logger.withTag("AlarmPreferencesImplTest")

        AlarmPreferencesImpl(
            mapper = AppThemeOptionsMapper(),
            logger = logger,
            settings = settings
        ).updateAlarmSortOrder(AlarmPreferences.AlarmSortOrder.TIME)

        val recreatedPreferences = AlarmPreferencesImpl(
            mapper = AppThemeOptionsMapper(),
            logger = logger,
            settings = settings
        )

        recreatedPreferences.loadAlarmSortOrder() shouldBe AlarmPreferences.AlarmSortOrder.TIME
    }

    @Test
    fun `acknowledged announcement stays dismissed after restart`() {
        val settings = MapSettings()
        val preferences = AlarmPreferencesImpl(AppThemeOptionsMapper(), Logger.withTag("test"), settings)
        preferences.hasSeenAnnouncement("math-challenges-v1") shouldBe false
        preferences.markAnnouncementSeen("math-challenges-v1")
        preferences.hasSeenAnnouncement("math-challenges-v1") shouldBe true

        val recreated = AlarmPreferencesImpl(AppThemeOptionsMapper(), Logger.withTag("test"), settings)
        recreated.hasSeenAnnouncement("math-challenges-v1") shouldBe true
    }

    @Test
    fun `new feature release is eligible after previous announcement was acknowledged`() {
        val preferences = AlarmPreferencesImpl(AppThemeOptionsMapper(), Logger.withTag("test"), MapSettings())
        preferences.markAnnouncementSeen("math-challenges-v1")
        preferences.hasSeenAnnouncement("next-feature-v1") shouldBe false
        preferences.markAnnouncementSeen("next-feature-v1")
        preferences.hasSeenAnnouncement("next-feature-v1") shouldBe true
    }
    @Test
    fun `seen features accumulate and survive recreation`() {
        val settings = MapSettings()
        val preferences = AlarmPreferencesImpl(AppThemeOptionsMapper(), Logger.withTag("test"), settings)
        preferences.markAnnouncementSeen("math-challenges-v1")
        preferences.markAnnouncementSeen("skip-next-alarm-v1")
        preferences.markAnnouncementSeen("math-challenges-v1")

        val recreated = AlarmPreferencesImpl(AppThemeOptionsMapper(), Logger.withTag("test"), settings)
        recreated.hasSeenAnnouncement("math-challenges-v1") shouldBe true
        recreated.hasSeenAnnouncement("skip-next-alarm-v1") shouldBe true
        recreated.hasSeenAnnouncement("future-feature") shouldBe false
    }

    @Test
    fun `legacy migration marks only the recorded feature and persists the result`() {
        for (legacyId in listOf("math-challenges-v1", "skip-next-alarm-v1")) {
            val settings = MapSettings().apply { putString("mathalarm_last_announcement", legacyId) }
            val preferences = AlarmPreferencesImpl(AppThemeOptionsMapper(), Logger.withTag("test"), settings)
            preferences.hasSeenAnnouncement(legacyId) shouldBe true
            val other = if (legacyId == "math-challenges-v1") "skip-next-alarm-v1" else "math-challenges-v1"
            preferences.hasSeenAnnouncement(other) shouldBe false
            settings.hasKey("mathalarm_last_announcement") shouldBe false

            val recreated = AlarmPreferencesImpl(AppThemeOptionsMapper(), Logger.withTag("test"), settings)
            recreated.hasSeenAnnouncement(legacyId) shouldBe true
            recreated.hasSeenAnnouncement(other) shouldBe false
        }
    }

    @Test
    fun `migration preserves features already stored by the new system`() {
        val settings = MapSettings().apply {
            putBoolean("mathalarm_seen_announcement_math-challenges-v1", true)
            putString("mathalarm_last_announcement", "skip-next-alarm-v1")
        }
        val preferences = AlarmPreferencesImpl(AppThemeOptionsMapper(), Logger.withTag("test"), settings)
        preferences.hasSeenAnnouncement("math-challenges-v1") shouldBe true
        preferences.hasSeenAnnouncement("skip-next-alarm-v1") shouldBe true
    }

    @Test
    fun `sequential updates replace replay batch with only new features`() {
        val settings = MapSettings()
        val prefs = AlarmPreferencesImpl(AppThemeOptionsMapper(), Logger.withTag("test"), settings)
        prefs.latestAnnouncementBatch(listOf("math")) shouldBe listOf("math")
        prefs.markAnnouncementSeen("math")
        prefs.latestAnnouncementBatch(listOf("math", "skip")) shouldBe listOf("skip")
        prefs.markAnnouncementSeen("skip")
        prefs.latestAnnouncementBatch(listOf("math", "skip", "future")) shouldBe listOf("future")
    }

    @Test
    fun `skipped releases include all missed features but exclude older acknowledged ones`() {
        val prefs = AlarmPreferencesImpl(AppThemeOptionsMapper(), Logger.withTag("test"), MapSettings())
        prefs.markAnnouncementSeen("old")
        prefs.latestAnnouncementBatch(listOf("old", "math", "skip")) shouldBe listOf("math", "skip")
    }

    @Test
    fun `update replay survives partial acknowledgement completion and restart`() {
        val settings = MapSettings()
        val catalog = listOf("math", "skip")
        val prefs = AlarmPreferencesImpl(AppThemeOptionsMapper(), Logger.withTag("test"), settings)
        prefs.latestAnnouncementBatch(catalog) shouldBe catalog
        prefs.markAnnouncementSeen("math")
        val recreated = AlarmPreferencesImpl(AppThemeOptionsMapper(), Logger.withTag("test"), settings)
        recreated.latestAnnouncementBatch(catalog) shouldBe catalog
        recreated.markAnnouncementSeen("skip")
        recreated.latestAnnouncementBatch(catalog) shouldBe catalog
        val restarted = AlarmPreferencesImpl(AppThemeOptionsMapper(), Logger.withTag("test"), settings)
        restarted.latestAnnouncementBatch(catalog) shouldBe catalog
    }

    @Test
    fun `migration with all features acknowledged replays only the latest feature`() {
        val prefs = AlarmPreferencesImpl(AppThemeOptionsMapper(), Logger.withTag("test"), MapSettings())
        prefs.markAnnouncementSeen("math")
        prefs.markAnnouncementSeen("skip")
        prefs.latestAnnouncementBatch(listOf("math", "skip")) shouldBe listOf("skip")
    }

    @Test
    fun `later update keeps still unread older features in its batch`() {
        val prefs = AlarmPreferencesImpl(AppThemeOptionsMapper(), Logger.withTag("test"), MapSettings())
        prefs.latestAnnouncementBatch(listOf("math", "skip")) shouldBe listOf("math", "skip")
        prefs.markAnnouncementSeen("math")
        prefs.latestAnnouncementBatch(listOf("math", "skip", "future")) shouldBe listOf("skip", "future")
    }

}
