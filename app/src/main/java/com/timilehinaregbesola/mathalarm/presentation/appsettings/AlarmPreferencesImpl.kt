package com.timilehinaregbesola.mathalarm.presentation.appsettings

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.core.intPreferencesKey
import com.timilehinaregbesola.mathalarm.presentation.appsettings.AlarmPreferences.Theme
import com.timilehinaregbesola.mathalarm.utils.dataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.runBlocking
import timber.log.Timber
import java.io.IOException

class AlarmPreferencesImpl(
    private val context: Context,
    private val mapper: AppThemeOptionsMapper
) : AlarmPreferences {
    companion object {
        val APP_THEME_OPTION = intPreferencesKey("mathalarm_theme_option")
    }

    override suspend fun updateAppTheme(theme: Theme) {
        context.dataStore.edit { settings ->
            settings[APP_THEME_OPTION] = mapper.toDataStore(theme).id
        }
    }

    override fun getInitialTheme() = runBlocking {
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
            val result = DataStoreTheme.entries.find { it.id == id } ?: DataStoreTheme.SYSTEM
            mapper.toRepo(result)
        }
    }
}
