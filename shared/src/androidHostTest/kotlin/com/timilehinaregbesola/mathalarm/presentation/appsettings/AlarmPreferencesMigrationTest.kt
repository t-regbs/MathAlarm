package com.timilehinaregbesola.mathalarm.presentation.appsettings

import co.touchlab.kermit.Logger
import com.russhwolf.settings.MapSettings
import io.kotest.matchers.shouldBe
import kotlin.test.Test

class AlarmPreferencesMigrationTest {
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

}
