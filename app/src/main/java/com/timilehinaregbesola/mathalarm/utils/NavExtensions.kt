package com.timilehinaregbesola.mathalarm.utils

import androidx.navigation.NavBackStackEntry

fun NavBackStackEntry.getAlarmIdArgument(key: String) =
    arguments?.getLong(Navigation.NAV_ALARM_MATH_ARGUMENT)?.let {
        it
    } ?: error("$key not provided")

object Navigation {
    const val NAV_APP_SETTINGS = "app_settings_screen"
    const val NAV_ALARM_LIST = "home_screen"
    const val NAV_SETTINGS_SHEET_ARGUMENT = "settingsAlarmId"
    const val NAV_SETTINGS_SHEET = "settings_screen/{$NAV_SETTINGS_SHEET_ARGUMENT}"
    const val NAV_ALARM_MATH_ARGUMENT = "alarmId"
    const val NAV_ALARM_MATH = "math_screen/{$NAV_ALARM_MATH_ARGUMENT}"
    private const val uri = "https://timilehinaregbesola.com"
    const val NAV_ALARM_MATH_URI = "$uri/$NAV_ALARM_MATH_ARGUMENT={$NAV_ALARM_MATH_ARGUMENT}"

    fun buildAlarmMathPath(alarmId: Long) = "math_screen/$alarmId"
    fun buildSettingsPath(alarmId: Long) = "settings_screen/$alarmId"
}
