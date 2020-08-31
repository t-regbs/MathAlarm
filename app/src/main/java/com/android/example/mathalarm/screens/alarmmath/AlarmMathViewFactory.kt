package com.android.example.mathalarm.screens.alarmmath
//
//import androidx.lifecycle.ViewModel
//import androidx.lifecycle.ViewModelProvider
//import com.android.example.mathalarm.database.AlarmDao
//import com.android.example.mathalarm.screens.alarmsettings.AlarmSettingsViewModel
//
//class AlarmMathViewFactory(
//    private val alarmKey: Long,
//    private val dataSource: AlarmDao
//): ViewModelProvider.Factory {
//    override fun <T : ViewModel?> create(modelClass: Class<T>): T {
//        if (modelClass.isAssignableFrom(AlarmMathViewModel::class.java)){
//            return AlarmMathViewModel(alarmKey, dataSource) as T
//        }
//        throw IllegalArgumentException("Unknown ViewModel Class")
//    }
//}