package com.timilehinaregbesola.mathalarm.presentation.appsettings

import com.russhwolf.settings.Settings

/** Platforms migrate preferences from their own released storage formats. */
internal expect fun migrateAlarmPreferences(settings: Settings)
