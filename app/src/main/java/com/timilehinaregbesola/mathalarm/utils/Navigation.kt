package com.timilehinaregbesola.mathalarm.utils

object Navigation {
    const val NAV_ALARM_LIST = "home_screen"

    const val NAV_ALARM_MATH_ARGUMENT = "alarmId"
    const val NAV_ALARM_MATH = "math_screen/{$NAV_ALARM_MATH_ARGUMENT}"

    fun buildAlarmMathPath(alarmId: Long) = "math_screen/$alarmId"
}
