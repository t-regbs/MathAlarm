package com.timilehinaregbesola.mathalarm.di

import androidx.room.Room
import com.timilehinaregbesola.mathalarm.analytics.AnalyticsTracker
import com.timilehinaregbesola.mathalarm.analytics.NoopAnalyticsTracker
import androidx.sqlite.driver.NativeSQLiteDriver
import co.touchlab.kermit.Logger
import co.touchlab.kermit.StaticConfig
import co.touchlab.kermit.platformLogWriter
import com.timilehinaregbesola.mathalarm.framework.app.permission.AlarmPermission
import com.timilehinaregbesola.mathalarm.framework.app.permission.AlarmPermissionImpl
import com.timilehinaregbesola.mathalarm.framework.database.AlarmDatabase
import com.timilehinaregbesola.mathalarm.interactors.AlarmInteractor
import com.timilehinaregbesola.mathalarm.interactors.AlarmInteractorImpl
import com.timilehinaregbesola.mathalarm.interactors.AudioPlayer
import com.timilehinaregbesola.mathalarm.interactors.IosAudioPlayer
import com.timilehinaregbesola.mathalarm.interactors.NotificationInteractor
import com.timilehinaregbesola.mathalarm.interactors.NotificationInteractorImpl
import com.timilehinaregbesola.mathalarm.notification.IosAlarmScheduler
import kotlinx.cinterop.ExperimentalForeignApi
import org.koin.core.context.startKoin
import org.koin.dsl.module
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

/**
 * iOS-specific Koin module
 */
internal val iosModule = module {
    single<AnalyticsTracker> { NoopAnalyticsTracker }
    // Room Database for iOS - uses lazy initialization
    // The database will only be built when first injected
    single<AlarmDatabase> {
        println("IosModule: Building Room database (lazy init)")
        val dbFile = documentDirectory() + "/alarm_history_database.db"
        buildIosAlarmDatabase(dbFile)
    }
    
    single { get<AlarmDatabase>().alarmDatabaseDao }
    
    // AlarmKit scheduler shared by both interactors
    single { IosAlarmScheduler(getWith("IosAlarmScheduler")) }
    
    // iOS Audio Player
    single<AudioPlayer> { IosAudioPlayer(getWith("IosAudioPlayer")) }
    
    // Alarm Interactor (iOS implementation)
    single<AlarmInteractor> { AlarmInteractorImpl(get()) }
    
    // Notification Interactor (iOS implementation)
    single<NotificationInteractor> {
        NotificationInteractorImpl(get())
    }
    
    // Alarm Permission (iOS doesn't need exact alarm permission)
    single<AlarmPermission> { AlarmPermissionImpl() }
    
    // Platform Logger - iOS-specific using platform log writer
    // This logger is used by all components via getWith("ComponentName")
    single { (tag: String) ->
        Logger(
            StaticConfig(
                logWriterList = listOf(platformLogWriter())
            ), tag
        )
    }
}

/** Reopens existing schema-9 files; the driver change does not migrate/reset data. */
internal fun buildIosAlarmDatabase(filePath: String): AlarmDatabase =
    Room.databaseBuilder<AlarmDatabase>(name = filePath)
        .setDriver(NativeSQLiteDriver())
        .build()

/**
 * Initialize Koin for iOS.
 * Called once from Swift's App init() before UI loads.
 */
internal fun initKoin() {
    println("IosModule: Initializing Koin")
    startKoin {
        modules(commonModule, iosModule)
    }
}

/**
 * Get iOS document directory path
 */
@OptIn(ExperimentalForeignApi::class)
private fun documentDirectory(): String {
    val documentDirectory = NSFileManager.defaultManager.URLForDirectory(
        directory = NSDocumentDirectory,
        inDomain = NSUserDomainMask,
        appropriateForURL = null,
        create = false,
        error = null,
    )
    return requireNotNull(documentDirectory?.path)
}
