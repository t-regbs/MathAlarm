package com.timilehinaregbesola.mathalarm.utils

import androidx.navigation.NavBackStackEntry

fun NavBackStackEntry.getAlarmIdArgument(key: String) =
    arguments?.getLong(Navigation.NAV_ALARM_MATH_ARGUMENT)?.let {
        it
    } ?: error("$key not provided")

object Navigation {
    const val NAV_ALARM_LIST = "home_screen"

    const val NAV_ALARM_MATH_ARGUMENT = "alarmId"
    const val NAV_ALARM_MATH = "math_screen/{$NAV_ALARM_MATH_ARGUMENT}"

    fun buildAlarmMathPath(alarmId: Long) = "math_screen/$alarmId"
}
