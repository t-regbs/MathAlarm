package com.timilehinaregbesola.mathalarm.usecases.preferences

import com.timilehinaregbesola.mathalarm.domain.model.AppThemeOptions
import kotlinx.coroutines.flow.Flow

/**
 * Loads the current app theme.
 *
 * @property preferencesRepository the preferences repository
 */
class LoadAppTheme(private val preferencesRepository: PreferencesRepository) {

    /**
     * Loads the current app theme.
     *
     * @return flow of [AppThemeOptions]
     */
    operator fun invoke(): Flow<AppThemeOptions> =
        preferencesRepository.loadAppTheme()
}
