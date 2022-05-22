package com.timilehinaregbesola.mathalarm.framework

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import com.timilehinaregbesola.mathalarm.data.datasource.PreferencesDataSource
import com.timilehinaregbesola.mathalarm.domain.model.AppThemeOptions
import com.timilehinaregbesola.mathalarm.framework.database.AppThemeOptionsMapper
import com.timilehinaregbesola.mathalarm.framework.database.dataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

class PreferencesDataSourceImpl(
    private val context: Context,
    private val mapper: AppThemeOptionsMapper
) : PreferencesDataSource {

    override suspend fun updateAppTheme(theme: AppThemeOptions) {
        context.dataStore.edit { settings ->
            settings[APP_THEME_OPTION] = mapper.toDataStore(theme).id
        }
    }

    override fun loadAppTheme(): Flow<AppThemeOptions> =
        context.dataStore.data.map { preferences ->
            val id = preferences[APP_THEME_OPTION] ?: DataStoreThemeOptions.SYSTEM.id
            val result = DataStoreThemeOptions.values().find { it.id == id } ?: DataStoreThemeOptions.SYSTEM
            mapper.toRepo(result)
        }

    private companion object {
        val APP_THEME_OPTION = intPreferencesKey("mathalarm_theme_option")
    }
}
