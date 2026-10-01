package com.timilehinaregbesola.mathalarm.presentation.alarmsettings.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.selected
import androidx.compose.ui.semantics.semantics
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import cafe.adriel.lyricist.strings
import com.timilehinaregbesola.mathalarm.platform.*
import com.timilehinaregbesola.mathalarm.presentation.ui.icon.Check
import com.timilehinaregbesola.mathalarm.presentation.ui.icon.PlayArrow
import com.timilehinaregbesola.mathalarm.presentation.ui.icon.Stop
import com.timilehinaregbesola.mathalarm.sound.AlarmSound
import com.timilehinaregbesola.mathalarm.sound.AlarmSoundCatalog
import com.timilehinaregbesola.mathalarm.utils.strings.Strings

/** Sound library page inside the alarm editor. Done applies to the unsaved alarm draft. */
@Composable
internal fun AlarmSoundEditor(
    currentTone: String,
    currentToneTitle: String,
    onDismiss: () -> Unit,
    onApply: (String) -> Unit,
) {
    val currentSound = remember(currentTone, currentToneTitle) {
        AlarmSoundCatalog.find(currentTone) ?: AlarmSound(currentTone, currentToneTitle)
    }
    var pendingTone by rememberSaveable(currentTone) {
        mutableStateOf(currentSound.id.ifEmpty { AlarmSoundCatalog.DEFAULT_SOUND })
    }
    var previewingTone by remember { mutableStateOf<String?>(null) }
    val stopPreview = {
        stopAlarmTonePreview()
        previewingTone = null
    }
    val audition: (AlarmSound) -> Unit = { sound ->
        stopPreview()
        previewingTone = sound.id
        previewAlarmTone(sound.id) { previewingTone = null }
    }
    val dismiss = { stopPreview(); onDismiss() }
    DisposableEffect(Unit) {
        onDispose { stopAlarmTonePreview() }
    }

    Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.surfaceContainerLow) {
        val lifecycleOwner = LocalLifecycleOwner.current
        DisposableEffect(lifecycleOwner) {
            val observer = LifecycleEventObserver { _, event ->
                if (event == Lifecycle.Event.ON_STOP) stopPreview()
            }
            lifecycleOwner.lifecycle.addObserver(observer)
            onDispose {
                lifecycleOwner.lifecycle.removeObserver(observer)
                stopAlarmTonePreview()
            }
        }
        Column(
            Modifier.fillMaxSize().padding(vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
        ) {
            // Let the title wrap for translated labels and larger text.
            Row(Modifier.widthIn(max = 560.dp).fillMaxWidth().padding(horizontal = 8.dp),
                verticalAlignment = Alignment.CenterVertically) {
                TextButton(onClick = dismiss) { Text(strings.back) }
                Text(strings.alarmSound, Modifier.weight(1f).padding(horizontal = 8.dp),
                    style = MaterialTheme.typography.headlineSmall, fontWeight = FontWeight.SemiBold,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center)
                TextButton(onClick = { stopPreview(); onApply(pendingTone) }) {
                    Text(strings.done, fontWeight = FontWeight.SemiBold)
                }
            }
            LazyColumn(
                Modifier.widthIn(max = 560.dp).fillMaxWidth().weight(1f),
                contentPadding = PaddingValues(horizontal = 20.dp, vertical = 20.dp),
                verticalArrangement = Arrangement.spacedBy(20.dp),
            ) {
                if (currentSound.id.isNotEmpty() && AlarmSoundCatalog.sounds.none { it.id == currentSound.id }) {
                    item(key = "current") {
                        Text(strings.currentSound, Modifier.padding(start = 16.dp, bottom = 8.dp),
                            style = MaterialTheme.typography.labelLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant)
                        SoundGroup(listOf(currentSound), pendingTone, previewingTone,
                            onSelect = { pendingTone = it.id; audition(it) },
                            onPreview = { if (previewingTone == it.id) stopPreview() else audition(it) })
                    }
                }
                item(key = "library") {
                    SoundGroup(AlarmSoundCatalog.sounds, pendingTone, previewingTone,
                        onSelect = { pendingTone = it.id; audition(it) },
                        onPreview = { if (previewingTone == it.id) stopPreview() else audition(it) })
                }
            }

        }
    }
}

@Composable
private fun SoundGroup(
    sounds: List<AlarmSound>, selectedTone: String, previewingTone: String?,
    onSelect: (AlarmSound) -> Unit, onPreview: (AlarmSound) -> Unit,
) {
    Surface(shape = RoundedCornerShape(20.dp), color = MaterialTheme.colorScheme.surface) {
        Column {
            sounds.forEachIndexed { index, sound ->
                AlarmTonePickerRow(sound, sound.id == selectedTone, sound.id == previewingTone,
                    { onSelect(sound) }, { onPreview(sound) })
                if (index < sounds.lastIndex) HorizontalDivider(Modifier.padding(start = 76.dp),
                    color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.4f))
            }
        }
    }
}

@Composable
private fun AlarmTonePickerRow(
    sound: AlarmSound, selected: Boolean, isPreviewing: Boolean,
    onRowClick: () -> Unit, onPreviewClick: () -> Unit,
) {
    Row(
        Modifier.fillMaxWidth().heightIn(min = 74.dp)
            .testTag("tone-${sound.id}")
            .semantics { this.selected = selected }
            .clickable(role = Role.RadioButton, onClick = onRowClick)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        val previewLabel = "${if (isPreviewing) strings.stopSoundPreview else strings.previewSound} ${sound.displayName}"
        Box(Modifier.size(44.dp).clip(RoundedCornerShape(14.dp))
            .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.1f))
            .testTag("preview-${sound.id}")
            .semantics { contentDescription = previewLabel }
            .clickable(role = Role.Button, onClick = onPreviewClick), contentAlignment = Alignment.Center) {
            Icon(if (isPreviewing) Stop else PlayArrow, null, Modifier.size(20.dp),
                tint = MaterialTheme.colorScheme.primary)
        }
        Column(Modifier.weight(1f).padding(horizontal = 16.dp)) {
            Text(sound.displayName.ifBlank { strings.defaultAlarmTone },
                style = MaterialTheme.typography.bodyLarge, fontWeight = FontWeight.Medium)
            strings.soundDescription(sound.id)?.let {
                Text(it, Modifier.padding(top = 3.dp), style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
        if (selected) Icon(Check, null, Modifier.size(20.dp), tint = MaterialTheme.colorScheme.primary)
        else Spacer(Modifier.size(20.dp))
    }
}

private fun Strings.soundDescription(id: String): String? = when (id) {
    "alarm_daybreak" -> daybreakDescription
    "alarm_orbit" -> orbitDescription
    "alarm_rally" -> rallyDescription
    "alarm_glass_garden" -> glassGardenDescription
    "alarm_stepping_stones" -> steppingStonesDescription
    "alarm_clear_signal" -> clearSignalDescription
    else -> null
}
