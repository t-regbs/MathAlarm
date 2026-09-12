package com.timilehinaregbesola.mathalarm.presentation.appsettings

import androidx.compose.runtime.mutableStateOf
import co.touchlab.kermit.Logger
import com.russhwolf.settings.Settings
import com.timilehinaregbesola.mathalarm.presentation.appsettings.AlarmPreferences.AlarmSortOrder
import com.timilehinaregbesola.mathalarm.presentation.appsettings.AlarmPreferences.Theme

class AlarmPreferencesImpl(
    private val mapper: AppThemeOptionsMapper,
    private val logger: Logger,
    private val settings: Settings
) : AlarmPreferences {
    companion object {
        private const val LAST_ANNOUNCEMENT = "mathalarm_last_announcement"
        const val APP_THEME_OPTION = "mathalarm_theme_option"
        const val ALARM_SORT_ORDER_OPTION = "mathalarm_alarm_sort_order"
    }

    private val lastAnnouncement = mutableStateOf(settings.getString(LAST_ANNOUNCEMENT, ""))

    fun hasSeenAnnouncement(id: String): Boolean = lastAnnouncement.value == id

    fun markAnnouncementSeen(id: String) {
        settings.putString(LAST_ANNOUNCEMENT, id)
        lastAnnouncement.value = id
    }

    val themeState = mutableStateOf(loadAppThemeFromStorage())
    val alarmSortOrderState = mutableStateOf(loadAlarmSortOrderFromStorage())

    override fun updateAppTheme(theme: Theme) {
        settings.putInt(APP_THEME_OPTION, mapper.toDataStore(theme).id)
        themeState.value = theme
    }

    override fun loadAppTheme(): Theme = themeState.value

    override fun updateAlarmSortOrder(sortOrder: AlarmSortOrder) {
        settings.putInt(ALARM_SORT_ORDER_OPTION, sortOrder.ordinal)
        alarmSortOrderState.value = sortOrder
    }

    override fun loadAlarmSortOrder(): AlarmSortOrder = alarmSortOrderState.value

    private fun loadAppThemeFromStorage(): Theme {
        val id = settings.getInt(APP_THEME_OPTION, DataStoreTheme.SYSTEM.id)
        val result = DataStoreTheme.entries.find { it.id == id } ?: DataStoreTheme.SYSTEM
        return mapper.toRepo(result)
    }

    private fun loadAlarmSortOrderFromStorage(): AlarmSortOrder {
        val id = settings.getInt(ALARM_SORT_ORDER_OPTION, AlarmSortOrder.CREATION.ordinal)
        return AlarmSortOrder.entries.find { it.ordinal == id } ?: AlarmSortOrder.CREATION
    }
}
