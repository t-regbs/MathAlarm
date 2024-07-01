package com.timilehinaregbesola.mathalarm.utils.strings

import cafe.adriel.lyricist.LyricistStrings

@LyricistStrings(languageTag = Locales.PA)
val PaMathAlarmStrings = Strings(
    multipleDays = "ਕਈ ਦਿਨ",
    greeting = { hour ->
        when {
            hour < 12 -> "ਸ਼ੁਭ ਸਵੇਰ"
            hour in 12..17 -> "ਦੁਪਹਿਰ"
            else -> "ਸ਼ੁਭ ਸ਼ਾਮ"
        }
    },
    expand = "ਵਿਸਤਾਰ",
    delete = "ਹਟਾਓ",
    edit = "ਸੋਧੋ",
    collapse = "ਸੰਕੁਚਿਤ",
    noUpcomingAlarms = "ਕੋਈ ਆਉਣ ਵਾਲੇ ਅਲਾਰਮ ਨਹੀਂ",
    nextAlarmText = "ਅਗਲਾ ਅਲਾਰਮ",
    alarmSet = "ਅਲਾਰਮ ਸੈੱਟ ਕੀਤਾ ਗਿਆ",
    alarmPermissionDialogConfirm = "ਪੁਸ਼ਟੀ ਕਰੋ",
    alarmPermissionDialogCancel = "ਰੱਦ ਕਰੋ",
    alarmPermissionDialogText = "ਇਸ ਐਪ ਨੂੰ ਅਲਾਰਮ ਸੈੱਟ ਕਰਨ ਦੀ ਆਗਿਆ ਦੀ ਲੋੜ ਹੈ।",
    alarmPermissionDialogTitle = "ਆਗਿਆ ਲੋੜੀਂਦੀ ਹੈ",
    clearAlarmDialogConfirm = "ਪੁਸ਼ਟੀ ਕਰੋ",
    clearAlarmDialogCancel = "ਰੱਦ ਕਰੋ",
    clearAlarmDialogText = "ਕੀ ਤੁਸੀਂ ਇਹ ਅਲਾਰਮ ਸਾਫ਼ ਕਰਨਾ ਚਾਹੁੰਦੇ ਹੋ?",
    clearAlarmDialogTitle = "ਅਲਾਰਮ ਸਾਫ਼ ਕਰੋ",
    emptyAlarmList = "ਖਾਲੀ ਅਲਾਰਮ ਸੂਚੀ",
    nothingToSee = "ਇਥੇ ਕੁਝ ਨਹੀਂ",
    alarms = "ਅਲਾਰਮ",
    deleteAll = "ਸਭ ਨੂੰ ਹਟਾਓ",
    settings = "ਸੈਟਿੰਗਜ਼",
    toneUnavailable = "ਟੋਨ ਉਪਲਬਧ ਨਹੀਂ",
    clear = "ਸਾਫ਼ ਕਰੋ",
    snooze = "ਸਨੂਜ਼",
    enter = "ਦਾਖਲ ਕਰੋ",
    noRingtonePicker = "ਕੋਈ ਰਿੰਗਟੋਨ ਚੁਣਨ ਵਾਲਾ ਨਹੀਂ",
    alarmTitle = "ਅਲਾਰਮ ਸਿਰਲੇਖ",
    goodDay = "ਸ਼ੁਭ ਦਿਨ",
    defaultAlarmTone = "ਅਲਾਰਮ ਟੋਨ (ਡਿਫਾਲਟ)",
    disabledNotificationMessage = "ਇਸ ਐਪ ਲਈ ਨੋਟੀਫਿਕੇਸ਼ਨ ਅਸਮਰੱਥ ਹਨ।",
    disabledNotificationMessageExtended = "ਇਸ ਐਪਲੀਕੇਸ਼ਨ ਦੀਆਂ ਨੋਟੀਫਿਕੇਸ਼ਨ ਅਸਮਰੱਥ ਹਨ। ਕਿਰਪਾ ਕਰਕੇ ਆਪਣੇ ਡਿਵਾਈਸ ਸੈਟਿੰਗਜ਼ ਵਿੱਚ ਜਾਓ ਅਤੇ ਉਹਨਾਂ ਨੂੰ ਸਮਰੱਥ ਕਰੋ।",
    ok = "ਠੀਕ ਹੈ",
    notificationPermissionDialogMessage = "ਇਸ ਐਪ ਨੂੰ ਨੋਟੀਫਿਕੇਸ਼ਨ ਦਿਖਾਉਣ ਦੀ ਆਗਿਆ ਦੀ ਲੋੜ ਹੈ।",
    repeatWeekly = "ਹਫਤਾਵਾਰੀ ਦੁਹਰਾਓ",
    vibrate = "ਵਾਈਬ੍ਰੇਟ",
    testAlarm = "ਪਰੀਖਣ ਅਲਾਰਮ",
    save = "ਸੰਭਾਲੋ",
    easyMath = "ਸੌਖੀ ਗਣਿਤ",
    mediumMath = "ਦਰਮਿਆਨੀ ਗਣਿਤ",
    hardMath = "ਕਠਿਨ ਗਣਿਤ",
    selectHour = "ਘੰਟਾ ਚੁਣੋ",
    cancel = "ਰੱਦ ਕਰੋ",
    input = "ਇਨਪੁੱਟ",
    picker = "ਚੁਣਨ ਵਾਲਾ",
    system = "ਸਿਸਟਮ",
    dark = "ਡਾਰਕ",
    light = "ਲਾਈਟ",
    appSettings = "ਐਪ ਸੈਟਿੰਗਜ਼",
    back = "ਪਿੱਛੇ",
    colorTheme = "ਰੰਗ ਥੀਮ",
    help = "ਮਦਦ",
    sendFeedback = "ਪ੍ਰਤੀਕ੍ਰਿਆ ਭੇਜੋ",
    sendFeedbackMessage = "ਡਿਵੈਲਪਰ ਨੂੰ ਪ੍ਰਤੀਕ੍ਰਿਆ ਭੇਜੋ",
    supportEmail = "aregbestimi@gmail.com",
    emailChooserTitle = "ਈਮੇਲ ਭੇਜੋ",
    defaultSendText = "ਡਿਫਾਲਟ ਅਲਾਰਮ",
    shareMathAlarm = "ਗਣਿਤ ਅਲਾਰਮ ਸਾਂਝਾ ਕਰੋ",
    shareWithOthers = "ਇਸ ਸ਼ਾਨਦਾਰ ਅਲਾਰਮ ਐਪ ਨੂੰ ਵੇਖੋ",
    share = "ਸਾਂਝਾ ਕਰੋ",
    taskAlarmPermissionDialogText = "ਅਲਾਰਮ ਕੰਮ ਕਰਨ ਲਈ, ਗਣਿਤ ਅਲਾਰਮ ਨੂੰ ਅਲਾਰਮ ਸੈੱਟ ਕਰਨ ਦੀ ਆਗਿਆ ਦੀ ਲੋੜ ਹੈ। ਜੇਕਰ ਇਹ ਆਗਿਆ ਨਹੀਂ ਦਿੱਤੀ ਗਈ ਤਾਂ ਮੌਜੂਦਾ ਅਲਾਰਮ ਵੀ ਕੰਮ ਨਹੀਂ ਕਰਨਗੇ।",
    taskAlarmPermissionDialogCancel = "ਹੁਣ ਨਹੀਂ",
    taskAlarmPermissionDialogConfirm = "ਆਗਿਆ ਦਿਓ",
    notification = "ਨੋਟੀਫਿਕੇਸ਼ਨ",
    alert = "ਰਿੰਗਟੋਨ",
    grantPermission = "ਆਗਿਆ ਦਿਓ",
    permissionsExternalStorageText = { tone ->
        "ਇਹ ਲੱਗਦਾ ਹੈ ਕਿ ਅਸੀਂ $tone ਨਹੀਂ ਚਲਾ ਸਕਦੇ, ਸ਼ਾਇਦ ਕਿਉਂਕਿ ਇੱਕ ਆਗਿਆ ਦੀ ਲੋੜ ਹੈ। ਜੇ ਤੁਸੀਂ ਚਾਹੁੰਦੇ ਹੋ, ਤਾਂ ਤੁਸੀਂ ਆਗਿਆ ਦੇ ਸਕਦੇ ਹੋ। ਬਦਲੇ ਵਿੱਚ, ਇੱਕ ਵੱਖਰਾ ਧੁਨ ਚੁਣੋ। ਇਹ ਫੈਸਲਾ ਸਿਸਟਮ ਸੈਟਿੰਗਜ਼ ਵਿੱਚ ਬਦਲਿਆ ਜਾ ਸਕਦਾ ਹੈ।"
    }
)
