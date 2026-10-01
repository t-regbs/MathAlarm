package com.timilehinaregbesola.mathalarm.presentation.appsettings

import com.russhwolf.settings.Settings

internal actual fun migrateAlarmPreferences(settings: Settings) {
    val oldKey = "mathalarm_last_announcement"
    val recordedFeature = settings.getString(oldKey, "")
    if (recordedFeature.isNotEmpty()) {
        settings.putBoolean(AlarmPreferencesImpl.SEEN_ANNOUNCEMENT_PREFIX + recordedFeature, true)
        settings.remove(oldKey)
    }
}
