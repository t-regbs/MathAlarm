package com.android.example.mathalarm.screens

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.android.example.mathalarm.database.AlarmDao

class AlarmListViewModelFactory(
    private val dataSource: AlarmDao,
    private val application: Application): ViewModelProvider.Factory {
    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(AlarmListViewModel::class.java)){
            return AlarmListViewModel(dataSource, application) as T
        }
        throw IllegalArgumentException("Unknown ViewModel Class")
    }
}