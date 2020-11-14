package com.timilehinaregbesola.mathalarm.app.di

import android.app.Application
import androidx.room.Room
import com.timilehinaregbesola.mathalarm.database.AlarmDao
import com.timilehinaregbesola.mathalarm.database.AlarmDatabase
import com.timilehinaregbesola.mathalarm.database.AlarmRepository
import com.timilehinaregbesola.mathalarm.screens.alarmlist.AlarmListViewModel
import com.timilehinaregbesola.mathalarm.screens.alarmmath.AlarmMathViewModel
import com.timilehinaregbesola.mathalarm.screens.alarmsettings.AlarmSettingsViewModel
import org.koin.android.ext.koin.androidApplication
import org.koin.androidx.viewmodel.dsl.viewModel
import org.koin.dsl.module

val viewModelModule = module {
    viewModel { AlarmListViewModel(get()) }
    viewModel { AlarmSettingsViewModel(get()) }
    viewModel { AlarmMathViewModel(get()) }
}

val databaseModule = module {
    fun provideDatabase(application: Application): AlarmDatabase {
        return Room.databaseBuilder(
            application,
            AlarmDatabase::class.java,
            "alarm_history_database"
        ).build()
    }

    fun provideAlarmDao(database: AlarmDatabase): AlarmDao {
        return database.alarmDatabaseDao
    }

    single { provideDatabase(androidApplication()) }
    single { provideAlarmDao(get()) }
}

val repositoryModule = module {
    fun provideRepository(alarmDao: AlarmDao): AlarmRepository {
        return AlarmRepository(alarmDao)
    }

    single { provideRepository(get()) }
}
