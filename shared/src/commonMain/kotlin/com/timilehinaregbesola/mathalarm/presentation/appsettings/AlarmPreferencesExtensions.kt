package com.timilehinaregbesola.mathalarm.presentation.appsettings

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue

/**
 * Returns whether dark colors should be used based on the current theme preference.
 * This is reactive - when the theme is changed via [AlarmPreferencesImpl.updateAppTheme],
 * any composable reading this value will recompose.
 */
@Composable
fun AlarmPreferencesImpl.shouldUseDarkColors(): Boolean {
    val themePreference by themeState.collectAsState()
    return when (themePreference) {
        AlarmPreferences.Theme.LIGHT -> false
        AlarmPreferences.Theme.DARK -> true
        else -> isSystemInDarkTheme()
    }
}
