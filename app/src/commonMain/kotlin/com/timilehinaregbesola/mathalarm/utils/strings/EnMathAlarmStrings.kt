package com.timilehinaregbesola.mathalarm.utils.strings

import cafe.adriel.lyricist.LyricistStrings

@LyricistStrings(languageTag = Locales.EN, default = true)
val EnMathAlarmStrings = Strings(
    multipleDays = "Multiple Days",
    greeting = { hour ->
        when {
            hour < 12 -> "Good Morning"
            hour in 12..17 -> "Afternoon"
            else -> "Good Evening"
        }
    },
    expand = "Expand",
    delete = "Delete",
    edit = "Edit",
    collapse = "Collapse",
    noUpcomingAlarms = "No Upcoming Alarms",
    nextAlarmText = "Next Alarm in",
    alarmSet = "Alarm Set for",
    alarmPermissionDialogConfirm = "Confirm",
    alarmPermissionDialogCancel = "Cancel",
    alarmPermissionDialogText = "This app requires permission to set alarms.",
    alarmPermissionDialogTitle = "Permission Required",
    clearAlarmDialogConfirm = "Confirm",
    clearAlarmDialogCancel = "Cancel",
    clearAlarmDialogText = "Are you sure you want to clear the alarms?",
    clearAlarmDialogTitle = "Clear Alarms",
    emptyAlarmList = "Empty Alarm List",
    nothingToSee = "Nothing to see here",
    alarms = "Alarms",
    deleteAll = "Delete All",
    settings = "Settings",
    toneUnavailable = "Tone Unavailable",
    clear = "Clear",
    snooze = "Snooze",
    enter = "Enter",
    noRingtonePicker = "No Ringtone Picker",
    alarmTitle = "Alarm Title",
    goodDay = "Good day",
    defaultAlarmTone = "Alarm Tone (Default)",
    disabledNotificationMessage = "Notifications are disabled for this app.",
    disabledNotificationMessageExtended = "Notifications of this application are disabled. Please go to your device settings and enable them.",
    ok = "OK",
    notificationPermissionDialogMessage = "This app requires permission to show notifications.",
    repeatWeekly = "Repeat Weekly",
    vibrate = "Vibrate",
    testAlarm = "Test Alarm",
    save = "Save",
    easyMath = "Easy Math",
    mediumMath = "Medium Math",
    hardMath = "Hard Math",
    selectHour = "Select Hour",
    cancel = "Cancel",
    input = "Input",
    picker = "Picker",
    system = "System",
    dark = "Dark",
    light = "Light",
    appSettings = "App Settings",
    back = "Back",
    colorTheme = "Color Theme",
    help = "Help",
    sendFeedback = "Send Feedback",
    sendFeedbackMessage = "Send feedback to the developer",
    supportEmail = "aregbestimi@gmail.com",
    emailChooserTitle = "Send Email",
    defaultSendText = "Default Alarm",
    shareMathAlarm = "Share Math Alarm",
    shareWithOthers = "Check out this cool alarm app",
    share = "Share",
    taskAlarmPermissionDialogText = "In order for alarms to work, Math alarm needs the permission to set alarms. If this permission is not granted, the existing alarms will not work as well.",
    taskAlarmPermissionDialogCancel = "Not now",
    taskAlarmPermissionDialogConfirm = "Grant",
    notification = "Notification",
    alert = "Ringtone",
    grantPermission = "Grant Permission",
    permissionsExternalStorageText = { tone ->
        "It seems that we cannot play $tone, probably because a permission is required. If you want, you can grant the permission. Alternatively, select a different sound. This decision can be changed in System Settings."
    }
)