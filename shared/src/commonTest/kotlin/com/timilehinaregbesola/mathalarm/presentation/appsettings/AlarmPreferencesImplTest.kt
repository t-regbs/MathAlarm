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
}
