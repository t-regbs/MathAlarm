package com.timilehinaregbesola.mathalarm.framework.app

import android.app.Application
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.ui.ExperimentalComposeUiApi
import co.touchlab.kermit.Logger
import com.google.firebase.messaging.FirebaseMessaging
import com.timilehinaregbesola.mathalarm.framework.app.di.appModule
import kotlinx.coroutines.InternalCoroutinesApi
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

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
        Logger.setTag("MathAlarm")
        FirebaseMessaging.getInstance().subscribeToTopic("all")
    }
}
