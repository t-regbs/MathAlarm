package com.timilehinaregbesola.mathalarm.interactors

import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.ui.ExperimentalComposeUiApi
import com.google.accompanist.navigation.material.ExperimentalMaterialNavigationApi
import com.timilehinaregbesola.mathalarm.domain.model.Alarm
import com.timilehinaregbesola.mathalarm.notification.MathAlarmNotification
import kotlinx.coroutines.InternalCoroutinesApi
import timber.log.Timber

@ExperimentalFoundationApi
@ExperimentalMaterialApi
@ExperimentalComposeUiApi
@InternalCoroutinesApi
@ExperimentalAnimationApi
@ExperimentalMaterialNavigationApi
internal class NotificationInteractorImpl constructor(
    private val alarmNotification: MathAlarmNotification,
) : NotificationInteractor {

    override fun show(alarm: Alarm) {
        Timber.d("show - alarmId = ${alarm.alarmId}")
        if (alarm.repeat) {
            alarmNotification.showRepeating(alarm)
        } else {
            alarmNotification.show(alarm)
        }
    }

    override fun dismiss(notificationId: Long) {
        Timber.d("dismiss - alarmId = $notificationId")
        alarmNotification.dismiss(notificationId)
    }
}
