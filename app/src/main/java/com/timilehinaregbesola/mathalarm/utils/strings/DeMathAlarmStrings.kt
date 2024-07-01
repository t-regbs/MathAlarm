package com.timilehinaregbesola.mathalarm.utils.strings

import cafe.adriel.lyricist.LyricistStrings

@LyricistStrings(languageTag = Locales.DE, default = false)
val DeMathAlarmStrings = Strings(
    multipleDays = "Mehrere Tage",
    greeting = { hour ->
        when {
            hour < 12 -> "Guten Morgen"
            hour in 12..17 -> "Guten Tag"
            else -> "Guten Abend"
        }
    },
    expand = "Erweitern",
    delete = "Löschen",
    edit = "Bearbeiten",
    collapse = "Zusammenbruch",
    noUpcomingAlarms = "Keine bevorstehenden Alarme",
    nextAlarmText = "Nächster Alarm in",
    alarmSet = "Alarm gesetzt",
    alarmPermissionDialogConfirm = "Bestätigen",
    alarmPermissionDialogCancel = "Stornieren",
    alarmPermissionDialogText = "Diese App benötigt die Berechtigung zum Festlegen von Alarmen.",
    alarmPermissionDialogTitle = "Erforderliche Berechtigung",
    clearAlarmDialogConfirm = "Bestätigen",
    clearAlarmDialogCancel = "Stornieren",
    clearAlarmDialogText = "Sind Sie sicher, dass Sie die Alarme löschen möchten?",
    clearAlarmDialogTitle = "Klare Alarme",
    emptyAlarmList = "Leere Alarmliste",
    nothingToSee = "Hier gibt es nichts zu sehen",
    alarms = "Alarme",
    deleteAll = "Alle löschen",
    settings = "Einstellungen",
    toneUnavailable = "Ton nicht verfügbar",
    clear = "Klar",
    snooze = "Schlummern",
    enter = "Eintreten",
    noRingtonePicker = "Kein Klingeltonauswahl",
    alarmTitle = "Alarmtitel",
    goodDay = "Guten Tag",
    defaultAlarmTone = "Standard-Alarmton",
    disabledNotificationMessage = "Benachrichtigungen sind für diese App deaktiviert.",
    ok = "OK",
    notificationPermissionDialogMessage = "Diese App benötigt die Berechtigung zum Anzeigen von Benachrichtigungen.",
    repeatWeekly = "Wöchentlich wiederholen",
    vibrate = "Vibrieren",
    testAlarm = "Testalarm",
    save = "Sparen",
    easyMath = "Einfache Mathematik",
    mediumMath = "Mittlere Mathematik",
    hardMath = "Schwere Mathematik",
    selectHour = "Stunde auswählen",
    cancel = "Stornieren",
    input = "Eingang",
    picker = "Picker",
    system = "System",
    dark = "Dunkel",
    light = "Licht",
    appSettings = "App-Einstellungen",
    back = "Zurück",
    colorTheme = "Farbthema",
    help = "Hilfe",
    sendFeedback = "Feedback senden",
    sendFeedbackMessage = "Feedback an den Entwickler senden",
    supportEmail = "aregbestimi@gmail.com",
    emailChooserTitle = "E-Mail senden",
    shareMathAlarm = "Mathematik-Alarm teilen",
    shareWithOthers = "Dies ist eine coole Alarm-App",
    share = "Teilen",
    defaultSendText = "Standard-Alarm",
    disabledNotificationMessageExtended = "Benachrichtigungen dieser Anwendung sind deaktiviert. Bitte gehen Sie zu den Geräteeinstellungen und aktivieren Sie sie.",
    taskAlarmPermissionDialogText = "Damit Alarme funktionieren, benötigt Math Alarm die Berechtigung zum Einstellen von Alarmen. Wird diese Berechtigung nicht erteilt, funktionieren auch die vorhandenen Alarme nicht.",
    taskAlarmPermissionDialogConfirm = "Bestätigen",
    taskAlarmPermissionDialogCancel = "Nicht jetzt",
    notification = "Benachrichtigung",
    alert = "Klingelton",
    grantPermission = "Berechtigung erteilen",
    permissionsExternalStorageText = { tone ->
        "Es scheint, dass wir $tone nicht abspielen können, wahrscheinlich weil eine Erlaubnis erforderlich ist. Wenn Sie möchten, können Sie die Erlaubnis erteilen. Alternativ können Sie einen anderen Ton auswählen. Diese Entscheidung kann in den Systemeinstellungen geändert werden."
    }
)