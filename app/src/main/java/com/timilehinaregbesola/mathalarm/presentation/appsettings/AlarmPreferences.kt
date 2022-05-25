package com.timilehinaregbesola.mathalarm.presentation.appsettings

import kotlinx.coroutines.flow.Flow

interface AlarmPreferences {

    /**
     * Updates the current app theme.
     *
     * @param theme the theme to be updated
     */
    suspend fun updateAppTheme(theme: Theme)

    /**
     * Loads the current app theme.
     *
     * @return flow of [Theme]
     */
    fun loadAppTheme(): Flow<Theme>

    enum class Theme {
        LIGHT,
        DARK,
        SYSTEM
    }

    fun getInitial(): Int
}
