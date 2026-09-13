package com.timilehinaregbesola.mathalarm.interactors

import co.touchlab.kermit.Logger
import kotlinx.cinterop.ExperimentalForeignApi
import platform.AVFAudio.AVAudioPlayer
import platform.AVFAudio.AVAudioSession
import platform.AVFAudio.AVAudioSessionCategoryPlayback
import platform.AVFAudio.setActive
import platform.Foundation.NSBundle
import platform.Foundation.NSURL

/**
 * iOS implementation of AudioPlayer using AVAudioPlayer
 */
@OptIn(ExperimentalForeignApi::class)
class IosAudioPlayer(
    private val logger: Logger
) : AudioPlayer {

    private var audioPlayer: AVAudioPlayer? = null
    private var dataSource: String = ""

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
                    bundledSoundUrl(dataSource)
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

    private fun bundledSoundUrl(soundName: String): NSURL? {
        val resourceName = soundName.substringBeforeLast(".", soundName)
        val resourceExtension = soundName.substringAfterLast(".", missingDelimiterValue = "")

        if (resourceExtension.isNotEmpty()) {
            NSBundle.mainBundle.URLForResource(resourceName, withExtension = resourceExtension)?.let {
                return it
            }
        }

        listOf("wav", "caf", "aiff", "mp3", "m4a", "aac").forEach { ext ->
            NSBundle.mainBundle.URLForResource(resourceName, withExtension = ext)?.let {
                return it
            }
        }

        return NSBundle.mainBundle.URLForResource(resourceName, withExtension = null)
    }
}
