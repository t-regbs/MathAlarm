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

    /**
     * Updates the current alarm list sort order.
     *
     * @param sortOrder the sort order to be updated
     */
    fun updateAlarmSortOrder(sortOrder: AlarmSortOrder)

    /**
     * Loads the current alarm list sort order.
     *
     * @return current [AlarmSortOrder]
     */
    fun loadAlarmSortOrder(): AlarmSortOrder

    enum class Theme {
        LIGHT,
        DARK,
        SYSTEM
    }

    enum class AlarmSortOrder {
        CREATION,
        TIME
    }

}
