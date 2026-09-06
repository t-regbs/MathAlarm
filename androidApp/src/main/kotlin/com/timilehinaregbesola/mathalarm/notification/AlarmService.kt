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
import android.os.VibrationEffect
import android.os.Vibrator
import android.util.Base64
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import kotlinx.coroutines.cancel
import com.timilehinaregbesola.mathalarm.framework.Usecases

/**
 * Foreground service that handles alarm playback independently of the app lifecycle.
 * Playback survives activity teardown; a system force-stop still stops the application.
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
    private val queuedAlarms = linkedMapOf<Long, Alarm>()
    private var alarmVibrator: Vibrator? = null
    private val usecases: Usecases by inject()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private val playbackState by lazy { getSharedPreferences("active_alarm_playback", MODE_PRIVATE) }
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
        
        if (intent == null) {
            restorePlayback()
            return START_STICKY
        }
        when (intent.action) {
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
                val id = intent.getLongExtra(EXTRA_ALARM_ID, -1)
                queuedAlarms.remove(id)
                persistPlayback()
                if (currentAlarm?.alarmId == id) {
                    timingController?.stop()
                    stopAudioPlayback()
                    val next = queuedAlarms.values.firstOrNull()
                    if (next == null) {
                        currentAlarm = null
                        persistPlayback()
                        stopSelf()
                    } else {
                        queuedAlarms.remove(next.alarmId)
                        currentAlarm = null
                        startAlarm(next)
                    }
                }
            }
            else -> {
                logger.w("Unknown action: ${intent?.action}")
            }
        }
        
        return if (currentAlarm == null) START_NOT_STICKY else START_STICKY
    }

    private fun startAlarm(alarm: Alarm) {
        logger.d("Starting alarm: ${alarm.title}")
        val current = currentAlarm
        if (current?.alarmId == alarm.alarmId) {
            currentAlarm = alarm
            persistPlayback()
            refreshNotificationForAlarm(alarm)
            return
        }
        if (current != null) {
            queuedAlarms[alarm.alarmId] = alarm
            persistPlayback()
            AlarmDeliveryLog.record(this, "queued", alarm.alarmId, alarm.activeAt)
            return
        }
        timingController?.stop()
        stopAudioPlayback()
        currentAlarm = alarm
        persistPlayback()
        AlarmDeliveryLog.record(this, "service_started", alarm.alarmId, alarm.activeAt)

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
    
    private fun persistPlayback() {
        val alarms = listOfNotNull(currentAlarm) + queuedAlarms.values
        val mapper = AlarmMapper()
        val snapshots = alarms.map(mapper::mapFromDomainModel)
        playbackState.edit().putString("alarms", Json.encodeToString(snapshots)).commit()
    }

    private fun restorePlayback() {
        val saved = runCatching {
            Json.decodeFromString<List<AlarmEntity>>(playbackState.getString("alarms", "[]") ?: "[]")
        }.getOrDefault(emptyList())
        if (saved.isEmpty()) { stopSelf(); return }
        // Enter the foreground promptly, then validate against Room before restarting audio.
        showForegroundNotification(AlarmMapper().mapToDomainModel(saved.first()), isPaused = true)
        serviceScope.launch {
            try {
                val valid = usecases.command {
                    saved.mapNotNull { snapshot ->
                        findAlarm(snapshot.alarmId)?.takeIf { it.isOn && it.activeAt != null && it.activeAt == snapshot.activeAt }
                    }
                }
                valid.forEach { startAlarm(it) }
                if (currentAlarm == null) { persistPlayback(); stopSelf() }
            } catch (e: kotlinx.coroutines.CancellationException) {
                throw e
            } catch (e: Exception) {
                logger.e(e) { "Unable to restore active alarm playback" }
                stopSelf()
            }
        }
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
        stopAudioPlayback()
        if (alarm.vibrate) {
            alarmVibrator = (getSystemService(Context.VIBRATOR_SERVICE) as Vibrator).also {
                it.vibrate(VibrationEffect.createWaveform(longArrayOf(0, 500, 500), 0))
            }
        }
        val candidates = listOfNotNull(
            alarm.alarmTone.takeIf { it.isNotBlank() },
            RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)?.toString(),
            "android.resource://$packageName/${R.raw.alarm_fallback}"
        ).distinct()
        playCandidate(alarm, candidates, 0)
    }

    private fun playCandidate(alarm: Alarm, candidates: List<String>, index: Int) {
        if (index >= candidates.size) {
            AlarmDeliveryLog.record(this, "audio_failed", alarm.alarmId, alarm.activeAt, "All audio sources failed")
            return
        }
        val player = MediaPlayer()
        mediaPlayer = player
        try {
            player.setAudioAttributes(AudioAttributes.Builder()
                .setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).setUsage(AudioAttributes.USAGE_ALARM).build())
            player.setWakeMode(this, PowerManager.PARTIAL_WAKE_LOCK)
            player.isLooping = true
            player.setOnPreparedListener {
                if (mediaPlayer === it && currentAlarm?.alarmId == alarm.alarmId) {
                    it.start()
                    AlarmDeliveryLog.record(this, "audio_started", alarm.alarmId, alarm.activeAt)
                }
            }
            player.setOnErrorListener { failed, what, extra ->
                if (mediaPlayer === failed) {
                    mediaPlayer = null
                    failed.release()
                    AlarmDeliveryLog.record(this, "audio_source_failed", alarm.alarmId, alarm.activeAt, "$what/$extra")
                    playCandidate(alarm, candidates, index + 1)
                }
                true
            }
            player.setDataSource(this, candidates[index].toUri())
            player.prepareAsync()
        } catch (e: Exception) {
            player.release()
            mediaPlayer = null
            AlarmDeliveryLog.record(this, "audio_source_failed", alarm.alarmId, alarm.activeAt, e.message)
            playCandidate(alarm, candidates, index + 1)
        }
    }

    private fun stopAudioPlayback() {
        alarmVibrator?.cancel()
        alarmVibrator = null
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
        val vibratePattern = if (isPaused || !alarm.vibrate) null else longArrayOf(0, 100, 200, 300)
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
            if (alarm.snooze != 0) {
                addAction(getSnoozeAction(alarm))
            }
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
        val alarmJson = Base64.encodeToString(json.toByteArray(), Base64.URL_SAFE or Base64.NO_WRAP)
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
            data = "mathalarm://action/${alarm.alarmId}/${alarm.activeAt}/$intentAction".toUri()
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
        serviceScope.cancel()
        
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
        const val EXTRA_ALARM_ID = "alarm_id"
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
        fun stopAlarm(context: Context, alarmId: Long) {
            val intent = Intent(context, AlarmService::class.java).apply {
                action = ACTION_STOP_ALARM
                putExtra(EXTRA_ALARM_ID, alarmId)
            }
            if (ActiveAlarmManager.hasActiveAlarm()) context.startService(intent)
            AlarmDeliveryLog.record(context, "dismissed", alarmId)
        }
        
    }
}
