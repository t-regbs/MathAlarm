package com.timilehinaregbesola.mathalarm.navigation

import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.scene.OverlayScene
import androidx.navigation3.scene.Scene
import androidx.navigation3.scene.SceneStrategy
import androidx.navigation3.scene.SceneStrategyScope
import com.mohamedrejeb.calf.ui.sheet.AdaptiveSheetState
import com.mohamedrejeb.calf.ui.sheet.AdaptiveBottomSheet
import com.mohamedrejeb.calf.ui.sheet.rememberAdaptiveSheetState
import com.timilehinaregbesola.mathalarm.platform.ChallengeBackHandler
import com.timilehinaregbesola.mathalarm.presentation.ui.spacing
import com.timilehinaregbesola.mathalarm.platform.isIosPlatform

/** An [OverlayScene] that renders an alarm settings entry within an [AdaptiveBottomSheet]. */
@OptIn(ExperimentalMaterial3Api::class)
internal class BottomSheetScene<T : Any>(
    override val key: Any,
    override val previousEntries: List<NavEntry<T>>,
    override val overlaidEntries: List<NavEntry<T>>,
    private val bottomSheetEntry: NavEntry<T>,
    private val useCenteredDialog: Boolean,
    private val onBack: () -> Unit,
) : OverlayScene<T> {

    override val entries: List<NavEntry<T>> = listOf(bottomSheetEntry)

    private var sheetState: AdaptiveSheetState? = null

    override suspend fun onRemove() {
        sheetState?.hide()
    }

    override val content: @Composable (() -> Unit) = {
        if (useCenteredDialog && !isIosPlatform()) {
            Dialog(
                onDismissRequest = onBack,
                properties = DialogProperties(usePlatformDefaultWidth = false),
            ) {
                BoxWithConstraints(
                    Modifier.fillMaxSize()
                        .safeDrawingPadding()
                        .imePadding()
                        .padding(MaterialTheme.spacing.extraMedium),
                    contentAlignment = Alignment.Center,
                ) {
                    Surface(
                        modifier = Modifier
                            .width(minOf(SettingsSheetDimensions.DIALOG_MAX_WIDTH, maxWidth))
                            .height(minOf(SettingsSheetDimensions.DIALOG_MAX_HEIGHT, maxHeight)),
                        shape = RoundedCornerShape(SettingsSheetDimensions.DIALOG_CORNER_RADIUS),
                        color = MaterialTheme.colorScheme.surface,
                    ) {
                        bottomSheetEntry.Content()
                    }
                }
            }
        } else {
            val state = rememberAdaptiveSheetState(skipPartiallyExpanded = true)
            sheetState = state
            AdaptiveBottomSheet(
                adaptiveSheetState = state,
                containerColor = MaterialTheme.colorScheme.background,
                shape = RoundedCornerShape(
                    topStart = SettingsSheetDimensions.SHEET_CORNER_RADIUS,
                    topEnd = SettingsSheetDimensions.SHEET_CORNER_RADIUS,
                ),
                onDismissRequest = onBack,
            ) {
                ChallengeBackHandler(enabled = true, onBack = onBack)
                bottomSheetEntry.Content()
            }
        }
    }

    // Navigation callbacks change when the stack changes; they are not scene identity.
    override fun equals(other: Any?): Boolean =
        other is BottomSheetScene<*> &&
            key == other.key &&
            previousEntries == other.previousEntries &&
            overlaidEntries == other.overlaidEntries &&
            bottomSheetEntry == other.bottomSheetEntry &&
            useCenteredDialog == other.useCenteredDialog

    override fun hashCode(): Int {
        var result = key.hashCode()
        result = 31 * result + previousEntries.hashCode()
        result = 31 * result + overlaidEntries.hashCode()
        result = 31 * result + bottomSheetEntry.hashCode()
        return 31 * result + useCenteredDialog.hashCode()
    }

}

/**
 * A [SceneStrategy] that displays entries that have added [bottomSheet] to their [NavEntry.metadata]
 * within an [AdaptiveBottomSheet] instance.
 *
 * This strategy should always be added before any non-overlay scene strategies.
 */
class BottomSheetSceneStrategy<T : Any>(
    private val useCenteredDialog: Boolean,
) : SceneStrategy<T> {

    override fun SceneStrategyScope<T>.calculateScene(
        entries: List<NavEntry<T>>
    ): Scene<T>? {
        if (entries.size < 2) return null
        val lastEntry = entries.last()
        if (lastEntry.metadata[BOTTOM_SHEET_KEY] != true) return null
        val underlyingEntries = entries.dropLast(1)
        return BottomSheetScene(
            key = lastEntry.contentKey,
            previousEntries = underlyingEntries,
            overlaidEntries = underlyingEntries,
            bottomSheetEntry = lastEntry,
            useCenteredDialog = useCenteredDialog,
            onBack = onBack,
        )
    }

    companion object {
        /**
         * Function to be called on the [NavEntry.metadata] to mark this entry as something that
         * should be displayed within a [AdaptiveBottomSheet].
         */
        fun bottomSheet(): Map<String, Any> = mapOf(BOTTOM_SHEET_KEY to true)

        internal const val BOTTOM_SHEET_KEY = "bottomSheet"
    }
}

private object SettingsSheetDimensions {
    val DIALOG_MAX_WIDTH = 600.dp
    val DIALOG_MAX_HEIGHT = 900.dp
    val DIALOG_CORNER_RADIUS = 28.dp
    val SHEET_CORNER_RADIUS = 40.dp
}
