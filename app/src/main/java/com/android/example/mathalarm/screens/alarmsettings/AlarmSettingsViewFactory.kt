package com.android.example.mathalarm.screens.alarmsettings

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.android.example.mathalarm.database.AlarmDao

class AlarmSettingsViewFactory(
    private val dataSource: AlarmDao,
    private val application: Application
): ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AlarmSettingsViewModel::class.java)){
            return AlarmSettingsViewModel(
                dataSource,
                application
            ) as T
        }
        throw IllegalArgumentException("Unknown ViewModel Class")
    }
}