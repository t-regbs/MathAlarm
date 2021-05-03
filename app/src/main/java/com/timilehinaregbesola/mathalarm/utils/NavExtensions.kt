package com.timilehinaregbesola.mathalarm.utils

import androidx.navigation.NavBackStackEntry

fun NavBackStackEntry.getAlarmIdArgument(key: String) =
    arguments?.getLong(Navigation.NAV_ALARM_MATH_ARGUMENT)?.let {
        it
    } ?: error("$key not provided")
