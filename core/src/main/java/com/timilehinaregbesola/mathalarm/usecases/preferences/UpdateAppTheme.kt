package com.timilehinaregbesola.mathalarm.usecases.preferences

import com.timilehinaregbesola.mathalarm.domain.model.AppThemeOptions

/**
 * Updates the current app theme.
 *
 * @property preferencesRepository the preferences repository
 */
class UpdateAppTheme(private val preferencesRepository: PreferencesRepository) {

    /**
     * Updates the current app theme.
     *
     * @param appTheme the theme to be updated
     */
    suspend operator fun invoke(appTheme: AppThemeOptions) =
        preferencesRepository.updateAppTheme(appTheme)
}
