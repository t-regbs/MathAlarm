package com.timilehinaregbesola.mathalarm.di

import com.russhwolf.settings.Settings
import com.timilehinaregbesola.mathalarm.coroutines.AppCoroutineScope
import com.timilehinaregbesola.mathalarm.data.AlarmDataSource
import com.timilehinaregbesola.mathalarm.data.AlarmRepository
import com.timilehinaregbesola.mathalarm.framework.RoomAlarmDataSource
import com.timilehinaregbesola.mathalarm.framework.Usecases
import com.timilehinaregbesola.mathalarm.framework.database.AlarmMapper
import com.timilehinaregbesola.mathalarm.presentation.alarmlist.AlarmListViewModel
import com.timilehinaregbesola.mathalarm.presentation.alarmmath.AlarmMathViewModel
import com.timilehinaregbesola.mathalarm.presentation.alarmsettings.AlarmSettingsViewModel
import com.timilehinaregbesola.mathalarm.presentation.appsettings.AlarmPreferencesImpl
import com.timilehinaregbesola.mathalarm.presentation.appsettings.AppThemeOptionsMapper
import com.timilehinaregbesola.mathalarm.provider.AlarmTimeCalculator
import com.timilehinaregbesola.mathalarm.provider.AlarmTimeCalculatorImpl
import com.timilehinaregbesola.mathalarm.provider.DateTimeProvider
import com.timilehinaregbesola.mathalarm.provider.DateTimeProviderImpl
import com.timilehinaregbesola.mathalarm.usecases.AddAlarm
import com.timilehinaregbesola.mathalarm.usecases.CancelAlarm
import com.timilehinaregbesola.mathalarm.usecases.ClearAlarms
import com.timilehinaregbesola.mathalarm.usecases.CompleteAlarm
import com.timilehinaregbesola.mathalarm.usecases.DeleteAlarm
import com.timilehinaregbesola.mathalarm.usecases.FindAlarm
import com.timilehinaregbesola.mathalarm.usecases.GetSavedAlarms
import com.timilehinaregbesola.mathalarm.usecases.RescheduleFutureAlarms
import com.timilehinaregbesola.mathalarm.usecases.ScheduleAlarm
import com.timilehinaregbesola.mathalarm.usecases.ScheduleNextAlarm
import com.timilehinaregbesola.mathalarm.usecases.ShowAlarm
import com.timilehinaregbesola.mathalarm.usecases.SnoozeAlarm
import com.timilehinaregbesola.mathalarm.usecases.UpdateAlarm
import org.koin.core.module.dsl.viewModel
import org.koin.core.parameter.parametersOf
import org.koin.core.scope.Scope
import org.koin.dsl.module

val commonModule = module {
    // Mappers
    single { AlarmMapper() }
    single { AppThemeOptionsMapper() }
    
    // Coroutine Scope - replaces GlobalScope usage
    single { AppCoroutineScope() }
    
    // DateTime Provider
    single<DateTimeProvider> { DateTimeProviderImpl() }
    
    // Alarm Time Calculator - moves time calculation to domain layer
    single<AlarmTimeCalculator> { AlarmTimeCalculatorImpl() }
    
    // Schedule Next Alarm (now depends on AlarmTimeCalculator)
    single { ScheduleNextAlarm(get(), get()) }
    
    // Data Source and Repository
    single<AlarmDataSource> { RoomAlarmDataSource(get(), get()) }
    single { AlarmRepository(get()) }
    
    // Preferences
    single { Settings() }
    single {
        AlarmPreferencesImpl(
            get<AppThemeOptionsMapper>(),
            getWith("AlarmPreferencesImpl"),
            get<Settings>()
        )
    }
    
    // Usecases
    single {
        Usecases(
            addAlarm = AddAlarm(get()),
            clearAlarms = ClearAlarms(get(), get()),
            deleteAlarm = DeleteAlarm(get(), get()),
            findAlarm = FindAlarm(get()),
            getSavedAlarms = GetSavedAlarms(get()),
            updateAlarm = UpdateAlarm(get()),
            scheduleAlarm = ScheduleAlarm(get(), get(), get()),
            completeAlarm = CompleteAlarm(get(), get(), get()),
            rescheduleFutureAlarms = RescheduleFutureAlarms(get(), get(), get(), get()),
            scheduleNextAlarm = get(),
            showAlarm = ShowAlarm(get(), get(), get()),
            snoozeAlarm = SnoozeAlarm(get(), get(), get(), get()),
            cancelAlarm = CancelAlarm(get())
        )
    }
    
    // ViewModels
    viewModel { AlarmListViewModel(get(), get(), getWith("AlarmListViewModel")) }
    viewModel { AlarmSettingsViewModel(get()) }
    viewModel { AlarmMathViewModel(get(), get(), getWith("AlarmMathViewModel")) }
}

/**
 * Helper function to get dependencies with parameters
 */
internal inline fun <reified T> Scope.getWith(vararg params: Any?): T =
    get(parameters = { parametersOf(*params) })
