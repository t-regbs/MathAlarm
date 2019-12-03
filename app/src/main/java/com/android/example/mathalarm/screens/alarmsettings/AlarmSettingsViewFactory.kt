package com.android.example.mathalarm.screens.alarmsettings

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.android.example.mathalarm.database.AlarmDao

class AlarmSettingsViewFactory(
    private val alarmKey: Long,
    private val dataSource: AlarmDao
): ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AlarmSettingsViewModel::class.java)){
            return AlarmSettingsViewModel(alarmKey, dataSource) as T
        }
        throw IllegalArgumentException("Unknown ViewModel Class")
    }
}