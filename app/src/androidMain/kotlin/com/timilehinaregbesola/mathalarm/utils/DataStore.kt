package com.timilehinaregbesola.mathalarm.utils

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore

/**
 * Extension function to return a singleton of MathAlarm DataStore settings.
 */
val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "mathalarm_settings")
