package com.timilehinaregbesola.mathalarm.presentation.whatsnew

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import com.timilehinaregbesola.mathalarm.presentation.ui.spacing
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import cafe.adriel.lyricist.strings
import com.timilehinaregbesola.mathalarm.domain.model.MathChallenge
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties

/** Change the ID only when there is a new feature announcement, not for every app update. */
internal data class FeatureAnnouncement(
    val id: String,
    val title: String,
    val description: String,
    val steps: List<String>,
    val actionLabel: String,
)

internal val currentAnnouncement: FeatureAnnouncement
    @Composable get() = FeatureAnnouncement(
        id = "math-challenges-v1",
        title = strings.challengeAnnouncementTitle,
        description = strings.challengeAnnouncementDescription(MathChallenge.MAX_QUESTIONS),
        steps = listOf(strings.challengeAnnouncementInstructions),
        actionLabel = strings.tryFeature,
    )

@Composable
internal fun WhatsNewDialog(
    announcement: FeatureAnnouncement,
    onDismiss: () -> Unit,
    onTryFeature: () -> Unit,
    visual: (@Composable () -> Unit)? = null,
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false),
    ) {
        Surface(
            modifier = Modifier.padding(MaterialTheme.spacing.extraMedium).widthIn(max = AnnouncementDimensions.MAX_WIDTH).fillMaxWidth(),
            shape = MaterialTheme.shapes.extraLarge,
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
        ) {
            Column(
                modifier = Modifier.padding(MaterialTheme.spacing.extraMedium),
                verticalArrangement = Arrangement.spacedBy(AnnouncementDimensions.SECTION_SPACING),
            ) {
                Column(
                    modifier = Modifier.weight(1f, fill = false).verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium),
                ) {
                    Text(strings.whatsNew, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                    Text(announcement.title, style = MaterialTheme.typography.headlineSmall)
                    visual?.invoke()
                    Text(announcement.description, style = MaterialTheme.typography.bodyLarge)
                    announcement.steps.forEachIndexed { index, step ->
                        val label = if (announcement.steps.size > 1) "${index + 1}. $step" else step
                        Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AnnouncementDimensions.ACTION_SPACING, Alignment.End),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    TextButton(onClick = onDismiss) { Text(strings.gotIt) }
                    Button(onClick = onTryFeature) { Text(announcement.actionLabel) }
                }
            }
        }
    }
}

private object AnnouncementDimensions {
    val MAX_WIDTH = 560.dp
    val SECTION_SPACING = 20.dp
    val ACTION_SPACING = 12.dp
}
