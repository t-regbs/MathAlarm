package com.timilehinaregbesola.mathalarm.navigation

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.scene.OverlayScene
import androidx.navigation3.scene.Scene
import androidx.navigation3.scene.SceneStrategy
import androidx.navigation3.scene.SceneStrategyScope
import com.mohamedrejeb.calf.ui.sheet.AdaptiveSheetState
import com.mohamedrejeb.calf.ui.sheet.AdaptiveBottomSheet
import com.mohamedrejeb.calf.ui.sheet.rememberAdaptiveSheetState
import com.timilehinaregbesola.mathalarm.platform.ChallengeBackHandler

/** An [OverlayScene] that renders an alarm settings entry within an [AdaptiveBottomSheet]. */
@OptIn(ExperimentalMaterial3Api::class)
internal class BottomSheetScene<T : Any>(
    override val key: Any,
    override val previousEntries: List<NavEntry<T>>,
    override val overlaidEntries: List<NavEntry<T>>,
    private val bottomSheetEntry: NavEntry<T>,
    private val onBack: () -> Unit,
) : OverlayScene<T> {

    override val entries: List<NavEntry<T>> = listOf(bottomSheetEntry)

    private var sheetState: AdaptiveSheetState? = null

    override suspend fun onRemove() {
        sheetState?.hide()
    }

    override val content: @Composable (() -> Unit) = {
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

    // Navigation callbacks change when the stack changes; they are not scene identity.
    override fun equals(other: Any?): Boolean =
        other is BottomSheetScene<*> &&
            key == other.key &&
            previousEntries == other.previousEntries &&
            overlaidEntries == other.overlaidEntries &&
            bottomSheetEntry == other.bottomSheetEntry

    override fun hashCode(): Int {
        var result = key.hashCode()
        result = 31 * result + previousEntries.hashCode()
        result = 31 * result + overlaidEntries.hashCode()
        result = 31 * result + bottomSheetEntry.hashCode()
        return result
    }

}

/**
 * A [SceneStrategy] that displays entries that have added [bottomSheet] to their [NavEntry.metadata]
 * within an [AdaptiveBottomSheet] instance.
 *
 * Wide windows give the list-detail strategy priority; compact windows use this sheet.
 */
class BottomSheetSceneStrategy<T : Any> : SceneStrategy<T> {

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
    val SHEET_CORNER_RADIUS = 40.dp
}
