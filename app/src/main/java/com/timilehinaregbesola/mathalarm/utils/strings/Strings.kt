package com.timilehinaregbesola.mathalarm.utils.strings

data class Strings(
    val multipleDays: String,
    val greeting: (hour: Int) -> String,
    val expand: String,
    val delete: String,
    val edit: String,
    val collapse: String,
    val noUpcomingAlarms: String,
    val nextAlarmText: String,
    val alarmSet: String,
    val alarmPermissionDialogConfirm: String,
    val alarmPermissionDialogCancel: String,
    val alarmPermissionDialogText: String,
    val alarmPermissionDialogTitle: String,
    val clearAlarmDialogConfirm: String,
    val clearAlarmDialogCancel: String,
    val clearAlarmDialogText: String,
    val clearAlarmDialogTitle: String,
    val emptyAlarmList: String,
    val nothingToSee: String,
    val alarms: String,
    val deleteAll: String,
    val settings: String,
    val toneUnavailable: String,
    val clear: String,
    val snooze: String,
    val enter: String,
    val noRingtonePicker: String,
    val alarmTitle: String,
    val goodDay: String,
    val defaultAlarmTone: String,
    val disabledNotificationMessage: String,
    val disabledNotificationMessageExtended: String,
    val ok: String,
    val notificationPermissionDialogMessage: String,
    val repeatWeekly: String,
    val vibrate: String,
    val testAlarm: String,
    val save: String,
    val easyMath: String,
    val mediumMath: String,
    val hardMath: String,
    val selectHour: String,
    val cancel: String,
    val input: String,
    val picker: String,
    val system: String,
    val dark: String,
    val light: String,
    val appSettings: String,
    val back: String,
    val colorTheme: String,
    val help: String,
    val sendFeedback: String,
    val sendFeedbackMessage: String,
    val supportEmail: String,
    val emailChooserTitle: String,
    val shareMathAlarm: String,
    val shareWithOthers: String,
    val share: String,
    val defaultSendText: String,
    val taskAlarmPermissionDialogText: String,
    val taskAlarmPermissionDialogConfirm: String,
    val taskAlarmPermissionDialogCancel: String,
    val notification: String,
    val alert: String,
    val grantPermission: String,
    val permissionsExternalStorageText: (tone: String) -> String
)

object Locales {
    const val EN = "en"
    const val ES = "es"
    const val DE = "de"
    const val RU = "ru"
    const val PT = "pt"
    const val HI = "hi"
    const val PA = "pa"
    const val BN = "bn"
    const val ZH = "zh"
}
