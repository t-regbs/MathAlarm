package com.timilehinaregbesola.mathalarm.utils.strings

import cafe.adriel.lyricist.LyricistStrings

@LyricistStrings(languageTag = Locales.RU)
val RuMathAlarmStrings = Strings(
    multipleDays = "Несколько дней",
    greeting = { hour ->
        when {
            hour < 12 -> "Доброе утро"
            hour in 12..17 -> "Добрый день"
            else -> "Добрый вечер"
        }
    },
    expand = "Развернуть",
    delete = "Удалить",
    edit = "Редактировать",
    collapse = "Свернуть",
    noUpcomingAlarms = "Нет предстоящих будильников",
    nextAlarmText = "Следующий будильник через",
    alarmSet = "Будильник установлен на",
    alarmPermissionDialogConfirm = "Подтвердить",
    alarmPermissionDialogCancel = "Отмена",
    alarmPermissionDialogText = "Это приложение требует разрешения на установку будильников.",
    alarmPermissionDialogTitle = "Требуется разрешение",
    clearAlarmDialogConfirm = "Подтвердить",
    clearAlarmDialogCancel = "Отмена",
    clearAlarmDialogText = "Вы уверены, что хотите сбросить сигналы тревоги?",
    clearAlarmDialogTitle = "Удалить будильник",
    emptyAlarmList = "Пустой список будильников",
    nothingToSee = "Здесь ничего нет",
    alarms = "Будильники",
    deleteAll = "Удалить все",
    settings = "Настройки",
    toneUnavailable = "Тон недоступен",
    clear = "Очистить",
    snooze = "Отложить",
    enter = "Ввод",
    noRingtonePicker = "Выбор мелодии звонка недоступен",
    alarmTitle = "Название будильника",
    goodDay = "Хороший день",
    defaultAlarmTone = "Мелодия будильника (по умолчанию)",
    disabledNotificationMessage = "Уведомления отключены для этого приложения.",
    disabledNotificationMessageExtended = "Уведомления этого приложения отключены. Пожалуйста, перейдите в настройки вашего устройства и включите их.",
    ok = "хорошо",
    notificationPermissionDialogMessage = "Это приложение требует разрешения на отображение уведомлений.",
    repeatWeekly = "Повторять еженедельно",
    vibrate = "Вибрация",
    testAlarm = "Тестовый будильник",
    save = "Сохранить",
    easyMath = "Простая математика",
    mediumMath = "Средняя математика",
    hardMath = "Сложная математика",
    selectHour = "Выберите час",
    cancel = "Отмена",
    input = "Ввод",
    picker = "Выбор",
    system = "Система",
    dark = "Темный",
    light = "Светлый",
    appSettings = "Настройки приложения",
    back = "Назад",
    colorTheme = "Цветовая тема",
    help = "Помощь",
    sendFeedback = "Отправить отзыв",
    sendFeedbackMessage = "Отправить отзыв разработчику",
    supportEmail = "aregbestimi@gmail.com",
    emailChooserTitle = "Выбор почты",
    shareMathAlarm = "Поделиться MathAlarm",
    shareWithOthers = "Поделиться с другими",
    share = "Поделиться",
    alert = "Предупреждение",
    defaultSendText = "Стандартный будильник",
    taskAlarmPermissionDialogText = "Чтобы сигналы тревоги работали, Math Alarm необходимо разрешение на установку сигналов тревоги. Если это разрешение не предоставлено, существующие сигналы тревоги также не будут работать.",
    taskAlarmPermissionDialogConfirm = "Подтвердить",
    grantPermission = "Разрешить",
    notification = "Уведомление",
    taskAlarmPermissionDialogCancel = "Не сейчас",
    permissionsExternalStorageText = { tone ->
        "Кажется, мы не можем воспроизвести $tone, возможно, потому, что требуется разрешение. Если хотите, можете дать разрешение. Либо выберите другой звук. Это решение можно изменить в настройках системы."
    }
)