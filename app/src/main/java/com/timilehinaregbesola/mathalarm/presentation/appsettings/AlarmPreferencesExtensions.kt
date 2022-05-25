package com.timilehinaregbesola.mathalarm.presentation.appsettings

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState

@Composable
fun AlarmPreferences.shouldUseDarkColors(): Boolean {
    val themePreference = loadAppTheme().collectAsState(initial = getInitial())
    return when (themePreference.value) {
        AlarmPreferences.Theme.LIGHT -> false
        AlarmPreferences.Theme.DARK -> true
        else -> isSystemInDarkTheme()
    }
}
