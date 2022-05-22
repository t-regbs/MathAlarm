package com.timilehinaregbesola.mathalarm.framework.database

import com.timilehinaregbesola.mathalarm.domain.model.AppThemeOptions
import com.timilehinaregbesola.mathalarm.framework.DataStoreThemeOptions

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
    fun toDataStore(appThemeOptions: AppThemeOptions): DataStoreThemeOptions =
        when (appThemeOptions) {
            AppThemeOptions.LIGHT -> DataStoreThemeOptions.LIGHT
            AppThemeOptions.DARK -> DataStoreThemeOptions.DARK
            AppThemeOptions.SYSTEM -> DataStoreThemeOptions.SYSTEM
        }

    /**
     * Maps AppThemeOptions from DataStore to Repo.
     *
     * @param appThemeOptions the object to be converted
     *
     * @return the converted object
     */
    fun toRepo(appThemeOptions: DataStoreThemeOptions): AppThemeOptions =
        when (appThemeOptions) {
            DataStoreThemeOptions.LIGHT -> AppThemeOptions.LIGHT
            DataStoreThemeOptions.DARK -> AppThemeOptions.DARK
            DataStoreThemeOptions.SYSTEM -> AppThemeOptions.SYSTEM
        }
}
