package com.timilehinaregbesola.mathalarm.framework.app

import android.app.Application
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.ui.ExperimentalComposeUiApi
import co.touchlab.kermit.Logger
import com.google.firebase.messaging.FirebaseMessaging
import com.russhwolf.settings.Settings
import com.timilehinaregbesola.mathalarm.framework.app.di.appModule
import com.timilehinaregbesola.mathalarm.platform.applyPlatformNightMode
import com.timilehinaregbesola.mathalarm.presentation.appsettings.AlarmPreferences
import com.timilehinaregbesola.mathalarm.presentation.appsettings.AlarmPreferencesImpl
import com.timilehinaregbesola.mathalarm.presentation.appsettings.DataStoreTheme
import kotlinx.coroutines.InternalCoroutinesApi
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.startKoin

class AlarmApplication : Application() {
    @OptIn(ExperimentalAnimationApi::class, ExperimentalComposeUiApi::class,
        ExperimentalFoundationApi::class, InternalCoroutinesApi::class
    )
    override fun onCreate() {
        super.onCreate()
        applySavedNightMode()
        startKoin {
            androidContext(this@AlarmApplication)
            modules(appModule)
        }
        Logger.setTag("MathAlarm")
        FirebaseMessaging.getInstance().subscribeToTopic("all")
    }

    private fun applySavedNightMode() {
        val themeId = Settings().getInt(
            AlarmPreferencesImpl.APP_THEME_OPTION,
            DataStoreTheme.SYSTEM.id
        )
        val theme = when (themeId) {
            DataStoreTheme.LIGHT.id -> AlarmPreferences.Theme.LIGHT
            DataStoreTheme.DARK.id -> AlarmPreferences.Theme.DARK
            else -> AlarmPreferences.Theme.SYSTEM
        }
        applyPlatformNightMode(this, theme)
    }
}
