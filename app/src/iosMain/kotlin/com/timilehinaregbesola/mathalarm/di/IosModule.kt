package com.timilehinaregbesola.mathalarm.di

import androidx.room.Room
import androidx.sqlite.driver.bundled.BundledSQLiteDriver
import co.touchlab.kermit.Logger
import co.touchlab.kermit.StaticConfig
import co.touchlab.kermit.platformLogWriter
import com.timilehinaregbesola.mathalarm.framework.app.permission.AlarmPermission
import com.timilehinaregbesola.mathalarm.framework.app.permission.AlarmPermissionImpl
import com.timilehinaregbesola.mathalarm.framework.database.AlarmDatabase
import com.timilehinaregbesola.mathalarm.framework.database.MIGRATION_2_3
import com.timilehinaregbesola.mathalarm.framework.database.MIGRATION_3_4
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
val iosModule = module {
    // Room Database for iOS
    single<AlarmDatabase> {
        val dbFile = documentDirectory() + "/alarm_history_database.db"
        Room.databaseBuilder<AlarmDatabase>(
            name = dbFile
        )
            .addMigrations(MIGRATION_2_3, MIGRATION_3_4)
            .setDriver(BundledSQLiteDriver())
            .build()
    }
    
    single { get<AlarmDatabase>().alarmDatabaseDao }
    
    // iOS Alarm Scheduler
    single { IosAlarmScheduler(getWith("IosAlarmScheduler")) }
    
    // iOS Audio Player
    single<AudioPlayer> { IosAudioPlayer(getWith("IosAudioPlayer")) }
    
    // Alarm Interactor (iOS implementation)
    single<AlarmInteractor> { AlarmInteractorImpl(getWith("AlarmInteractorImpl")) }
    
    // Notification Interactor (iOS implementation)
    single<NotificationInteractor> {
        NotificationInteractorImpl(getWith("NotificationInteractorImpl"))
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

/**
 * Initialize Koin for iOS
 */
fun initKoin() {
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
