package com.timilehinaregbesola.mathalarm.presentation.appsettings

import com.rickclephas.kmp.nativecoroutines.NativeCoroutinesState
import com.rickclephas.kmp.observableviewmodel.MutableStateFlow
import com.rickclephas.kmp.observableviewmodel.coroutineScope
import com.timilehinaregbesola.mathalarm.presentation.SharedFeatureViewModel
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AppSettingsState(
    val theme: AlarmPreferences.Theme,
    val sortOrder: AlarmPreferences.AlarmSortOrder,
    val announcementIds: List<String>,
    val seenAnnouncementIds: Set<String>,
    val failures: List<AppSettingsFailure> = emptyList(),
    val supportedAnnouncementIds: List<String> = announcementIds,
) {
    val unseenAnnouncementIds: List<String> get() = supportedAnnouncementIds.filterNot { it in seenAnnouncementIds }
}

enum class AppSettingsOperation { THEME, SORT_ORDER, ANNOUNCEMENT }
data class AppSettingsFailure(val id: Long, val operation: AppSettingsOperation)

/** Shared preference behavior; platform views own labels, sharing and external presentation. */
class AppSettingsViewModel internal constructor(
    private val preferences: AlarmPreferences,
    catalogIds: List<String> = listOf("math-challenges-v1", "skip-next-alarm-v1", "snooze-settings-v1"),
) : SharedFeatureViewModel() {
    private var nextFailureId = 0L
    private val supportedAnnouncementIds = catalogIds.distinct()
    private var initialFailure: AppSettingsFailure? = null
    private var announcementIds = try {
        preferences.latestAnnouncementBatch(supportedAnnouncementIds)
    } catch (_: Exception) {
        // Announcement bookkeeping must never prevent a delivered alarm from rendering.
        initialFailure = AppSettingsFailure(++nextFailureId, AppSettingsOperation.ANNOUNCEMENT)
        supportedAnnouncementIds
    }
    private val mutableState = MutableStateFlow(viewModelScope, snapshot().copy(failures = listOfNotNull(initialFailure)))
    @NativeCoroutinesState
    val state: StateFlow<AppSettingsState> = mutableState.asStateFlow()

    init {
        // Application preferences keep running without observers; only this owner's
        // projection is cancelled when the settings screen closes.
        viewModelScope.coroutineScope.launch {
            combine(preferences.themeState, preferences.alarmSortOrderState, preferences.seenAnnouncementIds) { theme, sort, seen ->
                AppSettingsState(theme, sort, announcementIds, seen.toSet(), supportedAnnouncementIds = supportedAnnouncementIds)
            }.collect { observed ->
                mutableState.update { observed.copy(failures = it.failures) }
            }
        }
    }

    fun selectTheme(theme: AlarmPreferences.Theme) {
        if (isClosed) return
        applyPreference(AppSettingsOperation.THEME) { preferences.updateAppTheme(theme) }
    }

    fun selectSortOrder(sortOrder: AlarmPreferences.AlarmSortOrder) {
        if (isClosed) return
        applyPreference(AppSettingsOperation.SORT_ORDER) { preferences.updateAlarmSortOrder(sortOrder) }
    }

    fun acknowledgeAnnouncement(id: String) {
        if (isClosed || id !in supportedAnnouncementIds) return
        applyPreference(AppSettingsOperation.ANNOUNCEMENT) { preferences.markAnnouncementSeen(id) }
    }

    fun refreshAnnouncements() {
        if (isClosed) return
        applyPreference(AppSettingsOperation.ANNOUNCEMENT) {
            announcementIds = preferences.latestAnnouncementBatch(supportedAnnouncementIds)
        }
    }

    fun acknowledgeFailure(id: Long) {
        mutableState.update { it.copy(failures = it.failures.filterNot { failure -> failure.id == id }) }
    }

    private fun applyPreference(operation: AppSettingsOperation, action: () -> Unit) {
        try {
            action()
            mutableState.update { snapshot().copy(failures = it.failures) }
        } catch (_: Exception) {
            val failure = AppSettingsFailure(++nextFailureId, operation)
            mutableState.update { it.copy(failures = it.failures + failure) }
        }
    }

    private fun snapshot() = AppSettingsState(preferences.themeState.value,
        preferences.alarmSortOrderState.value, announcementIds, preferences.seenAnnouncementIds.value.toSet(),
        supportedAnnouncementIds = supportedAnnouncementIds)
}
