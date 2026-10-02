package com.timilehinaregbesola.mathalarm.platform

import androidx.compose.runtime.Composable
import androidx.compose.runtime.staticCompositionLocalOf

val LocalPlatformVibrator = staticCompositionLocalOf<PlatformVibrator?> { null }
fun interface RingtonePickerLauncher { fun launch(currentTone: String?) }
@Composable
expect fun rememberRingtonePickerLauncher(onResult: (String?) -> Unit): RingtonePickerLauncher?
@Composable
expect fun rememberNotificationPermissionHandler(onResult: (Boolean) -> Unit): () -> Unit
expect fun checkRingtonePermissions(tones: List<String>, unplayableDialogTitle: String,
    unplayableDialogMessage: (String) -> String)
expect fun getRingtoneTitle(alarmTone: String): String
expect fun openNotificationSettings()
expect fun requestExactAlarmPermission()
expect fun shareText(title: String, text: String)
expect fun sendEmail(chooserTitle: String, email: String, subject: String = "", body: String = "")
