package com.timilehinaregbesola.mathalarm.presentation.appsettings

import kotlinx.coroutines.flow.Flow

interface AlarmPreferences {

    fun setup()

    var theme: Theme
    fun observeTheme(): Flow<Theme>

    enum class Theme {
        LIGHT,
        DARK,
        SYSTEM
    }
}
