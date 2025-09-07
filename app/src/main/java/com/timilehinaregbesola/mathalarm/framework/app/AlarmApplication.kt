package com.timilehinaregbesola.mathalarm.framework.app

import android.app.Application
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.ui.ExperimentalComposeUiApi
import com.google.firebase.messaging.FirebaseMessaging
import com.timilehinaregbesola.mathalarm.framework.app.di.appModule
import kotlinx.coroutines.InternalCoroutinesApi
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin
import timber.log.Timber

class AlarmApplication : Application() {
    @OptIn(ExperimentalAnimationApi::class, ExperimentalComposeUiApi::class,
        ExperimentalFoundationApi::class, InternalCoroutinesApi::class
    )
    override fun onCreate() {
        super.onCreate()
        startKoin {
            androidContext(this@AlarmApplication)
            modules(appModule)
        }
        Timber.plant(Timber.DebugTree())
        FirebaseMessaging.getInstance().subscribeToTopic("all")
    }
}
