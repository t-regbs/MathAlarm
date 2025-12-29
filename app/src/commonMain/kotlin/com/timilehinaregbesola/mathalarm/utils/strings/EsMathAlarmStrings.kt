package com.timilehinaregbesola.mathalarm.utils.strings

import cafe.adriel.lyricist.LyricistStrings

@LyricistStrings(languageTag = Locales.ES)
val EsMathAlarmStrings = Strings(
    multipleDays = "Días múltiples",
    greeting = { hour ->
        when {
            hour < 12 -> "Buenos días"
            hour in 12..17 -> "Buenas tardes"
            else -> "Buenas noches"
        }
    },
    expand = "Expandir",
    delete = "Eliminar",
    edit = "Editar",
    collapse = "Colapsar",
    noUpcomingAlarms = "No hay alarmas próximas",
    nextAlarmText = "Próxima alarma en",
    alarmSet = "Alarma establecida para",
    alarmPermissionDialogConfirm = "Confirmar",
    alarmPermissionDialogCancel = "Cancelar",
    alarmPermissionDialogText = "Esta aplicación requiere permiso para establecer alarmas.",
    alarmPermissionDialogTitle = "Permiso requerido",
    clearAlarmDialogConfirm = "Confirmar",
    clearAlarmDialogCancel = "Cancelar",
    clearAlarmDialogText = "¿Estás seguro de que quieres borrar esta alarmas?",
    clearAlarmDialogTitle = "Borrar alarma",
    emptyAlarmList = "Lista de alarmas vacía",
    nothingToSee = "Nada que ver aquí",
    alarms = "Alarmas",
    deleteAll = "Eliminar todo",
    settings = "Ajustes",
    toneUnavailable = "Tono no disponible",
    clear = "Borrar",
    snooze = "Siesta",
    enter = "Entrar",
    noRingtonePicker = "Selector de tonos no disponible",
    alarmTitle = "Título de la alarma",
    goodDay = "Buen día",
    defaultAlarmTone = "Tono de alarma (predeterminado)",
    disabledNotificationMessage = "Las notificaciones están desactivadas para esta aplicación.",
    disabledNotificationMessageExtended = "Las notificaciones de esta aplicación están desactivadas. Por favor, vaya a la configuración de su dispositivo y actívelas.",
    ok = "OK",
    notificationPermissionDialogMessage = "Esta aplicación requiere permiso para mostrar notificaciones.",
    repeatWeekly = "Repetir semanalmente",
    vibrate = "Vibrar",
    testAlarm = "Probar alarma",
    save = "Guardar",
    easyMath = "Matemáticas fáciles",
    mediumMath = "Matemáticas medias",
    hardMath = "Matemáticas difíciles",
    selectHour = "Seleccionar hora",
    cancel = "Cancelar",
    input = "Entrada",
    picker = "Selector",
    system = "Sistema",
    dark = "Oscuro",
    light = "Claro",
    appSettings = "Ajustes de la aplicación",
    back = "Atrás",
    colorTheme = "Tema de color",
    alert = "Alerta",
    help = "Ayuda",
    sendFeedback = "Enviar comentarios",
    defaultSendText = "Alarma predeterminada",
    supportEmail = "aregbestimi@gmail.com",
    emailChooserTitle = "Enviar correo electrónico",
    sendFeedbackMessage = "Envía comentarios al desarrollador",
    shareMathAlarm = "Compartir Math Alarm",
    shareWithOthers = "Echa un vistazo a esta genial aplicación de alarma",
    grantPermission = "Permitir",
    notification = "Notificación",
    permissionsExternalStorageText = { tone ->
        "Parece que no podemos reproducir $tone, probablemente porque se requiere permiso. Si lo deseas, puedes otorgar el permiso. Alternativamente, seleccione un sonido diferente. Esta decisión se puede cambiar en Configuración del sistema."
    },
    share = "Compartir",
    taskAlarmPermissionDialogText = "Para que las alarmas funcionen, Math Alarm necesita el permiso para establecer alarmas. Si no se concede este permiso, las alarmas existentes tampoco funcionarán.",
    taskAlarmPermissionDialogCancel = "Ahora no",
    taskAlarmPermissionDialogConfirm = "Conceder"
)