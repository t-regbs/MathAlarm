package com.timilehinaregbesola.mathalarm.presentation.appsettings

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import com.timilehinaregbesola.mathalarm.R
import com.timilehinaregbesola.mathalarm.presentation.appsettings.AlarmPreferences.Theme
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import javax.inject.Inject
import javax.inject.Named

class AlarmPreferencesImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    @Named("app") private val sharedPreferences: SharedPreferences
) : AlarmPreferences {
    private val defaultThemeValue = context.getString(R.string.pref_theme_default_value)

    private val preferenceKeyChangedFlow = MutableSharedFlow<String>(extraBufferCapacity = 1)

    private val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        preferenceKeyChangedFlow.tryEmit(key)
    }

    companion object {
        const val KEY_THEME = "pref_theme"
    }

    override fun setup() {
        sharedPreferences.registerOnSharedPreferenceChangeListener(listener)
    }

    override var theme: Theme
        get() = getThemeForStorageValue(sharedPreferences.getString(KEY_THEME, defaultThemeValue)!!)
        set(value) = sharedPreferences.edit {
            putString(KEY_THEME, value.storageKey)
        }

    override fun observeTheme(): Flow<Theme> {
        return preferenceKeyChangedFlow
            // Emit on start so that we always send the initial value
            .onStart { emit(KEY_THEME) }
            .filter { it == KEY_THEME }
            .map { theme }
            .distinctUntilChanged()
    }

    private val Theme.storageKey: String
        get() = when (this) {
            Theme.LIGHT -> context.getString(R.string.pref_theme_light_value)
            Theme.DARK -> context.getString(R.string.pref_theme_dark_value)
            Theme.SYSTEM -> context.getString(R.string.pref_theme_system_value)
        }

    private fun getThemeForStorageValue(value: String) = when (value) {
        context.getString(R.string.pref_theme_light_value) -> Theme.LIGHT
        context.getString(R.string.pref_theme_dark_value) -> Theme.DARK
        else -> Theme.SYSTEM
    }
}
