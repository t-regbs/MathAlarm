package com.timilehinaregbesola.mathalarm.utils

import androidx.navigation.NavBackStackEntry
import androidx.navigation3.runtime.NavKey
import kotlinx.serialization.Serializable

fun NavBackStackEntry.getAlarmIdArgument(key: String) =
    arguments?.getLong(Destinations.AlarmMath.ARGUMENT_KEY)?.let {
        it
    } ?: error("$key not provided")

// Type-safe navigation destinations using serializable classes
sealed interface Destinations : NavKey {

    @Serializable
    data object AlarmList : Destinations

    @Serializable
    data object AppSettings : Destinations

    @Serializable
    data class AlarmMath(val alarmJson: String, val fromSheet: Boolean = false) : Destinations {
        companion object {
            const val ARGUMENT_KEY = "alarmId"
            private const val BASE_URI = "https://timilehinaregbesola.com"
            const val DEEP_LINK_URI = "$BASE_URI/{$ARGUMENT_KEY}"
        }
    }

    @Serializable
    data class SettingsSheet(val settingsAlarm: String, val isTest: Boolean = false) : Destinations {
        companion object {
            const val ARGUMENT_KEY = "settingsAlarm"
        }
    }
}
