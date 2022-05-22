package com.timilehinaregbesola.mathalarm.usecases.preferences

import com.timilehinaregbesola.mathalarm.domain.model.AppThemeOptions
import kotlinx.coroutines.flow.Flow

/**
 * Interface to represent the implementation of Preferences repository.
 */
interface PreferencesRepository {

    /**
     * Updates the current app theme.
     *
     * @param theme the theme to be updated
     */
    suspend fun updateAppTheme(theme: AppThemeOptions)

    /**
     * Loads the current app theme.
     *
     * @return flow of [AppThemeOptions]
     */
    fun loadAppTheme(): Flow<AppThemeOptions>
}
