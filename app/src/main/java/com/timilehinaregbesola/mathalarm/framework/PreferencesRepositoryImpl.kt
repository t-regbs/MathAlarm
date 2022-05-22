package com.timilehinaregbesola.mathalarm.framework

import com.timilehinaregbesola.mathalarm.data.datasource.PreferencesDataSource
import com.timilehinaregbesola.mathalarm.domain.model.AppThemeOptions
import com.timilehinaregbesola.mathalarm.framework.database.AppThemeOptionsMapper
import com.timilehinaregbesola.mathalarm.usecases.preferences.PreferencesRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal class PreferencesRepositoryImpl(
    private val dataSource: PreferencesDataSource,
    private val mapper: AppThemeOptionsMapper
) : PreferencesRepository {

    override suspend fun updateAppTheme(theme: AppThemeOptions) =
        dataSource.updateAppTheme(theme)

    override fun loadAppTheme(): Flow<AppThemeOptions> =
        dataSource.loadAppTheme().map { it }
}
