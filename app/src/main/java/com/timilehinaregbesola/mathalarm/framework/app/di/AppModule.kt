package com.timilehinaregbesola.mathalarm.framework.app.di

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.room.Room
import com.timilehinaregbesola.mathalarm.data.AlarmDataSource
import com.timilehinaregbesola.mathalarm.data.AlarmRepository
import com.timilehinaregbesola.mathalarm.framework.RoomAlarmDataSource
import com.timilehinaregbesola.mathalarm.framework.Usecases
import com.timilehinaregbesola.mathalarm.framework.app.permission.AlarmPermission
import com.timilehinaregbesola.mathalarm.framework.app.permission.AndroidVersion
import com.timilehinaregbesola.mathalarm.framework.app.permission.AndroidVersionImpl
import com.timilehinaregbesola.mathalarm.framework.database.AlarmDatabase
import com.timilehinaregbesola.mathalarm.framework.database.AlarmMapper
import com.timilehinaregbesola.mathalarm.framework.database.MIGRATION_2_3
import com.timilehinaregbesola.mathalarm.framework.database.MIGRATION_3_4
import com.timilehinaregbesola.mathalarm.interactors.AlarmInteractor
import com.timilehinaregbesola.mathalarm.interactors.AlarmInteractorImpl
import com.timilehinaregbesola.mathalarm.interactors.AudioPlayer
import com.timilehinaregbesola.mathalarm.interactors.NotificationInteractor
import com.timilehinaregbesola.mathalarm.interactors.NotificationInteractorImpl
import com.timilehinaregbesola.mathalarm.interactors.PlayerWrapper
import com.timilehinaregbesola.mathalarm.notification.AlarmNotificationScheduler
import com.timilehinaregbesola.mathalarm.notification.MathAlarmNotification
import com.timilehinaregbesola.mathalarm.notification.MathAlarmNotificationChannel
import com.timilehinaregbesola.mathalarm.presentation.alarmlist.AlarmListViewModel
import com.timilehinaregbesola.mathalarm.presentation.alarmmath.AlarmMathViewModel
import com.timilehinaregbesola.mathalarm.presentation.alarmsettings.AlarmSettingsViewModel
import com.timilehinaregbesola.mathalarm.presentation.appsettings.AlarmPreferencesImpl
import com.timilehinaregbesola.mathalarm.presentation.appsettings.AppThemeOptionsMapper
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
import com.timilehinaregbesola.mathalarm.utils.getAlarmManager
import kotlinx.coroutines.InternalCoroutinesApi
import org.koin.android.ext.koin.androidApplication
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.dsl.viewModel
import org.koin.dsl.module

@ExperimentalFoundationApi
@ExperimentalComposeUiApi
@InternalCoroutinesApi
@ExperimentalAnimationApi
val appModule = module {
    single {
        Room.databaseBuilder(
            androidApplication(),
            AlarmDatabase::class.java,
            "alarm_history_database"
        ).addMigrations(MIGRATION_2_3, MIGRATION_3_4).build()
    }

    single { get<AlarmDatabase>().alarmDatabaseDao }

    single { AlarmMapper() }

    single { AppThemeOptionsMapper() }

    single<DateTimeProvider> { DateTimeProviderImpl() }

    single { ScheduleNextAlarm(get()) }

    single<AlarmDataSource> { RoomAlarmDataSource(get(), get()) }

    single { AlarmRepository(get()) }

    single<AlarmInteractor> { AlarmInteractorImpl(get()) }

    single {
        Usecases(
            addAlarm = AddAlarm(get()),
            clearAlarms = ClearAlarms(get(), get()),
            deleteAlarm = DeleteAlarm(get(), get()),
            findAlarm = FindAlarm(get()),
            getSavedAlarms = GetSavedAlarms(get()),
            updateAlarm = UpdateAlarm(get()),
            scheduleAlarm = ScheduleAlarm(get(), get()),
            completeAlarm = CompleteAlarm(get(), get(), get()),
            rescheduleFutureAlarms = RescheduleFutureAlarms(get(), get()),
            scheduleNextAlarm = get(),
            showAlarm = ShowAlarm(get(), get(), get()),
            snoozeAlarm = SnoozeAlarm(get(), get(), get(), get()),
            cancelAlarm = CancelAlarm(get())
        )
    }

    @OptIn(ExperimentalMaterial3Api::class)
    single { MathAlarmNotification(androidContext(), get(), get()) }

    single<AudioPlayer> { PlayerWrapper(androidContext()) }

    single { MathAlarmNotificationChannel(androidContext()) }

    single { AlarmPreferencesImpl(androidContext(), AppThemeOptionsMapper()) }

    @OptIn(
        ExperimentalAnimationApi::class,
        InternalCoroutinesApi::class,
        ExperimentalComposeUiApi::class,
        ExperimentalFoundationApi::class,
        ExperimentalMaterial3Api::class
    )
    single<NotificationInteractor> { NotificationInteractorImpl(get()) }

    single { AlarmNotificationScheduler(androidContext()) }

    single<AndroidVersion> { AndroidVersionImpl() }

    single {
        AlarmPermission(androidContext().getAlarmManager(), get())
    }

    viewModel { AlarmListViewModel(get(), get()) }
    viewModel { AlarmSettingsViewModel(get()) }
    viewModel { AlarmMathViewModel(get(), get()) }
}