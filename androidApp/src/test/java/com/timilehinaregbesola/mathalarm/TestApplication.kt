package com.timilehinaregbesola.mathalarm

import android.app.Application
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.ui.ExperimentalComposeUiApi
import co.touchlab.kermit.Logger
import com.timilehinaregbesola.mathalarm.coroutines.AppCoroutineScope
import com.timilehinaregbesola.mathalarm.framework.app.di.appModule
import com.timilehinaregbesola.mathalarm.framework.database.AlarmDatabase
import kotlinx.coroutines.InternalCoroutinesApi
import org.koin.android.ext.koin.androidContext
import org.koin.core.context.GlobalContext
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin

/**
 * Test Application class that initializes Koin for Robolectric tests.
 * This allows integration tests to use the real dependency injection setup.
 */
class TestApplication : Application() {
    @OptIn(
        ExperimentalAnimationApi::class,
        ExperimentalComposeUiApi::class,
        ExperimentalFoundationApi::class,
        InternalCoroutinesApi::class
    )
    override fun onCreate() {
        super.onCreate()
        
        try {
            // Stop any existing Koin instance (in case of test rerun)
            stopKoin()
        } catch (e: Exception) {
            // Ignore if Koin wasn't started
        }
        
        // Initialize Koin with the same modules as the real app
        startKoin {
            allowOverride(true)
            androidContext(this@TestApplication)
            modules(appModule, testModule)
        }
        
        Logger.setTag("MathAlarmTest")
    }
    
    override fun onTerminate() {
        val koin = GlobalContext.get()
        koin.get<AppCoroutineScope>().cancel()
        koin.get<AlarmDatabase>().close()
        stopKoin()
        super.onTerminate()
    }
}
