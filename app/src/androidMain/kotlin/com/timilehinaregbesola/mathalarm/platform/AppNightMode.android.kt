package com.timilehinaregbesola.mathalarm.platform

import android.app.UiModeManager
import android.content.Context
import android.os.Build
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.content.ContextCompat
import com.timilehinaregbesola.mathalarm.presentation.appsettings.AlarmPreferences
import org.koin.core.context.GlobalContext

actual fun applyPlatformNightMode(theme: AlarmPreferences.Theme) {
    applyPlatformNightMode(GlobalContext.get().get(), theme)
}

fun applyPlatformNightMode(context: Context, theme: AlarmPreferences.Theme) {
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
        val uiModeManager = ContextCompat.getSystemService(context, UiModeManager::class.java)
        when (theme) {
            AlarmPreferences.Theme.LIGHT -> uiModeManager?.setApplicationNightMode(UiModeManager.MODE_NIGHT_NO)
            AlarmPreferences.Theme.DARK -> uiModeManager?.setApplicationNightMode(UiModeManager.MODE_NIGHT_YES)
            AlarmPreferences.Theme.SYSTEM -> uiModeManager?.setApplicationNightMode(UiModeManager.MODE_NIGHT_AUTO)
        }
    }

    AppCompatDelegate.setDefaultNightMode(theme.toAppCompatNightMode())
}

fun AlarmPreferences.Theme.toAppCompatNightMode(): Int =
    when (this) {
        AlarmPreferences.Theme.LIGHT -> AppCompatDelegate.MODE_NIGHT_NO
        AlarmPreferences.Theme.DARK -> AppCompatDelegate.MODE_NIGHT_YES
        AlarmPreferences.Theme.SYSTEM -> AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM
    }
