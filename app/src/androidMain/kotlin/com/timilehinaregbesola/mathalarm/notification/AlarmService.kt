package com.timilehinaregbesola.mathalarm.notification

import android.app.PendingIntent
import android.app.Service
import android.app.TaskStackBuilder
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.graphics.BitmapFactory
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.RingtoneManager
import android.os.Build
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import android.os.PowerManager
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.core.app.NotificationCompat
import androidx.core.app.ServiceCompat
import androidx.core.net.toUri
import co.touchlab.kermit.Logger
import com.timilehinaregbesola.mathalarm.AlarmReceiver
import com.timilehinaregbesola.mathalarm.R
import com.timilehinaregbesola.mathalarm.domain.model.Alarm
import com.timilehinaregbesola.mathalarm.framework.database.AlarmEntity
import com.timilehinaregbesola.mathalarm.framework.database.AlarmMapper
import com.timilehinaregbesola.mathalarm.presentation.MainActivity
import kotlinx.coroutines.InternalCoroutinesApi
import kotlinx.serialization.json.Json
import org.koin.android.ext.android.inject
import java.io.InputStream
import java.net.URLEncoder

/**
 * Foreground service that handles alarm playback independently of the app lifecycle.
 * This ensures the alarm keeps ringing even if the user force-closes the app.
 */
@ExperimentalAnimationApi
@InternalCoroutinesApi
@ExperimentalComposeUiApi
@ExperimentalMaterial3Api
@ExperimentalFoundationApi
class AlarmService : Service() {

    private val channel: MathAlarmNotificationChannel by inject()
    private val logger = Logger.withTag("AlarmService")
    
    private var mediaPlayer: MediaPlayer? = null
    private var wakeLock: PowerManager.WakeLock? = null
    private var currentAlarm: Alarm? = null
    private val timeoutHandler = Handler(Looper.getMainLooper())
    
    // Timing controller for ring/pause/restart cycle
    private var timingController: AlarmTimingController? = null
    
    // Handler-based scheduler for the timing controller
    private val handlerScheduler = object : AlarmTimingController.TimingScheduler {
        override fun scheduleDelayed(task: Runnable, delayMillis: Long) {
            timeoutHandler.postDelayed(task, delayMillis)
        }
        
        override fun cancel(task: Runnable) {
            timeoutHandler.removeCallbacks(task)
        }
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onCreate() {
        super.onCreate()
        logger.d("AlarmService created")
        
        // Acquire wake lock to keep CPU running
        val powerManager = getSystemService(Context.POWER_SERVICE) as PowerManager
        wakeLock = powerManager.newWakeLock(
            PowerManager.PARTIAL_WAKE_LOCK,
            "mathalarm:alarmservice"
        ).apply {
            // Acquire for max 1 hour - service will be stopped when user dismisses
            acquire(MAX_WAKE_LOCK_MILLIS)
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        logger.d("onStartCommand: action=${intent?.action}")
        
        when (intent?.action) {
            ACTION_START_ALARM -> {
                val alarmJson = intent.getStringExtra(EXTRA_ALARM_JSON)
                if (alarmJson != null) {
                    try {
                        val alarmEntity = Json.decodeFromString<AlarmEntity>(alarmJson)
                        val alarm = AlarmMapper().mapToDomainModel(alarmEntity)
                        startAlarm(alarm)
                    } catch (e: Exception) {
                        logger.e("Failed to parse alarm JSON", e)
                        stopSelf()
                    }
                } else {
                    logger.e("No alarm JSON provided")
                    stopSelf()
                }
            }
            ACTION_STOP_ALARM -> {
                logger.d("Stopping alarm service")
                stopSelf()
            }
            ACTION_REFRESH_NOTIFICATION -> {
                // Refresh notification to trigger full-screen intent again
                currentAlarm?.let { alarm ->
                    logger.d("Refreshing notification for alarm: ${alarm.title}")
                    refreshNotificationForAlarm(alarm)
                }
            }
            else -> {
                logger.w("Unknown action: ${intent?.action}")
            }
        }
        
        return START_NOT_STICKY
    }

    private fun startAlarm(alarm: Alarm) {
        logger.d("Starting alarm: ${alarm.title}")
        currentAlarm = alarm
        
        ActiveAlarmManager.setActiveAlarm(alarm.alarmId)
        
        // Initialize timing controller with callbacks
        timingController = AlarmTimingController(
            ringDurationMillis = RING_DURATION_MILLIS,
            silencePeriodMillis = SILENCE_PERIOD_MILLIS,
            onStartRinging = { onStartRinging(alarm) },
            onPauseRinging = { onPauseRinging(alarm) },
            scheduler = handlerScheduler
        )
        
        // Show initial notification
        showForegroundNotification(alarm, isPaused = false)
        
        // Start the timing controller (will call onStartRinging)
        timingController?.start()
    }
    
    private fun onStartRinging(alarm: Alarm) {
        logger.d("Starting/Restarting alarm audio")
        showForegroundNotification(alarm, isPaused = false)
        startAudioPlayback(alarm)
    }
    
    private fun onPauseRinging(alarm: Alarm) {
        logger.d("Pausing alarm audio, will restart in ${SILENCE_PERIOD_MILLIS / 1000}s")
        stopAudioPlayback()
        showForegroundNotification(alarm, isPaused = true)
    }
    
    private fun showForegroundNotification(alarm: Alarm, isPaused: Boolean) {
        val notification = buildNotification(alarm, isPaused = isPaused)
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
            ServiceCompat.startForeground(
                this,
                alarm.alarmId.toInt(),
                notification.build(),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            )
        } else if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(
                alarm.alarmId.toInt(),
                notification.build(),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_MEDIA_PLAYBACK
            )
        } else {
            startForeground(alarm.alarmId.toInt(), notification.build())
        }
    }

    private fun refreshNotificationForAlarm(alarm: Alarm) {
        val isPaused = timingController?.currentState == AlarmTimingController.State.PAUSED
        showForegroundNotification(alarm, isPaused = isPaused)
    }

    private fun startAudioPlayback(alarm: Alarm) {
        try {
            stopAudioPlayback()
            
            val toneUri = alarm.alarmTone.toUri()
            var uriExists = false
            
            try {
                val inputStream: InputStream? = contentResolver.openInputStream(toneUri)
                inputStream?.close()
                uriExists = true
            } catch (_: Exception) {
                logger.w("Alarm tone URI does not exist: $toneUri")
            }
            
            val toneString = if (uriExists) {
                alarm.alarmTone
            } else {
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM).toString()
            }
            
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(
                    AudioAttributes.Builder()
                        .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC)
                        .setUsage(AudioAttributes.USAGE_ALARM)
                        .build()
                )
                setDataSource(this@AlarmService, toneString.toUri())
                isLooping = true
                setOnErrorListener { mp, _, _ ->
                    logger.e("MediaPlayer error")
                    mp.release()
                    mediaPlayer = null
                    true
                }
                prepare()
                start()
            }
            
            logger.d("Audio playback started")
        } catch (e: Exception) {
            logger.e("Failed to start audio playback", e)
        }
    }

    private fun stopAudioPlayback() {
        try {
            mediaPlayer?.run {
                if (isPlaying) stop()
                release()
            }
        } catch (e: Exception) {
            logger.w("Error stopping media player", e)
        } finally {
            mediaPlayer = null
        }
    }

    private fun buildNotification(alarm: Alarm, isPaused: Boolean = false): NotificationCompat.Builder {
        val alarmImage = BitmapFactory.decodeResource(resources, R.drawable.icon)
        val vibratePattern = if (isPaused) null else longArrayOf(0, 100, 200, 300)
        val bigPicStyle = NotificationCompat.BigPictureStyle()
            .bigPicture(alarmImage)
        
        val contentText = if (isPaused) {
            "Paused - will ring again soon. Tap to dismiss."
        } else {
            alarm.title
        }
        
        return NotificationCompat.Builder(this, channel.getAlarmChannelId()).apply {
            setContentIntent(buildPendingIntent(alarm))
            setSmallIcon(R.drawable.icon)
            setContentTitle(getString(R.string.notification_title))
            setContentText(contentText)
            setStyle(bigPicStyle)
            setSound(null) // We handle audio separately
            setLargeIcon(alarmImage)
            setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            setCategory(NotificationCompat.CATEGORY_ALARM)
            if (vibratePattern != null) {
                setVibrate(vibratePattern)
            }
            setPriority(NotificationCompat.PRIORITY_HIGH)
            setOngoing(true) // Cannot be dismissed by swiping
            setAutoCancel(false)
            addAction(getSnoozeAction(alarm))
            // Only set full-screen intent when actively ringing
            if (!isPaused) {
                setFullScreenIntent(buildPendingIntent(alarm), true)
            }
            // Re-show notification immediately if user somehow manages to dismiss it
            setDeleteIntent(getDismissIntent(alarm))
        }
    }

    private fun getDismissIntent(alarm: Alarm): PendingIntent {
        return getActionIntent(alarm, AlarmReceiver.DISMISS_ACTION, REQUEST_CODE_ACTION_DISMISS)
    }

    private fun buildPendingIntent(alarm: Alarm): PendingIntent {
        val alarmEntity = AlarmMapper().mapFromDomainModel(alarm)
        val json = Json.encodeToString(alarmEntity)
        val alarmJson = URLEncoder.encode(json, "utf-8")
        val notificationIntent = Intent(
            Intent.ACTION_VIEW,
            "https://timilehinaregbesola.com/alarmId=$alarmJson".toUri(),
            this,
            MainActivity::class.java,
        )
        return TaskStackBuilder.create(this).run {
            addNextIntentWithParentStack(notificationIntent)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                getPendingIntent(REQUEST_CODE_OPEN_TASK, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_MUTABLE)
            } else {
                getPendingIntent(REQUEST_CODE_OPEN_TASK, PendingIntent.FLAG_UPDATE_CURRENT)
            }
        }!!
    }

    private fun getSnoozeAction(alarm: Alarm): NotificationCompat.Action {
        val actionTitle = getString(R.string.notification_action_snooze)
        val intent = getActionIntent(alarm, AlarmReceiver.SNOOZE_ACTION, REQUEST_CODE_ACTION_SNOOZE)
        return NotificationCompat.Action(0, actionTitle, intent)
    }

    private fun getActionIntent(
        alarm: Alarm,
        intentAction: String,
        requestCode: Int,
    ): PendingIntent {
        val receiverIntent = Intent(this, AlarmReceiver::class.java).apply {
            action = intentAction
            putExtra(AlarmReceiver.EXTRA_TASK, alarm.alarmId)
        }

        return PendingIntent.getBroadcast(
            this,
            requestCode,
            receiverIntent,
            PendingIntent.FLAG_CANCEL_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
    }

    override fun onDestroy() {
        logger.d("AlarmService destroyed")
        
        ActiveAlarmManager.clearActiveAlarm()
        
        // Stop timing controller (cancels all scheduled callbacks)
        timingController?.stop()
        timingController = null
        
        stopAudioPlayback()
        
        try {
            wakeLock?.let {
                if (it.isHeld) it.release()
            }
        } catch (e: Exception) {
            logger.w("Error releasing wake lock", e)
        }
        wakeLock = null
        
        super.onDestroy()
    }

    companion object {
        const val ACTION_START_ALARM = "com.timilehinaregbesola.mathalarm.START_ALARM"
        const val ACTION_STOP_ALARM = "com.timilehinaregbesola.mathalarm.STOP_ALARM"
        const val ACTION_REFRESH_NOTIFICATION = "com.timilehinaregbesola.mathalarm.REFRESH_NOTIFICATION"
        const val EXTRA_ALARM_JSON = "extra_alarm_json"
        
        private const val REQUEST_CODE_OPEN_TASK = 1_121_111
        private const val REQUEST_CODE_ACTION_SNOOZE = 4_321
        private const val REQUEST_CODE_ACTION_DISMISS = 5_678
        
        // Ring for 10 minutes before auto-pausing
        private const val RING_DURATION_MILLIS = 10 * 60 * 1000L
        
        // Silence period before ringing again (1 minute)
        private const val SILENCE_PERIOD_MILLIS = 1 * 60 * 1000L
        
        // Maximum wake lock duration (1 hour) - service stops when user dismisses
        private const val MAX_WAKE_LOCK_MILLIS = 60 * 60 * 1000L
        
        /**
         * Start the alarm service with the given alarm.
         */
        fun startAlarm(context: Context, alarm: Alarm) {
            val alarmEntity = AlarmMapper().mapFromDomainModel(alarm)
            val alarmJson = Json.encodeToString(alarmEntity)
            
            val intent = Intent(context, AlarmService::class.java).apply {
                action = ACTION_START_ALARM
                putExtra(EXTRA_ALARM_JSON, alarmJson)
            }

            context.startForegroundService(intent)
        }
        
        /**
         * Stop the alarm service.
         */
        fun stopAlarm(context: Context) {
            val intent = Intent(context, AlarmService::class.java).apply {
                action = ACTION_STOP_ALARM
            }
            context.stopService(intent)
        }
        
        /**
         * Refresh the notification to trigger full-screen intent again.
         * Call this when the user closes the app while alarm is active.
         */
        fun refreshNotification(context: Context) {
            val intent = Intent(context, AlarmService::class.java).apply {
                action = ACTION_REFRESH_NOTIFICATION
            }

            context.startForegroundService(intent)
        }
    }
}
