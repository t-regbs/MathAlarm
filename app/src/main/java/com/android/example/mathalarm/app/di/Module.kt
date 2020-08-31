package com.android.example.mathalarm.app.di

import android.app.Application
import androidx.room.Room
import com.android.example.mathalarm.database.AlarmDao
import com.android.example.mathalarm.database.AlarmDatabase
import org.koin.android.ext.koin.androidApplication
import org.koin.dsl.module

val databaseModule = module {
    fun provideDatabase(application: Application): AlarmDatabase {
        return Room.databaseBuilder(application,
            AlarmDatabase::class.java,
            "alarm_history_database")
            .build()
    }

    fun provideAlarmDao(database: AlarmDatabase): AlarmDao {
        return database.alarmDatabaseDao
    }

    single { provideDatabase(androidApplication()) }
    single { provideAlarmDao(get()) }
}