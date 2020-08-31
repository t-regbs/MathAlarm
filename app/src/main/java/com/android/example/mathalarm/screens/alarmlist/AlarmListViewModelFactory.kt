package com.android.example.mathalarm.screens.alarmlist

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.android.example.mathalarm.database.AlarmDao

//class AlarmListViewModelFactory(
//    private val dataSource: AlarmDao): ViewModelProvider.Factory {
//    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
//        if (modelClass.isAssignableFrom(AlarmListViewModel::class.java)){
//            @Suppress("UNCHECKED_CAST")
//            return AlarmListViewModel(dataSource) as T
//        }
//        throw IllegalArgumentException("Unknown ViewModel Class")
//    }
//}