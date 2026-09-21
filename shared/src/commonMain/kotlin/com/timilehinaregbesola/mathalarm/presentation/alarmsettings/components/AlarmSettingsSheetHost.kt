package com.timilehinaregbesola.mathalarm.presentation.alarmsettings.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.Saver
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import com.timilehinaregbesola.mathalarm.domain.model.MathChallenge
import com.timilehinaregbesola.mathalarm.platform.ChallengeBackHandler

internal enum class AlarmSettingsPage {
    Main,
    Challenge,
    Snooze,
}

private val pageSaver = Saver<AlarmSettingsPage, String>(
    save = { it.name },
    restore = { AlarmSettingsPage.valueOf(it) },
)

/** One navigation owner for all pages inside the existing alarm settings sheet. */
@Composable
internal fun AlarmSettingsSheetHost(
    challenge: MathChallenge,
    snoozeEnabled: Boolean,
    snoozeMinutes: Int,
    maxSnoozes: Int,
    onChallengeApply: (MathChallenge) -> Unit,
    onSnoozeApply: (Boolean, Int, Int) -> Unit,
    content: @Composable (onEditChallenge: () -> Unit, onEditSnooze: () -> Unit) -> Unit,
) {
    var page by rememberSaveable(stateSaver = pageSaver) { mutableStateOf(AlarmSettingsPage.Main) }
    val savedPages = rememberSaveableStateHolder()
    val returnToMain = {
        // Editors own temporary drafts. Reopening starts with the latest applied settings.
        savedPages.removeState(page.name)
        page = AlarmSettingsPage.Main
    }
    ChallengeBackHandler(enabled = page != AlarmSettingsPage.Main, onBack = returnToMain)
    Box(Modifier.fillMaxSize()) {
        savedPages.SaveableStateProvider(page.name) {
            when (page) {
                AlarmSettingsPage.Main -> content(
                    { page = AlarmSettingsPage.Challenge },
                    { page = AlarmSettingsPage.Snooze },
                )
                AlarmSettingsPage.Challenge -> ChallengeEditor(
                    initial = challenge.normalized(),
                    onDismiss = returnToMain,
                    onApply = {
                        onChallengeApply(it)
                        returnToMain()
                    },
                )
                AlarmSettingsPage.Snooze -> SnoozeEditor(
                    initialEnabled = snoozeEnabled,
                    initialMinutes = snoozeMinutes,
                    initialMaximum = maxSnoozes,
                    onDismiss = returnToMain,
                    onApply = { enabled, minutes, maximum ->
                        onSnoozeApply(enabled, minutes, maximum)
                        returnToMain()
                    },
                )
            }
        }
    }
}
