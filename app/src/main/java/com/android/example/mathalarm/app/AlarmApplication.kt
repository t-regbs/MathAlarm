package com.android.example.mathalarm.app

import android.app.Application
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.graphics.Color
import android.os.Build
import com.android.example.mathalarm.R
import com.android.example.mathalarm.app.di.databaseModule
import com.android.example.mathalarm.app.di.repositoryModule
import com.android.example.mathalarm.app.di.viewModelModule
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import org.koin.core.logger.Level
import timber.log.Timber

class AlarmApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        Timber.plant(Timber.DebugTree())
        startKoin {
            androidLogger(Level.ERROR)
            androidContext(this@AlarmApplication)
            modules(listOf(databaseModule, repositoryModule, viewModelModule))
        }
        createChannel(
            getString(R.string.alarm_notification_channel_id),
            getString(R.string.notification_title)
        )
    }

    private fun createChannel(channelId: String, channelName: String) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val notificationChannel = NotificationChannel(
                channelId,
                channelName,
                NotificationManager.IMPORTANCE_HIGH
            )
                .apply {
                    setShowBadge(false)
                    enableLights(false)
                    lightColor = Color.RED
                    enableVibration(true)
                    description = getString(R.string.math_alarm)
                }

            val notificationManager = getSystemService(
                NotificationManager::class.java
            )
            notificationManager.createNotificationChannel(notificationChannel)

        }
    }
}