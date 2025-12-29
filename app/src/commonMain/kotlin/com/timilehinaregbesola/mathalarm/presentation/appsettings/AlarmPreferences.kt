package com.timilehinaregbesola.mathalarm.presentation.appsettings

interface AlarmPreferences {

    /**
     * Updates the current app theme.
     *
     * @param theme the theme to be updated
     */
    fun updateAppTheme(theme: Theme)

    /**
     * Loads the current app theme.
     *
     * @return flow of [Theme]
     */
    fun loadAppTheme(): Theme

    enum class Theme {
        LIGHT,
        DARK,
        SYSTEM
    }

}
