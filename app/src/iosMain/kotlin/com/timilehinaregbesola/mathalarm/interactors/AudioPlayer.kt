package com.timilehinaregbesola.mathalarm.interactors

import co.touchlab.kermit.Logger
import kotlinx.cinterop.ExperimentalForeignApi
import platform.AVFAudio.AVAudioPlayer
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryPlayback
import platform.AVFAudio.setActive
import platform.Foundation.NSBundle
import platform.Foundation.NSURL

actual interface AudioPlayer {
    actual val currentPosition: Int
    actual val duration: Int
    actual val isPlaying: Boolean

    actual fun init()
    actual fun startAlarmAudio()
    actual fun setPerceivedVolume(perceived: Float)
    actual fun stop()
    actual fun reset()
    actual fun setDataSourceFromString(alarmtone: String)
}

/**
 * iOS implementation of AudioPlayer using AVAudioPlayer
 */
@OptIn(ExperimentalForeignApi::class)
class IosAudioPlayer(
    private val logger: Logger
) : AudioPlayer {
    
    private var audioPlayer: AVAudioPlayer? = null
    private var dataSource: String = ""
    
    override val currentPosition: Int
        get() = (audioPlayer?.currentTime?.times(1000))?.toInt() ?: 0
    
    override val duration: Int
        get() = (audioPlayer?.duration?.times(1000))?.toInt() ?: 0
    
    override val isPlaying: Boolean
        get() = audioPlayer?.isPlaying() == true
    
    override fun init() {
        try {
            // Configure audio session for playback
            val audioSession = AVAudioSession.sharedInstance()
            audioSession.setCategory(AVAudioSessionCategoryPlayback, error = null)
            audioSession.setActive(true, error = null)
            logger.d { "Audio session initialized" }
        } catch (e: Exception) {
            logger.e(e) { "Error initializing audio session" }
        }
    }
    
    override fun startAlarmAudio() {
        try {
            if (dataSource.isEmpty()) {
                logger.w { "No data source set for audio player" }
                return
            }
            
            val url = when {
                dataSource.startsWith("http") || dataSource.startsWith("file://") -> {
                    NSURL.URLWithString(dataSource)
                }
                else -> {
                    // Try to load from bundle or use default alarm sound
                    NSBundle.mainBundle.URLForResource(dataSource, withExtension = null)
                }
            }
            
            url?.let {
                audioPlayer = AVAudioPlayer(contentsOfURL = it, error = null)
                audioPlayer?.numberOfLoops = -1 // Loop indefinitely for alarm
                audioPlayer?.prepareToPlay()
                audioPlayer?.play()
                logger.d { "Playing alarm audio: $dataSource" }
            } ?: run {
                logger.e { "Failed to create URL for audio source: $dataSource" }
            }
        } catch (e: Exception) {
            logger.e(e) { "Error playing alarm audio" }
        }
    }
    
    override fun setPerceivedVolume(perceived: Float) {
        audioPlayer?.volume = perceived.coerceIn(0f, 1f)
    }
    
    override fun stop() {
        audioPlayer?.stop()
        logger.d { "Audio stopped" }
    }
    
    override fun reset() {
        audioPlayer?.stop()
        audioPlayer = null
        dataSource = ""
        logger.d { "Audio player reset" }
    }
    
    override fun setDataSourceFromString(alarmtone: String) {
        dataSource = alarmtone
        logger.d { "Data source set: $alarmtone" }
    }
}
