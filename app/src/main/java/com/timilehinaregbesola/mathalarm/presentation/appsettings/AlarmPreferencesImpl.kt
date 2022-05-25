package com.timilehinaregbesola.mathalarm.presentation.appsettings

import android.content.Context
import android.content.SharedPreferences
import androidx.core.content.edit
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import com.timilehinaregbesola.mathalarm.R
import com.timilehinaregbesola.mathalarm.presentation.appsettings.AlarmPreferences.Theme
import com.timilehinaregbesola.mathalarm.utils.dataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.runBlocking
import timber.log.Timber
import java.io.IOException
import javax.inject.Inject

class AlarmPreferencesImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val mapper: AppThemeOptionsMapper
) : AlarmPreferences {
    private val defaultThemeValue = context.getString(R.string.pref_theme_default_value)

    private val preferenceKeyChangedFlow = MutableSharedFlow<String>(extraBufferCapacity = 1)

    private val listener = SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
        preferenceKeyChangedFlow.tryEmit(key)
    }

    companion object {
        const val KEY_THEME = "pref_theme"
        val APP_THEME_OPTION = intPreferencesKey("mathalarm_theme_option")
    }

//    fun observeTheme(): Flow<Theme> {
//        return preferenceKeyChangedFlow
//            // Emit on start so that we always send the initial value
//            .onStart { emit(KEY_THEME) }
//            .filter { it == KEY_THEME }
//            .map { theme }
//            .distinctUntilChanged()
//    }

    override suspend fun updateAppTheme(theme: Theme) {
        context.dataStore.edit { settings ->
            settings[APP_THEME_OPTION] = mapper.toDataStore(theme).id
        }
    }

    override fun getInitial() = runBlocking {
        context.dataStore.data.first()[APP_THEME_OPTION] ?: DataStoreTheme.SYSTEM.id
    }

    override fun loadAppTheme(): Flow<Theme> {
        return context.dataStore.data.catch { exception ->
            if (exception is IOException) {
                Timber.e("Error reading preferences: ", exception)
                emit(emptyPreferences())
            } else {
                throw exception
            }
        }.map { preferences ->
            val id = preferences[APP_THEME_OPTION] ?: DataStoreTheme.SYSTEM.id
            val result = DataStoreTheme.values().find { it.id == id } ?: DataStoreTheme.SYSTEM
            mapper.toRepo(result)
        }
    }
}
