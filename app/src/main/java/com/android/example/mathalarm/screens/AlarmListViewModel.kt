package com.android.example.mathalarm.screens

import android.app.Application
import androidx.lifecycle.ViewModel
import com.android.example.mathalarm.database.AlarmDao
import kotlinx.coroutines.Job

class AlarmListViewModel(
    dataSource: AlarmDao,
    application: Application): ViewModel(){

    val database = dataSource

    private var viewModelJob = Job()
}