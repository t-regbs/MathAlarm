package com.timilehinaregbesola.mathalarm.navigation

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.scene.OverlayScene
import androidx.navigation3.scene.Scene
import androidx.navigation3.scene.SceneStrategy
import androidx.navigation3.scene.SceneStrategyScope
import com.timilehinaregbesola.mathalarm.navigation.BottomSheetSceneStrategy.Companion.bottomSheet

/** An [OverlayScene] that renders an [entry] within a [ModalBottomSheet]. */
@OptIn(ExperimentalMaterial3Api::class)
internal class BottomSheetScene<T : Any>(
    override val key: Any,
    override val previousEntries: List<NavEntry<T>>,
    override val overlaidEntries: List<NavEntry<T>>,
    private val bottomSheetEntry: NavEntry<T>,
    private val onBack: () -> Unit,
) : OverlayScene<T> {

    override val entries: List<NavEntry<T>> = listOf(bottomSheetEntry)

    override val content: @Composable (() -> Unit) = {
        ModalBottomSheet(
            sheetState = rememberModalBottomSheetState(
                skipPartiallyExpanded = true
            ),
            containerColor = MaterialTheme.colorScheme.background,
            shape = RoundedCornerShape(topStart = 40.dp, topEnd = 40.dp),
            onDismissRequest = { onBack() }
        ) {
            bottomSheetEntry.Content()
        }
    }

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other == null || this::class != other::class) return false

        other as BottomSheetScene<*>

        return key == other.key &&
                previousEntries == other.previousEntries &&
                overlaidEntries == other.overlaidEntries &&
                bottomSheetEntry == other.bottomSheetEntry
    }

    override fun hashCode(): Int {
        return key.hashCode() * 31 +
                previousEntries.hashCode() * 31 +
                overlaidEntries.hashCode() * 31 +
                bottomSheetEntry.hashCode() * 31
    }

    override fun toString(): String {
        return "DialogScene(key=$key, entry=$bottomSheetEntry, previousEntries=$previousEntries, overlaidEntries=$overlaidEntries)"
    }
}

/**
 * A [SceneStrategy] that displays entries that have added [bottomSheet] to their [NavEntry.metadata]
 * within a [ModalBottomSheet] instance.
 *
 * This strategy should always be added before any non-overlay scene strategies.
 */
class BottomSheetSceneStrategy<T : Any>() : SceneStrategy<T> {

    override fun SceneStrategyScope<T>.calculateScene(
        entries: List<NavEntry<T>>
    ): Scene<T>? {
        val lastEntry = entries.lastOrNull()
        val isBottomSheet = lastEntry?.metadata?.get(BOTTOM_SHEET_KEY) as? Boolean

        return if (isBottomSheet == true && entries.size >= 2) {
            val contentEntry = entries[entries.size - 2] // The entry below the bottom sheet
            BottomSheetScene(
                key = Pair(contentEntry.contentKey, lastEntry.contentKey),
                previousEntries = entries.dropLast(2),
                overlaidEntries = entries.dropLast(1),
                bottomSheetEntry = lastEntry,
                onBack = onBack,
            )
        } else null
    }

    companion object {
        /**
         * Function to be called on the [NavEntry.metadata] to mark this entry as something that
         * should be displayed within a [ModalBottomSheet].
         */
        fun bottomSheet(): Map<String, Any> = mapOf(BOTTOM_SHEET_KEY to true)

        internal const val BOTTOM_SHEET_KEY = "bottomSheet"
    }
}