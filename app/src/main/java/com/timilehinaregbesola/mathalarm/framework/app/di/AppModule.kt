package com.timilehinaregbesola.mathalarm.framework.app.di

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import android.media.MediaPlayer
import android.preference.PreferenceManager
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.room.Room
import com.google.accompanist.navigation.material.ExperimentalMaterialNavigationApi
import com.timilehinaregbesola.mathalarm.data.AlarmRepository
import com.timilehinaregbesola.mathalarm.framework.RoomAlarmDataSource
import com.timilehinaregbesola.mathalarm.framework.Usecases
import com.timilehinaregbesola.mathalarm.framework.app.permission.AlarmPermission
import com.timilehinaregbesola.mathalarm.framework.app.permission.AndroidVersion
import com.timilehinaregbesola.mathalarm.framework.app.permission.AndroidVersionImpl
import com.timilehinaregbesola.mathalarm.framework.database.*
import com.timilehinaregbesola.mathalarm.interactors.AlarmInteractor
import com.timilehinaregbesola.mathalarm.interactors.AlarmInteractorImpl
import com.timilehinaregbesola.mathalarm.interactors.NotificationInteractor
import com.timilehinaregbesola.mathalarm.interactors.NotificationInteractorImpl
import com.timilehinaregbesola.mathalarm.notification.AlarmNotificationScheduler
import com.timilehinaregbesola.mathalarm.notification.MathAlarmNotification
import com.timilehinaregbesola.mathalarm.notification.MathAlarmNotificationChannel
import com.timilehinaregbesola.mathalarm.provider.CalendarProvider
import com.timilehinaregbesola.mathalarm.provider.CalendarProviderImpl
import com.timilehinaregbesola.mathalarm.usecases.alarm.*
import com.timilehinaregbesola.mathalarm.utils.getAlarmManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.InternalCoroutinesApi
import javax.inject.Named
import javax.inject.Singleton

@ExperimentalFoundationApi
@ExperimentalMaterialApi
@ExperimentalComposeUiApi
@InternalCoroutinesApi
@ExperimentalAnimationApi
@ExperimentalMaterialNavigationApi
@Module
@InstallIn(SingletonComponent::class)
object AppModule {
    @Provides
    @Singleton
    fun provideNoteDatabase(app: Application): AlarmDatabase {
        return Room.databaseBuilder(
            app,
            AlarmDatabase::class.java,
            "alarm_history_database"
        ).addMigrations(MIGRATION_2_3, MIGRATION_3_4).build()
    }

    @Provides
    @Singleton
    fun provideAlarmDao(database: AlarmDatabase): AlarmDao {
        return database.alarmDatabaseDao
    }

    @Provides
    @Singleton
    fun provideAlarmMapper(): AlarmMapper {
        return AlarmMapper()
    }

    @Provides
    @Singleton
    fun provideCalenderProvider(): CalendarProvider {
        return CalendarProviderImpl()
    }

    @Provides
    @Singleton
    fun provideScheduleNextAlarm(interactor: AlarmInteractor): ScheduleNextAlarm {
        return ScheduleNextAlarm(interactor)
    }

    @Provides
    @Singleton
    fun provideDataSource(alarmDao: AlarmDao, mapper: AlarmMapper): RoomAlarmDataSource {
        return RoomAlarmDataSource(alarmDao, mapper)
    }

    @Provides
    @Singleton
    fun provideRepository(alarmDataSource: RoomAlarmDataSource): AlarmRepository {
        return AlarmRepository(alarmDataSource)
    }

    @Provides
    @Singleton
    fun provideAlarmInteractor(alarmManager: AlarmNotificationScheduler): AlarmInteractor {
        return AlarmInteractorImpl(alarmManager)
    }

    @Provides
    @Singleton
    fun provideInteractors(
        repository: AlarmRepository,
        alarmInteractor: AlarmInteractor,
        notificationInteractor: NotificationInteractor,
        calendarProvider: CalendarProvider,
        scheduleNextAlarm: ScheduleNextAlarm
    ): Usecases {
        return Usecases(
            addAlarm = AddAlarm(repository),
            clearAlarms = ClearAlarms(repository, alarmInteractor),
            deleteAlarm = DeleteAlarm(repository, alarmInteractor),
            findAlarm = FindAlarm(repository),
            getSavedAlarms = GetSavedAlarms(repository),
            updateAlarm = UpdateAlarm(repository),
            scheduleAlarm = ScheduleAlarm(repository, alarmInteractor),
            completeAlarm = CompleteAlarm(repository, alarmInteractor, notificationInteractor),
            rescheduleFutureAlarms = RescheduleFutureAlarms(repository, alarmInteractor),
            scheduleNextAlarm = ScheduleNextAlarm(alarmInteractor),
            showAlarm = ShowAlarm(repository, notificationInteractor, scheduleNextAlarm),
            snoozeAlarm = SnoozeAlarm(calendarProvider, notificationInteractor, alarmInteractor, repository),
            cancelAlarm = CancelAlarm(alarmInteractor)
        )
    }

    @Provides
    @Singleton
    fun provideAlarmNotification(
        @ApplicationContext context: Context,
        channel: MathAlarmNotificationChannel,
        player: MediaPlayer
    ): MathAlarmNotification {
        return MathAlarmNotification(context, channel, player)
    }

    @Provides
    @Singleton
    fun provideAudioPlayer(): MediaPlayer {
        return MediaPlayer()
    }

    @Provides
    @Singleton
    fun provideAlarmNotificationChannel(@ApplicationContext context: Context): MathAlarmNotificationChannel {
        return MathAlarmNotificationChannel(context)
    }

    @OptIn(
        ExperimentalMaterialNavigationApi::class,
        androidx.compose.animation.ExperimentalAnimationApi::class,
        kotlinx.coroutines.InternalCoroutinesApi::class,
        androidx.compose.ui.ExperimentalComposeUiApi::class,
        androidx.compose.material.ExperimentalMaterialApi::class,
        androidx.compose.foundation.ExperimentalFoundationApi::class
    )
    @Provides
    @Singleton
    fun provideNotificationInteractor(alarmNotification: MathAlarmNotification): NotificationInteractor {
        return NotificationInteractorImpl(alarmNotification)
    }

    @Provides
    @Singleton
    fun provideNotificationScheduler(@ApplicationContext context: Context): AlarmNotificationScheduler {
        return AlarmNotificationScheduler(context)
    }

    @Named("app")
    @Provides
    @Singleton
    fun provideAppPreferences(
        @ApplicationContext context: Context
    ): SharedPreferences {
        return PreferenceManager.getDefaultSharedPreferences(context)
    }

    @Provides
    @Singleton
    fun provideAndroidVersion(): AndroidVersion {
        return AndroidVersionImpl()
    }

    @Provides
    @Singleton
    fun provideAlarmPermission(@ApplicationContext context: Context, version: AndroidVersion): AlarmPermission {
        return AlarmPermission(context.getAlarmManager(), version)
    }
}
