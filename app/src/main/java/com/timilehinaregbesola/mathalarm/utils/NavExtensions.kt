package com.timilehinaregbesola.mathalarm.utils

import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

sealed interface Destinations : NavKey {

    @Serializable
    data object AlarmList : Destinations

    @Serializable
    data object AppSettings : Destinations

    @Serializable
    data class AlarmMath(val alarmJson: String, val fromSheet: Boolean = false) : Destinations

    @Serializable
    data class SettingsSheet(val settingsAlarm: String, val isTest: Boolean = false) : Destinations
}
