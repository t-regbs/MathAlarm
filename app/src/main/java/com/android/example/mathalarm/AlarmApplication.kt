package com.android.example.mathalarm

import android.app.Application
import timber.log.Timber

class AlarmApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Timber.plant(Timber.DebugTree())
    }
}