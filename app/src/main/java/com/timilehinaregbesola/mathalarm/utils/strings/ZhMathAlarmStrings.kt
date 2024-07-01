package com.timilehinaregbesola.mathalarm.utils.strings

import cafe.adriel.lyricist.LyricistStrings

@LyricistStrings(languageTag = Locales.ZH)
val ZhMathAlarmStrings = Strings(
    multipleDays = "多天",
    greeting = { hour ->
        when {
            hour < 12 -> "早上好"
            hour in 12..17 -> "下午好"
            else -> "晚上好"
        }
    },
    expand = "展开",
    delete = "删除",
    edit = "编辑",
    collapse = "折叠",
    noUpcomingAlarms = "没有即将到来的闹钟",
    nextAlarmText = "下一个闹钟",
    alarmSet = "闹钟已设置",
    alarmPermissionDialogConfirm = "确认",
    alarmPermissionDialogCancel = "取消",
    alarmPermissionDialogText = "此应用需要设置闹钟的权限。",
    alarmPermissionDialogTitle = "需要权限",
    clearAlarmDialogConfirm = "确认",
    clearAlarmDialogCancel = "取消",
    clearAlarmDialogText = "您確定要清除警報嗎？",
    clearAlarmDialogTitle = "清除闹钟",
    emptyAlarmList = "空闹钟列表",
    nothingToSee = "这里什么都没有",
    alarms = "闹钟",
    deleteAll = "删除所有",
    settings = "设置",
    toneUnavailable = "铃声不可用",
    clear = "清除",
    snooze = "贪睡",
    enter = "输入",
    noRingtonePicker = "没有铃声选择器",
    alarmTitle = "闹钟标题",
    goodDay = "祝你有个好日子",
    defaultAlarmTone = "默认闹钟铃声",
    disabledNotificationMessage = "此应用的通知已被禁用。",
    disabledNotificationMessageExtended = "此应用的通知已被禁用。请到您的设备设置中启用它们。",
    ok = "确定",
    notificationPermissionDialogMessage = "此应用需要显示通知的权限。",
    repeatWeekly = "每周重复",
    vibrate = "振动",
    testAlarm = "测试闹钟",
    save = "保存",
    easyMath = "简单数学",
    mediumMath = "中等数学",
    hardMath = "困难数学",
    selectHour = "选择小时",
    cancel = "取消",
    input = "输入",
    picker = "选择器",
    system = "系统",
    dark = "深色",
    light = "浅色",
    appSettings = "应用设置",
    back = "返回",
    colorTheme = "颜色主题",
    help = "帮助",
    sendFeedback = "发送反馈",
    sendFeedbackMessage = "向开发者发送反馈",
    supportEmail = "aregbestimi@gmail.com",
    emailChooserTitle = "发送电子邮件",
    defaultSendText = "默认闹钟",
    shareMathAlarm = "分享数学闹钟",
    shareWithOthers = "看看这个很酷的闹钟应用",
    share = "分享",
    taskAlarmPermissionDialogText = "为了使闹钟正常工作，数学闹钟需要设置闹钟的权限。如果未授予此权限，现有的闹钟也将无法工作。",
    taskAlarmPermissionDialogCancel = "暂不",
    taskAlarmPermissionDialogConfirm = "授予",
    notification = "通知",
    alert = "铃声",
    grantPermission = "授予权限",
    permissionsExternalStorageText = { tone ->
        "我们似乎无法播放$tone，可能是因为需要权限。如果您愿意，您可以授予权限。或者，选择不同的声音。这个决定可以在系统设置中更改。"
    }
)
