package com.timilehinaregbesola.mathalarm.presentation.appsettings

/**
 * Maps AppThemeOptions between Repository and DataStore.
 */
class AppThemeOptionsMapper {

    /**
     * Maps AppThemeOptions from DataStore to Domain.
     *
     * @param appThemeOptions the object to be converted
     *
     * @return the converted object
     */
    fun toDataStore(appThemeOptions: AlarmPreferences.Theme): DataStoreTheme =
        when (appThemeOptions) {
            AlarmPreferences.Theme.LIGHT -> DataStoreTheme.LIGHT
            AlarmPreferences.Theme.DARK -> DataStoreTheme.DARK
            AlarmPreferences.Theme.SYSTEM -> DataStoreTheme.SYSTEM
        }

    /**
     * Maps AppThemeOptions from DataStore to Repo.
     *
     * @param appThemeOptions the object to be converted
     *
     * @return the converted object
     */
    fun toRepo(appThemeOptions: DataStoreTheme): AlarmPreferences.Theme =
        when (appThemeOptions) {
            DataStoreTheme.LIGHT -> AlarmPreferences.Theme.LIGHT
            DataStoreTheme.DARK -> AlarmPreferences.Theme.DARK
            DataStoreTheme.SYSTEM -> AlarmPreferences.Theme.SYSTEM
        }
}
