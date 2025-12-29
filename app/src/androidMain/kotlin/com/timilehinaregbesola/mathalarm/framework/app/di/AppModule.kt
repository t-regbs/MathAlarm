package com.timilehinaregbesola.mathalarm.framework.app.di

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.room.Room
import co.touchlab.kermit.ExperimentalKermitApi
import co.touchlab.kermit.Logger
import co.touchlab.kermit.StaticConfig
import co.touchlab.kermit.crashlytics.CrashlyticsLogWriter
import co.touchlab.kermit.platformLogWriter
import com.timilehinaregbesola.mathalarm.di.commonModule
import com.timilehinaregbesola.mathalarm.di.getWith
import com.timilehinaregbesola.mathalarm.framework.app.permission.AlarmPermission
import com.timilehinaregbesola.mathalarm.framework.app.permission.AlarmPermissionImpl
import com.timilehinaregbesola.mathalarm.framework.app.permission.AndroidVersion
import com.timilehinaregbesola.mathalarm.framework.app.permission.AndroidVersionImpl
import com.timilehinaregbesola.mathalarm.framework.database.AlarmDatabase
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
import com.timilehinaregbesola.mathalarm.utils.getAlarmManager
import kotlinx.coroutines.InternalCoroutinesApi
import org.koin.android.ext.koin.androidApplication
import org.koin.android.ext.koin.androidContext
import org.koin.core.module.Module
import org.koin.core.parameter.parametersOf
import org.koin.core.scope.Scope
import org.koin.dsl.module

/**
 * Android-specific Koin module containing platform dependencies
 */
@OptIn(
    ExperimentalKermitApi::class,
    ExperimentalAnimationApi::class,
    ExperimentalComposeUiApi::class,
    ExperimentalFoundationApi::class,
    InternalCoroutinesApi::class
)
val androidModule = module {
    // Room Database for Android
    single<AlarmDatabase> {
        Room.databaseBuilder(
            androidApplication(),
            AlarmDatabase::class.java,
            "alarm_history_database"
        ).addMigrations(MIGRATION_2_3, MIGRATION_3_4).build()
    }

    single { get<AlarmDatabase>().alarmDatabaseDao }

    // Android Alarm Interactor
    single<AlarmInteractor> { AlarmInteractorImpl(get(), getWith("AlarmInteractorImpl")) }

    // Android Notification components
    @OptIn(ExperimentalMaterial3Api::class)
    single {
        MathAlarmNotification(
            androidContext(),
            get(),
            get(),
            getWith("MathAlarmNotification")
        )
    }

    single { MathAlarmNotificationChannel(androidContext()) }

    single { AlarmNotificationScheduler(androidContext(), getWith("AlarmNotificationScheduler")) }

    @OptIn(
        ExperimentalAnimationApi::class,
        InternalCoroutinesApi::class,
        ExperimentalComposeUiApi::class,
        ExperimentalFoundationApi::class,
        ExperimentalMaterial3Api::class
    )
    single<NotificationInteractor> {
        NotificationInteractorImpl(
            get(),
            getWith("NotificationInteractorImpl")
        )
    }

    // Android Audio Player
    single<AudioPlayer> { PlayerWrapper(androidContext(), getWith("PlayerWrapper")) }

    // Android Version and Permission
    single<AndroidVersion> { AndroidVersionImpl() }

    single<AlarmPermission> {
        AlarmPermissionImpl(androidContext().getAlarmManager(), get())
    }

    // Platform Logger - Android-specific with Crashlytics integration
    // This logger is used by all components via getWith("ComponentName")
    single { (tag: String) ->
        Logger(
            StaticConfig(
                logWriterList = listOf(
                    platformLogWriter(),
                    CrashlyticsLogWriter()
                )
            ), tag
        )
    }
}

/**
 * Combined app module (common + android)
 */
@ExperimentalFoundationApi
@ExperimentalComposeUiApi
@InternalCoroutinesApi
@ExperimentalAnimationApi
val appModule: Module = module {
    includes(commonModule, androidModule)
}