package com.timilehinaregbesola.mathalarm.interactors

import co.touchlab.kermit.Logger
import com.timilehinaregbesola.mathalarm.sound.AlarmSoundCatalog
import com.timilehinaregbesola.mathalarm.sound.TonePreviewOwnership
import com.timilehinaregbesola.mathalarm.sound.TonePreviewResult
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCObjectVar
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.value
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import platform.AVFAudio.AVAudioPlayer
import platform.AVFAudio.AVAudioPlayerDelegateProtocol
import platform.Foundation.NSError
import platform.darwin.NSObject
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryPlayback
import platform.AVFAudio.AVAudioSessionModeDefault
import platform.AVFAudio.AVAudioSessionCategoryOptionMixWithOthers
import platform.AVFAudio.setActive
import platform.AVFAudio.AVAudioSessionInterruptionNotification
import platform.AVFAudio.AVAudioSessionInterruptionTypeKey
import platform.AVFAudio.AVAudioSessionInterruptionTypeBegan
import platform.AVFAudio.AVAudioSessionInterruptionTypeEnded
import platform.AVFAudio.AVAudioSessionRouteChangeNotification
import platform.AVFAudio.AVAudioSessionMediaServicesWereResetNotification
import platform.AudioToolbox.AudioServicesPlaySystemSound
import platform.AudioToolbox.kSystemSoundID_Vibrate
import platform.Foundation.NSBundle
import platform.Foundation.NSURL
import platform.Foundation.NSNotificationCenter
import platform.Foundation.NSOperationQueue
import platform.Foundation.NSNumber
import platform.UIKit.UIApplicationDidBecomeActiveNotification
import platform.UIKit.UIImpactFeedbackGenerator
import platform.UIKit.UIImpactFeedbackStyle

/**
 * iOS Alarm Audio Manager
 * 
 * Handles continuous alarm audio playback with vibration.
 * This is a singleton that manages the alarm state independently of the UI.
 */
@OptIn(ExperimentalForeignApi::class, kotlinx.cinterop.BetaInteropApi::class,
    kotlin.experimental.ExperimentalObjCRefinement::class)
@kotlin.native.HiddenFromObjC
object IosAlarmAudioManager {
    private val logger = Logger.withTag("IosAlarmAudioManager")
    private var audioPlayer: AVAudioPlayer? = null
    private var previewPlayer: AVAudioPlayer? = null
    private val previewOwnership = TonePreviewOwnership()
    // AVAudioPlayer holds its delegate weakly; retain it for the manager's lifetime.
    private val previewDelegate = object : NSObject(), AVAudioPlayerDelegateProtocol {
        override fun audioPlayerDidFinishPlaying(player: AVAudioPlayer, successfully: Boolean) {
            if (player == previewPlayer) stopPreview(TonePreviewResult.FINISHED)
        }

        override fun audioPlayerDecodeErrorDidOccur(player: AVAudioPlayer, error: NSError?) {
            if (player == previewPlayer) stopPreview(TonePreviewResult.UNAVAILABLE)
        }
    }
    private var vibrationJob: Job? = null
    private var soundJob: Job? = null
    private var playbackRetryJob: Job? = null
    private var isAlarmActive = false
    private var requestedSound = ""
    private var requestedVolume = 1.0f
    private val playbackRecovery = IosAlarmPlaybackRecovery(
        isPlaying = { audioPlayer?.playing == true || soundJob?.isActive == true },
        activate = ::configureAudioSession,
        play = { playSound(requestedSound, requestedVolume) },
    )
    // The singleton and its observers have application lifetime, independent of a view.
    private val playbackObservers = mutableListOf<Any>()
    private var guideOwnerId: String? = null
    private var guideInterrupted: (() -> Unit)? = null
    private val scope = CoroutineScope(Dispatchers.Main)
    

    private fun bundledSoundUrl(soundName: String): NSURL? {
        val name = AlarmSoundCatalog.iosResourceName(soundName)
        for (candidate in listOf(name, AlarmSoundCatalog.DEFAULT_SOUND).distinct()) {
            for (extension in listOf("caf", "wav", "m4a", "mp3", "aiff")) {
                NSBundle.mainBundle.URLForResource(candidate, withExtension = extension)?.let { return it }
            }
        }
        return null
    }

    fun startPreview(soundName: String, onFinished: () -> Unit) {
        startOwnedPreview("legacy-tone-preview", soundName) { onFinished() }
    }

    fun startOwnedPreview(ownerId: String, soundName: String, onFinished: (TonePreviewResult) -> Unit) {
        val request = previewOwnership.nextRequest()
        if (previewOwnership.rejectWhileAlarmActive(isAlarmActive, onFinished)) return
        interruptGuideAudio()
        stopPreview(TonePreviewResult.REPLACED)
        // Completing the replaced preview can synchronously request another preview
        // or start a real alarm. That newer request wins; this call must not overwrite it.
        if (!previewOwnership.isCurrentRequest(request)) { onFinished(TonePreviewResult.REPLACED); return }
        if (previewOwnership.rejectWhileAlarmActive(isAlarmActive, onFinished)) return
        previewOwnership.begin(ownerId, onFinished)
        try {
            val url = bundledSoundUrl(soundName)
            if (url == null) { stopPreview(TonePreviewResult.UNAVAILABLE); return }
            if (!configureAudioSession()) { stopPreview(TonePreviewResult.UNAVAILABLE); return }
            val player = AVAudioPlayer(contentsOfURL = url, error = null)
            previewPlayer = player
            player.delegate = previewDelegate
            player.numberOfLoops = 0
            player.volume = 0.75f
            if (!player.prepareToPlay() || !player.play()) stopPreview(TonePreviewResult.UNAVAILABLE)
        } catch (error: Exception) {
            stopPreview(TonePreviewResult.UNAVAILABLE)
            logger.e(error) { "Unable to preview alarm sound" }
        }
    }

    fun stopOwnedPreview(ownerId: String) {
        if (previewOwnership.owns(ownerId)) stopPreview()
    }

    fun stopPreview(result: TonePreviewResult = TonePreviewResult.STOPPED) {
        val hadPreview = previewPlayer != null
        previewPlayer?.delegate = null
        previewPlayer?.stop()
        previewPlayer = null
        if (hadPreview && !isAlarmActive && guideOwnerId == null) AVAudioSession.sharedInstance().setActive(false, error = null)
        previewOwnership.finish(result)
    }

    /**
     * Start playing alarm audio
     * 
     * @param soundName The name of the sound to play (from AlarmSoundCatalog), or empty for default
     * @param vibrate Whether to also vibrate
     * @param volume Volume level 0.0 to 1.0
     */
    fun startAlarm(soundName: String = "", vibrate: Boolean = true, volume: Float = 1.0f) {
        if (isAlarmActive) {
            logger.d { "Alarm already active, restarting..." }
            stopAlarm()
        }
        
        isAlarmActive = true
        requestedSound = soundName
        requestedVolume = volume
        installPlaybackRecoveryObservers()
        playbackRecovery.begin()
        interruptGuideAudio()
        previewOwnership.nextRequest()
        // Establish real priority before completion callbacks can request playback.
        stopPreview(TonePreviewResult.INTERRUPTED_BY_ALARM)
        logger.d { "Starting alarm: sound=$soundName, vibrate=$vibrate" }
        
        // A background Stop intent can initialize before AlarmKit releases audio.
        // Keep the request pending if activation/play is rejected; foreground and
        // interruption-end notifications retry the same player without resetting progress.
        resumeRequestedPlayback()
        
        // Start vibration pattern if enabled
        if (vibrate) {
            startVibrationPattern()
        }
    }
    
    /**
     * Stop the alarm
     */
    fun stopAlarm() {
        playbackRecovery.stop()
        playbackRetryJob?.cancel()
        playbackRetryJob = null
        if (!isAlarmActive && audioPlayer == null && vibrationJob == null && soundJob == null) return
        logger.d { "Stopping alarm" }
        isAlarmActive = false
        
        // Stop audio
        audioPlayer?.stop()
        audioPlayer = null
        
        // Stop vibration
        vibrationJob?.cancel()
        vibrationJob = null
        soundJob?.cancel()
        soundJob = null
        
        // Deactivate audio session
        try {
            if (previewPlayer == null && guideOwnerId == null) AVAudioSession.sharedInstance().setActive(false, error = null)
        } catch (e: Exception) {
            logger.e(e) { "Error deactivating audio session" }
        }
    }
    
    /**
     * Check if alarm is currently active
     */
    fun isPlaying(): Boolean = isAlarmActive

    /** Silent PiP still needs a playback session. Real alarms and tone previews win. */
    fun beginSettingsGuideAudio(ownerId: String, onInterrupted: () -> Unit): Boolean {
        require(ownerId.isNotBlank())
        if (isAlarmActive || previewPlayer != null || guideOwnerId != null) return false
        val session = AVAudioSession.sharedInstance()
        if (!session.setCategory(AVAudioSessionCategoryPlayback, mode = AVAudioSessionModeDefault,
                options = AVAudioSessionCategoryOptionMixWithOthers, error = null)) return false
        if (!session.setActive(true, error = null)) return false
        guideOwnerId = ownerId
        guideInterrupted = onInterrupted
        return true
    }

    fun endSettingsGuideAudio(ownerId: String): Boolean {
        if (guideOwnerId != ownerId) return false
        guideOwnerId = null
        guideInterrupted = null
        if (!isAlarmActive && previewPlayer == null) AVAudioSession.sharedInstance().setActive(false, error = null)
        return true
    }

    private fun interruptGuideAudio() {
        val ownerId = guideOwnerId ?: return
        val callback = guideInterrupted
        endSettingsGuideAudio(ownerId)
        callback?.invoke()
    }
    
    /**
     * Configure audio session for alarm playback
     * Ensures audio plays even in silent mode and mixes with other audio
     */
    private fun configureAudioSession(): Boolean = try {
        memScoped {
            val session = AVAudioSession.sharedInstance()
            val error = alloc<ObjCObjectVar<NSError?>>()
            error.value = null
            if (!session.setCategory(
                AVAudioSessionCategoryPlayback,
                mode = AVAudioSessionModeDefault,
                options = 0u, // No mixing - alarm should be prominent
                error = error.ptr,
            )) {
                logger.w { "Alarm audio category rejected: ${error.value?.code} ${error.value?.localizedDescription}" }
                return@memScoped false
            }
            error.value = null
            if (!session.setActive(true, error = error.ptr)) {
                logger.w { "Alarm audio activation rejected: ${error.value?.code} ${error.value?.localizedDescription}" }
                return@memScoped false
            }
            logger.d { "Audio session configured for alarm" }
            true
        }
    } catch (e: Exception) {
        logger.e(e) { "Error configuring audio session" }
        false
    }

    private fun resumeRequestedPlayback() {
        val token = playbackRecovery.request ?: return
        playbackRetryJob?.cancel()
        playbackRetryJob = null
        if (playbackRecovery.attempt(token) || playbackRecovery.interrupted) return
        playbackRetryJob = scope.launch {
            repeat(12) {
                delay(250)
                if (!playbackRecovery.owns(token) || playbackRecovery.interrupted) return@launch
                if (playbackRecovery.attempt(token)) return@launch
            }
            if (playbackRecovery.owns(token)) logger.w { "Alarm audio remains pending after startup retries" }
        }
    }

    private fun installPlaybackRecoveryObservers() {
        if (playbackObservers.isNotEmpty()) return
        val center = NSNotificationCenter.defaultCenter
        playbackObservers += center.addObserverForName(UIApplicationDidBecomeActiveNotification,
            `object` = null, queue = NSOperationQueue.mainQueue) { resumeRequestedPlayback() }
        playbackObservers += center.addObserverForName(AVAudioSessionInterruptionNotification,
            `object` = null, queue = NSOperationQueue.mainQueue) { notification ->
            when ((notification?.userInfo?.get(AVAudioSessionInterruptionTypeKey) as? NSNumber)?.unsignedLongLongValue) {
                AVAudioSessionInterruptionTypeBegan -> {
                    playbackRecovery.interruptionBegan()
                    playbackRetryJob?.cancel()
                    playbackRetryJob = null
                }
                AVAudioSessionInterruptionTypeEnded -> {
                    playbackRecovery.interruptionEnded()
                    resumeRequestedPlayback()
                }
            }
        }
        playbackObservers += center.addObserverForName(AVAudioSessionRouteChangeNotification,
            `object` = null, queue = NSOperationQueue.mainQueue) { resumeRequestedPlayback() }
        playbackObservers += center.addObserverForName(AVAudioSessionMediaServicesWereResetNotification,
            `object` = null, queue = NSOperationQueue.mainQueue) {
            if (playbackRecovery.request != null) {
                audioPlayer = null
                playbackRecovery.interruptionEnded()
                resumeRequestedPlayback()
            }
        }
    }
    
    /**
     * Play the alarm sound in a loop
     */
    private fun playSound(soundName: String, volume: Float): Boolean {
        return try {
            audioPlayer?.let { return it.play() }
            var soundUrl = bundledSoundUrl(soundName)

            // If bundled sounds are missing, try system sounds
            if (soundUrl == null) {
                logger.d { "Custom sound not found, trying system sounds..." }
                // Try various system alarm sound paths (iOS simulator and device paths)
                val systemPaths = listOf(
                    "/System/Library/Audio/UISounds/alarm.caf",
                    "/System/Library/Audio/UISounds/New/Alarm.caf",
                    "/System/Library/Audio/UISounds/nano/Alarm_Nightstand_Haptic.caf",
                    "/System/Library/Audio/UISounds/new-mail.caf"
                )
                
                for (path in systemPaths) {
                    val testUrl = NSURL.fileURLWithPath(path)
                    val testPlayer = AVAudioPlayer(contentsOfURL = testUrl, error = null)
                    if (testPlayer != null) {
                        soundUrl = testUrl
                        logger.d { "Found system sound: $path" }
                        break
                    }
                }
            }
            
            if (soundUrl != null) {
                audioPlayer = AVAudioPlayer(contentsOfURL = soundUrl, error = null)
                val player = requireNotNull(audioPlayer)
                player.apply {
                    numberOfLoops = -1 // Loop indefinitely
                    this.volume = volume
                }
                val accepted = player.prepareToPlay() && player.play()
                if (accepted) logger.d { "Playing alarm sound" }
                else logger.w { "Alarm audio player rejected playback; retaining request" }
                accepted
            } else {
                logger.w { "Could not find any alarm sound, using system beep fallback" }
                startSystemSoundFallback()
                true
            }
        } catch (e: Exception) {
            logger.e(e) { "Error playing alarm sound: ${e.message}" }
            startSystemSoundFallback()
            true
        }
    }
    
    /**
     * Fallback to system sound if no audio file available
     */
    private fun startSystemSoundFallback() {
        soundJob = scope.launch {
            while (isAlarmActive) {
                // Play system alert sound repeatedly
                AudioServicesPlaySystemSound(1005u) // System alert sound
                delay(1000)
            }
        }
    }
    
    /**
     * Start a vibration pattern for the alarm
     */
    private fun startVibrationPattern() {
        vibrationJob = scope.launch {
            val feedbackGenerator = UIImpactFeedbackGenerator(style = UIImpactFeedbackStyle.UIImpactFeedbackStyleHeavy)
            feedbackGenerator.prepare()
            
            while (isAlarmActive) {
                // Vibrate using both methods for stronger effect
                AudioServicesPlaySystemSound(kSystemSoundID_Vibrate)
                feedbackGenerator.impactOccurred()
                delay(800) // Vibrate every 800ms
            }
        }
    }
    
}
