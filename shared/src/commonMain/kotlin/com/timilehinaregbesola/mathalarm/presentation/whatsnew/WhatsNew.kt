package com.timilehinaregbesola.mathalarm.presentation.whatsnew

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.semantics.ProgressBarRangeInfo
import androidx.compose.ui.semantics.progressBarRangeInfo
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import cafe.adriel.lyricist.strings
import com.timilehinaregbesola.mathalarm.domain.model.MathChallenge
import com.timilehinaregbesola.mathalarm.presentation.ui.icon.ArrowBack
import com.timilehinaregbesola.mathalarm.presentation.ui.spacing

internal data class FeatureAnnouncement(
    val feature: AnnouncementFeature,
    val title: String,
    val description: String,
    val steps: List<String>,
    val actionLabel: String,
)

internal val announcementCatalog: List<FeatureAnnouncement>
    @Composable get() = listOf(
        FeatureAnnouncement(
            feature = AnnouncementFeature.MATH_CHALLENGES,
            title = strings.challengeAnnouncementTitle,
            description = strings.challengeAnnouncementDescription(MathChallenge.MAX_QUESTIONS),
            steps = listOf(strings.challengeAnnouncementInstructions),
            actionLabel = strings.tryFeature,
        ),
        FeatureAnnouncement(
            feature = AnnouncementFeature.SKIP_NEXT,
            title = strings.skipNext,
            description = strings.skipAnnouncementDescription,
            steps = listOf(strings.skipAnnouncementInstructions, strings.skipAnnouncementUndo),
            actionLabel = strings.viewAlarms,
        ),
    )

@Composable
internal fun WhatsNewDialog(
    announcements: List<FeatureAnnouncement>,
    onSeen: (String) -> Unit,
    onDismiss: () -> Unit,
    onTryFeature: (AnnouncementFeature) -> Unit,
) {
    if (announcements.isEmpty()) return
    var page by rememberSaveable(announcements.map { it.feature.id }) { mutableStateOf(0) }
    val announcement = announcements[page]
    val acknowledge = { onSeen(announcement.feature.id) }
    val dismiss = {
        acknowledge()
        onDismiss()
    }
    Dialog(
        onDismissRequest = dismiss,
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
                key(announcement.feature) {
                    Column(
                        modifier = Modifier
                            .weight(1f, fill = false)
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.medium),
                    ) {
                        Text(strings.whatsNew, style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.primary)
                        Text(announcement.title, style = MaterialTheme.typography.headlineSmall)
                        when (announcement.feature) {
                            AnnouncementFeature.MATH_CHALLENGES -> MathChallengeAnnouncementPreview()
                            AnnouncementFeature.SKIP_NEXT -> SkipAlarmAnnouncementPreview()
                        }
                        Text(announcement.description, style = MaterialTheme.typography.bodyLarge)
                        announcement.steps.forEachIndexed { index, step ->
                            val label = if (announcement.steps.size > 1) "${index + 1}. $step" else step
                            Text(label, style = MaterialTheme.typography.bodyMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        }
                    }
                }
                if (announcements.size > 1) {
                    AnnouncementPagination(
                        page = page,
                        count = announcements.size,
                        onPageChange = {
                            acknowledge()
                            page = it
                        },
                    )
                }
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(AnnouncementDimensions.ACTION_SPACING, Alignment.End),
                ) {
                    TextButton(onClick = dismiss) {
                        Text(strings.gotIt)
                    }
                    Button(
                        onClick = {
                            acknowledge()
                            onTryFeature(announcement.feature)
                        }
                    ) {
                        Text(announcement.actionLabel)
                    }
                }
            }
        }
    }
}

/** Keep browsing controls together and independent from feature actions. */
@Composable
private fun AnnouncementPagination(page: Int, count: Int, onPageChange: (Int) -> Unit) {
    val isRtl = LocalLayoutDirection.current == LayoutDirection.Rtl
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        IconButton(
            enabled = page > 0,
            onClick = { onPageChange(page - 1) }
        ) {
            Icon(
                imageVector = ArrowBack,
                contentDescription = strings.back,
                modifier = Modifier.rotate(if (isRtl) 180f else 0f)
            )
        }
        Row(
            modifier = Modifier.weight(1f).semantics {
                progressBarRangeInfo = ProgressBarRangeInfo(page.toFloat(), 0f..(count - 1).toFloat())
            },
            horizontalArrangement = Arrangement.spacedBy(MaterialTheme.spacing.small, Alignment.CenterHorizontally),
        ) {
            repeat(count) { index ->
                Box(
                    Modifier.width(AnnouncementDimensions.INDICATOR_WIDTH)
                        .height(MaterialTheme.spacing.extraSmall)
                        .background(
                            if (index == page) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.outlineVariant,
                            CircleShape,
                        ),
                )
            }
        }
        IconButton(
            enabled = page < count - 1,
            onClick = { onPageChange(page + 1) }
        ) {
            Icon(
                imageVector = ArrowBack,
                contentDescription = strings.nextFeature,
                modifier = Modifier.rotate(if (isRtl) 0f else 180f)
            )
        }
    }
}

private object AnnouncementDimensions {
    val MAX_WIDTH = 560.dp
    val INDICATOR_WIDTH = 24.dp
    val SECTION_SPACING = 20.dp
    val ACTION_SPACING = 12.dp
}
