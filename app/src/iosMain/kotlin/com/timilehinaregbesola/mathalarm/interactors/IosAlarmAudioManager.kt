package com.timilehinaregbesola.mathalarm.interactors

import co.touchlab.kermit.Logger
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import platform.AVFAudio.AVAudioPlayer
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryPlayback
import platform.AVFAudio.AVAudioSessionCategoryOptionMixWithOthers
import platform.AVFAudio.AVAudioSessionModeDefault
import platform.AVFAudio.setActive
import platform.AudioToolbox.AudioServicesPlaySystemSound
import platform.AudioToolbox.kSystemSoundID_Vibrate
import platform.Foundation.NSBundle
import platform.Foundation.NSURL
import platform.UIKit.UIImpactFeedbackGenerator
import platform.UIKit.UIImpactFeedbackStyle

/**
 * iOS Alarm Audio Manager
 * 
 * Handles continuous alarm audio playback with vibration.
 * This is a singleton that manages the alarm state independently of the UI.
 */
@OptIn(ExperimentalForeignApi::class)
object IosAlarmAudioManager {
    private val logger = Logger.withTag("IosAlarmAudioManager")
    private var audioPlayer: AVAudioPlayer? = null
    private var vibrationJob: Job? = null
    private var isAlarmActive = false
    private val scope = CoroutineScope(Dispatchers.Main)
    
    // Bundled alarm sounds (filename without extension)
    val availableSounds = listOf(
        AlarmSound("alarm_classic", "Classic Alarm"),
        AlarmSound("alarm_gentle", "Gentle Wake"),
        AlarmSound("alarm_digital", "Digital Beep"),
        AlarmSound("alarm_nature", "Nature Morning"),
        AlarmSound("alarm_urgent", "Urgent"),
    )
    
    data class AlarmSound(
        val filename: String,
        val displayName: String
    )
    
    /**
     * Start playing alarm audio
     * 
     * @param soundName The name of the sound to play (from availableSounds), or empty for default
     * @param vibrate Whether to also vibrate
     * @param volume Volume level 0.0 to 1.0
     */
    fun startAlarm(soundName: String = "", vibrate: Boolean = true, volume: Float = 1.0f) {
        if (isAlarmActive) {
            logger.d { "Alarm already active, restarting..." }
            stopAlarm()
        }
        
        isAlarmActive = true
        logger.d { "Starting alarm: sound=$soundName, vibrate=$vibrate" }
        
        // Configure audio session for alarm playback
        configureAudioSession()
        
        // Start audio playback
        playSound(soundName, volume)
        
        // Start vibration pattern if enabled
        if (vibrate) {
            startVibrationPattern()
        }
    }
    
    /**
     * Stop the alarm
     */
    fun stopAlarm() {
        logger.d { "Stopping alarm" }
        isAlarmActive = false
        
        // Stop audio
        audioPlayer?.stop()
        audioPlayer = null
        
        // Stop vibration
        vibrationJob?.cancel()
        vibrationJob = null
        
        // Deactivate audio session
        try {
            AVAudioSession.sharedInstance().setActive(false, error = null)
        } catch (e: Exception) {
            logger.e(e) { "Error deactivating audio session" }
        }
    }
    
    /**
     * Check if alarm is currently active
     */
    fun isPlaying(): Boolean = isAlarmActive
    
    /**
     * Configure audio session for alarm playback
     * Ensures audio plays even in silent mode and mixes with other audio
     */
    private fun configureAudioSession() {
        try {
            val session = AVAudioSession.sharedInstance()
            session.setCategory(
                AVAudioSessionCategoryPlayback,
                mode = AVAudioSessionModeDefault,
                options = 0u, // No mixing - alarm should be prominent
                error = null
            )
            session.setActive(true, error = null)
            logger.d { "Audio session configured for alarm" }
        } catch (e: Exception) {
            logger.e(e) { "Error configuring audio session" }
        }
    }
    
    /**
     * Play the alarm sound in a loop
     */
    private fun playSound(soundName: String, volume: Float) {
        try {
            // Try to find the sound in the app bundle
            val soundFile = if (soundName.isNotEmpty()) soundName else "alarm_classic"
            
            // Try different extensions
            val extensions = listOf("caf", "m4a", "mp3", "wav", "aiff")
            var soundUrl: NSURL? = null
            
            for (ext in extensions) {
                soundUrl = NSBundle.mainBundle.URLForResource(soundFile, withExtension = ext)
                if (soundUrl != null) {
                    logger.d { "Found sound file: $soundFile.$ext" }
                    break
                }
            }
            
            // If custom sound not found, try system sounds
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
                audioPlayer?.apply {
                    numberOfLoops = -1 // Loop indefinitely
                    this.volume = volume
                    prepareToPlay()
                    play()
                }
                logger.d { "Playing alarm sound" }
            } else {
                logger.w { "Could not find any alarm sound, using system beep fallback" }
                startSystemSoundFallback()
            }
        } catch (e: Exception) {
            logger.e(e) { "Error playing alarm sound: ${e.message}" }
            startSystemSoundFallback()
        }
    }
    
    /**
     * Fallback to system sound if no audio file available
     */
    private fun startSystemSoundFallback() {
        scope.launch {
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
        vibrationJob = scope.launch(Dispatchers.IO) {
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
    
    /**
     * Snooze the alarm for the specified duration
     * 
     * @param minutes Snooze duration in minutes
     * @param onSnoozeEnd Callback when snooze ends
     */
    fun snooze(minutes: Int = 5, onSnoozeEnd: () -> Unit = {}) {
        stopAlarm()
        logger.d { "Alarm snoozed for $minutes minutes" }
        
        scope.launch {
            delay(minutes * 60 * 1000L)
            if (!isAlarmActive) {
                onSnoozeEnd()
            }
        }
    }
}
