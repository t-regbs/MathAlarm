package com.timilehinaregbesola.mathalarm.presentation.appsettings

import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import co.touchlab.kermit.Logger
import com.russhwolf.settings.Settings
import com.timilehinaregbesola.mathalarm.presentation.appsettings.AlarmPreferences.AlarmSortOrder
import com.timilehinaregbesola.mathalarm.presentation.appsettings.AlarmPreferences.Theme

@OptIn(kotlin.experimental.ExperimentalObjCRefinement::class)
@kotlin.native.HiddenFromObjC
class AlarmPreferencesImpl(
    private val mapper: AppThemeOptionsMapper,
    private val logger: Logger,
    private val settings: Settings
) : AlarmPreferences {
    companion object {
        internal const val SEEN_ANNOUNCEMENT_PREFIX = "mathalarm_seen_announcement_"
        private const val ANNOUNCEMENT_CATALOG = "mathalarm_announcement_catalog"
        private const val ANNOUNCEMENT_BATCH = "mathalarm_announcement_batch"
        private const val ANNOUNCEMENT_ID_SEPARATOR = "\n"
        const val APP_THEME_OPTION = "mathalarm_theme_option"
        const val ALARM_SORT_ORDER_OPTION = "mathalarm_alarm_sort_order"
    }

    private val seenAnnouncements = MutableStateFlow(loadSeenAnnouncements())
    override val seenAnnouncementIds = seenAnnouncements.asStateFlow()

    override fun hasSeenAnnouncement(id: String): Boolean = id in seenAnnouncements.value

    override fun markAnnouncementSeen(id: String) {
        settings.putBoolean(SEEN_ANNOUNCEMENT_PREFIX + id, true)
        seenAnnouncements.value = seenAnnouncements.value + id
    }

    /** Freeze the relevant features for this update, even after they are acknowledged. */
    override fun latestAnnouncementBatch(catalogIds: List<String>): List<String> {
        val catalog = catalogIds.joinToString(ANNOUNCEMENT_ID_SEPARATOR)
        if (!settings.hasKey(ANNOUNCEMENT_BATCH) || settings.getString(ANNOUNCEMENT_CATALOG, "") != catalog) {
            val unseen = catalogIds.filterNot(::hasSeenAnnouncement)
            // Older installations have no batch history. If everything is seen, replay
            // just the newest feature rather than reconstructing the entire archive.
            val batch = unseen.ifEmpty { catalogIds.takeLast(1) }
            settings.putString(ANNOUNCEMENT_BATCH, batch.joinToString(ANNOUNCEMENT_ID_SEPARATOR))
            settings.putString(ANNOUNCEMENT_CATALOG, catalog)
        }
        return settings.getString(ANNOUNCEMENT_BATCH, "")
            .split(ANNOUNCEMENT_ID_SEPARATOR).filter { it in catalogIds }
    }

    private fun loadSeenAnnouncements(): Set<String> {
        migrateAlarmPreferences(settings)
        return settings.keys.filter { key ->
            key.startsWith(SEEN_ANNOUNCEMENT_PREFIX) && settings.getBoolean(key, false)
        }.map { it.removePrefix(SEEN_ANNOUNCEMENT_PREFIX) }.toSet()
    }

    private val theme = MutableStateFlow(loadAppThemeFromStorage())
    override val themeState = theme.asStateFlow()
    private val sortOrder = MutableStateFlow(loadAlarmSortOrderFromStorage())
    override val alarmSortOrderState = sortOrder.asStateFlow()

    override fun updateAppTheme(theme: Theme) {
        settings.putInt(APP_THEME_OPTION, mapper.toDataStore(theme).id)
        this.theme.value = theme
    }

    override fun loadAppTheme(): Theme = themeState.value

    override fun updateAlarmSortOrder(sortOrder: AlarmSortOrder) {
        settings.putInt(ALARM_SORT_ORDER_OPTION, sortOrder.ordinal)
        this.sortOrder.value = sortOrder
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
