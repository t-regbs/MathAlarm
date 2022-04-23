package com.timilehinaregbesola.mathalarm.utils

import androidx.navigation.NavBackStackEntry
import com.ramcosta.composedestinations.navargs.DestinationsNavTypeSerializer
import com.ramcosta.composedestinations.navargs.NavTypeSerializer
import com.timilehinaregbesola.mathalarm.framework.database.AlarmEntity

fun NavBackStackEntry.getAlarmIdArgument(key: String) =
    arguments?.getLong(Navigation.NAV_ALARM_MATH_ARGUMENT) ?: error("$key not provided")

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

@NavTypeSerializer
class AlarmEntityNavTypeSerializer : DestinationsNavTypeSerializer<AlarmEntity> {

    override fun toRouteString(value: AlarmEntity): String {
        return "${value.alarmId};${value.hour};${value.minute};${value.repeat};${value.repeatDays};${value.isOn};${value.difficulty};${value.alarmTone};${value.vibrate};${value.snooze};${value.title};${value.isSaved}"
    }

    override fun fromRouteString(routeStr: String): AlarmEntity {
        val parts = routeStr.split(";")
        return AlarmEntity(
            parts[0].toLong(),
            parts[1].toInt(),
            parts[2].toInt(),
            parts[3].toBoolean(),
            parts[4],
            parts[5].toBoolean(),
            parts[6].toInt(),
            parts[7],
            parts[8].toBoolean(),
            parts[9].toInt(),
            parts[10],
            parts[11].toBoolean()
        )
    }
}
