package com.timilehinaregbesola.mathalarm.presentation.whatsnew

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import com.timilehinaregbesola.mathalarm.presentation.ui.spacing
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import cafe.adriel.lyricist.strings
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.timilehinaregbesola.mathalarm.presentation.ui.icon.EmojiSymbols

/** Illustrative content for this release; the announcement container accepts any optional visual. */
@Composable
internal fun MathChallengeAnnouncementPreview() {
    val names = strings.mathDifficultyNames
    val examples = listOf("14 + 28" to names[0], "36 ÷ 4" to names[0], "128 + 246" to names[1])
    Surface(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(PreviewDimensions.CORNER_RADIUS),
        color = MaterialTheme.colorScheme.surfaceContainerHighest,
    ) {
        Column(Modifier.padding(MaterialTheme.spacing.medium), verticalArrangement = Arrangement.spacedBy(PreviewDimensions.ROW_SPACING)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(PreviewDimensions.ROW_SPACING),
            ) {
                Surface(shape = CircleShape, color = MaterialTheme.colorScheme.primaryContainer) {
                    Icon(
                        EmojiSymbols,
                        contentDescription = null,
                        modifier = Modifier.padding(PreviewDimensions.ICON_PADDING).size(PreviewDimensions.ICON_SIZE),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer,
                    )
                }
                Column {
                    Text(strings.questionCount(examples.size), style = MaterialTheme.typography.titleMedium)
                    Text(strings.exampleDifficultyMix(2, 1), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            examples.forEachIndexed { index, (equation, difficulty) ->
                Surface(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(PreviewDimensions.QUESTION_CORNER_RADIUS),
                    color = MaterialTheme.colorScheme.surface,
                ) {
                    Row(
                        modifier = Modifier.padding(horizontal = PreviewDimensions.ROW_SPACING, vertical = PreviewDimensions.ROW_SPACING),
                        horizontalArrangement = Arrangement.spacedBy(PreviewDimensions.ROW_SPACING),
                        verticalAlignment = Alignment.CenterVertically,
                    ) {
                        Text("${index + 1}", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                        Text(equation, modifier = Modifier.weight(1f), style = MaterialTheme.typography.titleMedium)
                        Text(difficulty, style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary)
                    }
                }
            }
            Text(strings.exampleChallenge, style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

private object PreviewDimensions {
    val CORNER_RADIUS = 20.dp
    val QUESTION_CORNER_RADIUS = 12.dp
    val ROW_SPACING = 12.dp
    val ICON_PADDING = 10.dp
    val ICON_SIZE = 24.dp
}
