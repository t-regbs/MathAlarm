package com.timilehinaregbesola.mathalarm

import androidx.room.Room
import android.content.Context
import com.russhwolf.settings.Settings
import com.russhwolf.settings.SharedPreferencesSettings
import com.timilehinaregbesola.mathalarm.coroutines.AppCoroutineScope
import com.timilehinaregbesola.mathalarm.fake.DateTimeProviderFake
import com.timilehinaregbesola.mathalarm.framework.database.AlarmDatabase
import com.timilehinaregbesola.mathalarm.provider.DateTimeProvider
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val testModule = module {
    // Robolectric does not run the AndroidX initializer used by Settings().
    single<Settings> {
        SharedPreferencesSettings(androidContext().getSharedPreferences("test_settings", Context.MODE_PRIVATE))
    }
    single { TestCoroutineScheduler() }
    single { StandardTestDispatcher(get<TestCoroutineScheduler>()) }
    single { AppCoroutineScope(get<TestDispatcher>()) }
    single<DateTimeProvider> { DateTimeProviderFake() }
    single<AlarmDatabase> {
        Room.inMemoryDatabaseBuilder(androidContext(), AlarmDatabase::class.java)
            .setQueryCoroutineContext(get<TestDispatcher>())
            .build()
    }
}
