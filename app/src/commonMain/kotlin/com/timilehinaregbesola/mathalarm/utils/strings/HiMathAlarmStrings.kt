package com.timilehinaregbesola.mathalarm.utils.strings

import cafe.adriel.lyricist.LyricistStrings

@LyricistStrings(languageTag = Locales.HI)
val HiMathAlarmStrings = Strings(
    multipleDays = "कई दिन",
    greeting = { hour ->
        when {
            hour < 12 -> "सुप्रभात"
            hour in 12..17 -> "शुभ अपराह्न"
            else -> "शुभ संध्या"
        }
    },
    expand = "विस्तार",
    delete = "हटाएं",
    edit = "संपादित करें",
    collapse = "संक्षेप करें",
    noUpcomingAlarms = "कोई आगामी अलार्म नहीं",
    nextAlarmText = "अगला अलार्म",
    alarmSet = "अलार्म सेट किया गया",
    alarmPermissionDialogConfirm = "पुष्टि करें",
    alarmPermissionDialogCancel = "रद्द करें",
    alarmPermissionDialogText = "इस ऐप को अलार्म सेट करने की अनुमति की आवश्यकता है।",
    alarmPermissionDialogTitle = "अनुमति आवश्यक",
    clearAlarmDialogConfirm = "पुष्टि करें",
    clearAlarmDialogCancel = "रद्द करें",
    clearAlarmDialogText = "क्या आप वाकई अलार्म साफ़ करना चाहते हैं?",
    clearAlarmDialogTitle = "अलार्म साफ़ करें",
    emptyAlarmList = "खाली अलार्म सूची",
    nothingToSee = "यहाँ कुछ नहीं है",
    alarms = "अलार्म",
    deleteAll = "सभी हटाएं",
    settings = "सेटिंग्स",
    toneUnavailable = "स्वर अनुपलब्ध",
    clear = "साफ़ करें",
    snooze = "स्नूज़",
    enter = "प्रवेश करें",
    noRingtonePicker = "कोई रिंगटोन पिकर नहीं",
    alarmTitle = "अलार्म शीर्षक",
    goodDay = "शुभ दिन",
    defaultAlarmTone = "अलार्म स्वर (डिफ़ॉल्ट)",
    disabledNotificationMessage = "इस ऐप के लिए सूचनाएँ अक्षम हैं।",
    disabledNotificationMessageExtended = "इस एप्लिकेशन की सूचनाएँ अक्षम हैं। कृपया अपने डिवाइस सेटिंग्स में जाएं और उन्हें सक्षम करें।",
    ok = "ठीक है",
    notificationPermissionDialogMessage = "इस ऐप को सूचनाएं दिखाने की अनुमति की आवश्यकता है।",
    repeatWeekly = "साप्ताहिक दोहराएं",
    vibrate = "कंपन",
    testAlarm = "परीक्षण अलार्म",
    save = "सहेजें",
    easyMath = "आसान गणित",
    mediumMath = "मध्यम गणित",
    hardMath = "कठिन गणित",
    selectHour = "घंटा चुनें",
    cancel = "रद्द करें",
    input = "इनपुट",
    picker = "पिकर",
    system = "सिस्टम",
    dark = "डार्क",
    light = "लाइट",
    appSettings = "ऐप सेटिंग्स",
    back = "वापस",
    colorTheme = "रंग थीम",
    help = "मदद",
    sendFeedback = "प्रतिक्रिया भेजें",
    sendFeedbackMessage = "डेवलपर को प्रतिक्रिया भेजें",
    supportEmail = "aregbestimi@gmail.com",
    emailChooserTitle = "ईमेल भेजें",
    defaultSendText = "डिफ़ॉल्ट अलार्म",
    shareMathAlarm = "मैथ अलार्म साझा करें",
    shareWithOthers = "इस शानदार अलार्म ऐप को देखें",
    share = "साझा करें",
    taskAlarmPermissionDialogText = "अलार्म काम करने के लिए, मैथ अलार्म को अलार्म सेट करने की अनुमति की आवश्यकता है। यदि यह अनुमति नहीं दी गई है, तो मौजूदा अलार्म भी काम नहीं करेंगे।",
    taskAlarmPermissionDialogCancel = "अभी नहीं",
    taskAlarmPermissionDialogConfirm = "अनुमति दें",
    notification = "सूचना",
    alert = "रिंगटोन",
    grantPermission = "अनुमति दें",
    permissionsExternalStorageText = { tone ->
        "ऐसा लगता है कि हम $tone नहीं चला सकते, शायद क्योंकि अनुमति की आवश्यकता है। यदि आप चाहें तो अनुमति दे सकते हैं। वैकल्पिक रूप से, एक अलग ध्वनि चुनें। यह निर्णय सिस्टम सेटिंग्स में बदला जा सकता है।"
    }
)
