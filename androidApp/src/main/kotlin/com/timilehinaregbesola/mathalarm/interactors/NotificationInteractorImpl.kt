package com.timilehinaregbesola.mathalarm.interactors

import android.content.Context
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.ui.ExperimentalComposeUiApi
import co.touchlab.kermit.Logger
import com.timilehinaregbesola.mathalarm.domain.model.Alarm
import com.timilehinaregbesola.mathalarm.notification.AlarmService
import kotlinx.coroutines.InternalCoroutinesApi

@ExperimentalFoundationApi
@ExperimentalMaterial3Api
@ExperimentalComposeUiApi
@InternalCoroutinesApi
@ExperimentalAnimationApi
internal class NotificationInteractorImpl(
    private val context: Context,
    private val logger: Logger
) : NotificationInteractor {

    override fun show(alarm: Alarm) {
        logger.d("show - alarmId = ${alarm.alarmId}, starting AlarmService")
        AlarmService.startAlarm(context, alarm)
    }

    override fun dismiss(notificationId: Long) {
        logger.d("dismiss - alarmId = $notificationId, stopping AlarmService")
        AlarmService.stopAlarm(context)
    }
}
