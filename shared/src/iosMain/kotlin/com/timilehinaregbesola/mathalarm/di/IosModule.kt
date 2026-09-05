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
import com.timilehinaregbesola.mathalarm.framework.database.MIGRATION_4_5
import com.timilehinaregbesola.mathalarm.interactors.AlarmInteractor
import com.timilehinaregbesola.mathalarm.interactors.AlarmInteractorImpl
import com.timilehinaregbesola.mathalarm.interactors.AudioPlayer
import com.timilehinaregbesola.mathalarm.interactors.IosAudioPlayer
import com.timilehinaregbesola.mathalarm.interactors.NotificationInteractor
import com.timilehinaregbesola.mathalarm.interactors.NotificationInteractorImpl
import com.timilehinaregbesola.mathalarm.notification.IosAlarmNotification
import com.timilehinaregbesola.mathalarm.notification.IosAlarmScheduler
import com.timilehinaregbesola.mathalarm.notification.NotificationActionDelegate
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject
import org.koin.core.context.startKoin
import org.koin.dsl.module
import platform.Foundation.NSDocumentDirectory
import platform.Foundation.NSFileManager
import platform.Foundation.NSUserDomainMask

/**
 * iOS-specific Koin module
 */
val iosModule = module {
    // Room Database for iOS - uses lazy initialization
    // The database will only be built when first injected
    single<AlarmDatabase> {
        println("IosModule: Building Room database (lazy init)")
        val dbFile = documentDirectory() + "/alarm_history_database.db"
        Room.databaseBuilder<AlarmDatabase>(
            name = dbFile
        )
            .addMigrations(MIGRATION_2_3, MIGRATION_3_4, MIGRATION_4_5)
            .setDriver(BundledSQLiteDriver())
            .build()
    }
    
    single { get<AlarmDatabase>().alarmDatabaseDao }
    
    // iOS Alarm Scheduler - schedules notifications
    single { IosAlarmScheduler(getWith("IosAlarmScheduler")) }
    
    // iOS Alarm Notification - handles showing/dismissing delivered notifications
    single { IosAlarmNotification(getWith("IosAlarmNotification")) }
    
    // Notification Action Delegate - handles snooze/dismiss actions from notifications
    single { 
        NotificationActionDelegate(
            appCoroutineScope = get(),
            usecases = get(),
            logger = getWith("NotificationActionDelegate")
        )
    }
    
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
 * Initialize Koin for iOS.
 * Called once from Swift's App init() before UI loads.
 */
fun initKoin() {
    println("IosModule: Initializing Koin")
    startKoin {
        modules(commonModule, iosModule)
    }
}

/**
 * Prewarm the database in background.
 * Call this after Koin init to initialize Room on a background thread,
 * so it's ready when the UI needs it.
 * 
 */
fun prewarmDatabase() {
    CoroutineScope(Dispatchers.IO).launch {
        try {
            println("IosModule: Prewarming database in background")
            val helper = object : KoinComponent {
                val database: AlarmDatabase by inject()
            }
            // Trigger lazy initialization by accessing the database
            // This runs the Room builder and migrations off the main thread
            helper.database.alarmDatabaseDao
            println("IosModule: Database prewarmed successfully")
        } catch (e: Exception) {
            println("IosModule: Database prewarm failed: ${e.message}")
        }
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
