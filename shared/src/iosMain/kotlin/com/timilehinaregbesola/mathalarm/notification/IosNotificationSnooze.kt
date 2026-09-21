package com.timilehinaregbesola.mathalarm.notification

import co.touchlab.kermit.Logger
import com.timilehinaregbesola.mathalarm.framework.Usecases
import com.timilehinaregbesola.mathalarm.framework.NotificationSnoozeEvents
import com.timilehinaregbesola.mathalarm.framework.snoozeFromNotification
import com.timilehinaregbesola.mathalarm.interactors.IosAlarmAudioManager
import com.timilehinaregbesola.mathalarm.interactors.AudioPlayer
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import org.koin.core.component.KoinComponent
import org.koin.core.component.inject

/** Keeps the notification response alive until persistence and OS scheduling finish. */
object IosNotificationSnooze : KoinComponent {
    private val usecases: Usecases by inject()
    private val audioPlayer: AudioPlayer by inject()
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    // Null means success; a non-null result asks the delegate to open the challenge.
    fun snooze(alarmId: Long, completion: (String?) -> Unit) {
        scope.launch {
            val failure = try {
                if (usecases.snoozeFromNotification(alarmId)) {
                    audioPlayer.stop()
                    IosAlarmAudioManager.stopAlarm()
                    NotificationDeeplinkHolder.clearDeeplink()
                    NotificationSnoozeEvents.notifySnoozed(alarmId)
                    null
                } else "Snooze unavailable"
            } catch (e: Exception) {
                Logger.e(e) { "Unable to snooze notification" }
                "Unable to schedule snooze"
            }
            completion(failure)
        }
    }
}
