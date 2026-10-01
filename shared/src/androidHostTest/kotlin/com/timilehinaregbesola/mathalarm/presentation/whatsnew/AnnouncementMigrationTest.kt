package com.timilehinaregbesola.mathalarm.presentation.whatsnew

import co.touchlab.kermit.Logger
import com.russhwolf.settings.MapSettings
import com.timilehinaregbesola.mathalarm.presentation.appsettings.AlarmPreferencesImpl
import com.timilehinaregbesola.mathalarm.presentation.appsettings.AppThemeOptionsMapper
import kotlin.test.Test
import kotlin.test.assertEquals

class AnnouncementMigrationTest {
    private fun preferences(settings: MapSettings) =
        AlarmPreferencesImpl(AppThemeOptionsMapper(), Logger.withTag("test"), settings)

    @Test
    fun oldMathAcknowledgementShowsLaterFeatures() {
        val prefs = preferences(MapSettings().apply {
            putString("mathalarm_last_announcement", "math-challenges-v1")
        })
        assertEquals(listOf(AnnouncementFeature.SKIP_NEXT, AnnouncementFeature.SNOOZE_SETTINGS), announcementFeaturesToShow(prefs::hasSeenAnnouncement))
    }

    @Test
    fun oldSkipAcknowledgementDoesNotAssumeMathWasSeen() {
        val prefs = preferences(MapSettings().apply {
            putString("mathalarm_last_announcement", "skip-next-alarm-v1")
        })
        assertEquals(listOf(AnnouncementFeature.MATH_CHALLENGES, AnnouncementFeature.SNOOZE_SETTINGS), announcementFeaturesToShow(prefs::hasSeenAnnouncement))
    }

}
