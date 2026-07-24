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
}
