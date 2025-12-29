package com.timilehinaregbesola.mathalarm.utils.strings

import cafe.adriel.lyricist.LyricistStrings

@LyricistStrings(languageTag = Locales.BN)
val BnMathAlarmStrings = Strings(
    multipleDays = "একাধিক দিন",
    greeting = { hour ->
        when {
            hour < 12 -> "সুপ্রভাত"
            hour in 12..17 -> "শুভ অপরাহ্ন"
            else -> "শুভ সন্ধ্যা"
        }
    },
    expand = "বিস্তৃত করুন",
    delete = "মুছে ফেলুন",
    edit = "সম্পাদনা করুন",
    collapse = "সংকুচিত করুন",
    noUpcomingAlarms = "কোনও আসন্ন অ্যালার্ম নেই",
    nextAlarmText = "পরবর্তী অ্যালার্ম",
    alarmSet = "অ্যালার্ম সেট হয়েছে",
    alarmPermissionDialogConfirm = "নিশ্চিত করুন",
    alarmPermissionDialogCancel = "বাতিল করুন",
    alarmPermissionDialogText = "এই অ্যাপকে অ্যালার্ম সেট করার অনুমতি প্রয়োজন।",
    alarmPermissionDialogTitle = "অনুমতি প্রয়োজন",
    clearAlarmDialogConfirm = "নিশ্চিত করুন",
    clearAlarmDialogCancel = "বাতিল করুন",
    clearAlarmDialogText = "আপনি কি সত্যিই এই অ্যালার্মটি মুছতে চান?",
    clearAlarmDialogTitle = "অ্যালার্ম মুছুন",
    emptyAlarmList = "খালি অ্যালার্ম তালিকা",
    nothingToSee = "এখানে দেখার কিছু নেই",
    alarms = "অ্যালার্ম",
    deleteAll = "সব মুছে ফেলুন",
    settings = "সেটিংস",
    toneUnavailable = "টোন উপলব্ধ নয়",
    clear = "পরিষ্কার করুন",
    snooze = "স্নুজ",
    enter = "প্রবেশ করুন",
    noRingtonePicker = "কোনও রিংটোন পিকার নেই",
    alarmTitle = "অ্যালার্ম শিরোনাম",
    goodDay = "শুভ দিন",
    defaultAlarmTone = "অ্যালার্ম টোন (ডিফল্ট)",
    disabledNotificationMessage = "এই অ্যাপের জন্য বিজ্ঞপ্তি অক্ষম করা হয়েছে।",
    disabledNotificationMessageExtended = "এই অ্যাপ্লিকেশনের বিজ্ঞপ্তি অক্ষম করা হয়েছে। অনুগ্রহ করে আপনার ডিভাইসের সেটিংসে যান এবং সেগুলি সক্ষম করুন।",
    ok = "ঠিক আছে",
    notificationPermissionDialogMessage = "এই অ্যাপটিকে বিজ্ঞপ্তি দেখানোর অনুমতি প্রয়োজন।",
    repeatWeekly = "সাপ্তাহিক পুনরাবৃত্তি",
    vibrate = "কম্পন",
    testAlarm = "পরীক্ষা অ্যালার্ম",
    save = "সংরক্ষণ করুন",
    easyMath = "সহজ গাণিতিক",
    mediumMath = "মাঝারি গাণিতিক",
    hardMath = "কঠিন গাণিতিক",
    selectHour = "ঘণ্টা নির্বাচন করুন",
    cancel = "বাতিল করুন",
    input = "ইনপুট",
    picker = "পিকার",
    system = "সিস্টেম",
    dark = "ডার্ক",
    light = "লাইট",
    appSettings = "অ্যাপ সেটিংস",
    back = "পিছনে",
    colorTheme = "রঙের থিম",
    help = "সাহায্য",
    sendFeedback = "প্রতিক্রিয়া পাঠান",
    sendFeedbackMessage = "ডেভেলপারকে প্রতিক্রিয়া পাঠান",
    supportEmail = "aregbestimi@gmail.com",
    emailChooserTitle = "ইমেল পাঠান",
    defaultSendText = "ডিফল্ট অ্যালার্ম",
    shareMathAlarm = "গাণিতিক অ্যালার্ম শেয়ার করুন",
    shareWithOthers = "এই চমৎকার অ্যালার্ম অ্যাপটি দেখুন",
    share = "শেয়ার করুন",
    taskAlarmPermissionDialogText = "অ্যালার্ম কাজ করার জন্য, গাণিতিক অ্যালার্মকে অ্যালার্ম সেট করার অনুমতি প্রয়োজন। যদি এই অনুমতি না দেওয়া হয়, তবে বিদ্যমান অ্যালার্মও কাজ করবে না।",
    taskAlarmPermissionDialogCancel = "এখন নয়",
    taskAlarmPermissionDialogConfirm = "অনুমতি দিন",
    notification = "বিজ্ঞপ্তি",
    alert = "রিংটোন",
    grantPermission = "অনুমতি দিন",
    permissionsExternalStorageText = { tone ->
        "মনে হচ্ছে আমরা $tone চালাতে পারছি না, সম্ভবত একটি অনুমতির প্রয়োজন। যদি আপনি চান, আপনি অনুমতি দিতে পারেন। বিকল্পভাবে, একটি ভিন্ন শব্দ নির্বাচন করুন। এই সিদ্ধান্তটি সিস্টেম সেটিংসে পরিবর্তন করা যেতে পারে।"
    }
)
