package com.timilehinaregbesola.mathalarm.utils.strings

import cafe.adriel.lyricist.LyricistStrings

@LyricistStrings(languageTag = Locales.PT)
val PtMathAlarmStrings = Strings(
    multipleDays = "Múltiplos dias",
    greeting = { hour ->
        when {
            hour < 12 -> "Bom dia"
            hour in 12..17 -> "Boa tarde"
            else -> "Boa noite"
        }
    },
    expand = "Expandir",
    delete = "Excluir",
    edit = "Editar",
    collapse = "Colapso",
    noUpcomingAlarms = "Nenhuma alarme próximo",
    nextAlarmText = "Próximo alarme em",
    alarmSet = "Alarme definido para",
    alarmPermissionDialogConfirm = "Confirmar",
    alarmPermissionDialogCancel = "Cancelar",
    alarmPermissionDialogText = "Este aplicativo requer permissão para definir alarmes.",
    alarmPermissionDialogTitle = "Permissão necessária",
    clearAlarmDialogConfirm = "Confirmar",
    clearAlarmDialogCancel = "Cancelar",
    clearAlarmDialogText = "Tem certeza de que deseja limpar os alarmes?",
    clearAlarmDialogTitle = "Limpar alarmes",
    emptyAlarmList = "Lista de alarmes vazia",
    nothingToSee = "Nada para ver aqui",
    alarms = "Alarmes",
    deleteAll = "Excluir tudo",
    settings = "Configurações",
    toneUnavailable = "Tom indisponível",
    clear = "Claro",
    snooze = "Soneca",
    enter = "Entrar",
    noRingtonePicker = "Seleção de toque indisponível",
    alarmTitle = "Título do alarme",
    goodDay = "Bom dia",
    defaultAlarmTone = "Tom de alarme padrão",
    disabledNotificationMessage = "Notificações estão desativadas para este aplicativo.",
    ok = "OK",
    notificationPermissionDialogMessage = "Este aplicativo requer permissão para exibir notificações.",
    repeatWeekly = "Repetir semanalmente",
    vibrate = "Vibrar",
    testAlarm = "Testar alarme",
    save = "Salvar",
    easyMath = "Matemática fácil",
    mediumMath = "Matemática média",
    hardMath = "Matemática difícil",
    selectHour = "Selecione a hora",
    cancel = "Cancelar",
    input = "Entrada",
    picker = "Seletor",
    system = "Sistema",
    dark = "Escuro",
    light = "Claro",
    appSettings = "Configurações do aplicativo",
    back = "Voltar",
    colorTheme = "Tema de cor",
    help = "Ajuda",
    sendFeedback = "Enviar feedback",
    sendFeedbackMessage = "Enviar feedback para o desenvolvedor",
    supportEmail = "aregbestimi@gmail.com",
    emailChooserTitle = "Escolha um aplicativo de e-mail",
    shareMathAlarm = "Compartilhar MathAlarm",
    shareWithOthers = "Compartilhar com outros",
    taskAlarmPermissionDialogText = "Para que os alarmes funcionem, o alarme matemático precisa de permissão para definir alarmes. Se esta permissão não for concedida, os alarmes existentes não funcionarão tão bem.",
    alert = "Alerta",
    share = "Compartilhar",
    defaultSendText = "Alarme padrão",
    disabledNotificationMessageExtended = "Notificações deste aplicativo estão desativadas. Por favor, vá para as configurações do seu dispositivo e ative-as.",
    grantPermission = "Permitir",
    notification = "Notificação",
    permissionsExternalStorageText = {  tone ->
        "Parece que não podemos tocar $tone, provavelmente porque é necessária permissão. Se desejar, você pode conceder a permissão. Como alternativa, selecione um som diferente. Esta decisão pode ser alterada nas configurações do sistema."
    },
    taskAlarmPermissionDialogCancel = "Agora não",
    taskAlarmPermissionDialogConfirm = "Conceder"

)