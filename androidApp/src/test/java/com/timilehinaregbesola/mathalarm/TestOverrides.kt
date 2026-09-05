package com.timilehinaregbesola.mathalarm

import androidx.room.Room
import com.timilehinaregbesola.mathalarm.coroutines.AppCoroutineScope
import com.timilehinaregbesola.mathalarm.fake.DateTimeProviderFake
import com.timilehinaregbesola.mathalarm.framework.database.AlarmDatabase
import com.timilehinaregbesola.mathalarm.provider.DateTimeProvider
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestCoroutineScheduler
import org.koin.android.ext.koin.androidContext
import org.koin.dsl.module

val testModule = module {
    single { TestCoroutineScheduler() }
    single { StandardTestDispatcher(get<TestCoroutineScheduler>()) }
    single { AppCoroutineScope(get<kotlinx.coroutines.test.TestDispatcher>()) }
    single<DateTimeProvider> { DateTimeProviderFake() }
    single<AlarmDatabase> {
        Room.inMemoryDatabaseBuilder(androidContext(), AlarmDatabase::class.java)
            .setQueryCoroutineContext(get<kotlinx.coroutines.test.TestDispatcher>())
            .build()
    }
}
