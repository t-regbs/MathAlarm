package com.timilehinaregbesola.mathalarm.presentation.whatsnew

import co.touchlab.kermit.Logger
import com.russhwolf.settings.MapSettings
import com.timilehinaregbesola.mathalarm.presentation.appsettings.AlarmPreferencesImpl
import com.timilehinaregbesola.mathalarm.presentation.appsettings.AppThemeOptionsMapper
import kotlin.test.Test
import kotlin.test.assertEquals

class AnnouncementFeatureTest {
    private fun preferences(settings: MapSettings = MapSettings()) =
        AlarmPreferencesImpl(AppThemeOptionsMapper(), Logger.withTag("test"), settings)

    @Test
    fun skippingReleasesIncludesAllFeaturesInReleaseOrder() {
        assertEquals(listOf(AnnouncementFeature.MATH_CHALLENGES, AnnouncementFeature.SKIP_NEXT, AnnouncementFeature.SNOOZE_SETTINGS),
            announcementFeaturesToShow(preferences()::hasSeenAnnouncement))
    }

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

    @Test
    fun existingUsersSeeSnoozeSettingsAfterEarlierFeaturesWereSeen() {
        val prefs = preferences()
        prefs.markAnnouncementSeen(AnnouncementFeature.MATH_CHALLENGES.id)
        prefs.markAnnouncementSeen(AnnouncementFeature.SKIP_NEXT.id)
        assertEquals(listOf(AnnouncementFeature.SNOOZE_SETTINGS),
            announcementFeaturesToShow(prefs::hasSeenAnnouncement))
        assertEquals(listOf(AnnouncementFeature.SNOOZE_SETTINGS.id),
            prefs.latestAnnouncementBatch(AnnouncementFeature.entries.map { it.id }))
    }

    @Test
    fun finishingAllFeaturesSuppressesAutomaticDisplayButKeepsTheUpdateBatch() {
        val prefs = preferences()
        val catalogIds = AnnouncementFeature.entries.map { it.id }
        val batch = prefs.latestAnnouncementBatch(catalogIds)
        AnnouncementFeature.entries.forEach { prefs.markAnnouncementSeen(it.id) }
        assertEquals(emptyList(), announcementFeaturesToShow(prefs::hasSeenAnnouncement))
        assertEquals(batch, prefs.latestAnnouncementBatch(catalogIds))
    }

    @Test
    fun partialAcknowledgementLeavesUnreadPagesEligibleAfterRestart() {
        val settings = MapSettings()
        val prefs = preferences(settings)
        val session = announcementFeaturesToShow(prefs::hasSeenAnnouncement)
        prefs.markAnnouncementSeen(session.first().id)
        assertEquals(3, session.size)
        assertEquals(listOf(AnnouncementFeature.SKIP_NEXT, AnnouncementFeature.SNOOZE_SETTINGS),
            announcementFeaturesToShow(preferences(settings)::hasSeenAnnouncement))
    }
}
