package com.timilehinaregbesola.mathalarm.app

import android.app.Application
import com.timilehinaregbesola.mathalarm.app.di.databaseModule
import com.timilehinaregbesola.mathalarm.app.di.repositoryModule
import com.timilehinaregbesola.mathalarm.app.di.viewModelModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level
import timber.log.Timber

class AlarmApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Timber.plant(Timber.DebugTree())
        startKoin {
            androidLogger(Level.ERROR)
            androidContext(this@AlarmApplication)
            modules(listOf(databaseModule, repositoryModule, viewModelModule))
        }
    }
}