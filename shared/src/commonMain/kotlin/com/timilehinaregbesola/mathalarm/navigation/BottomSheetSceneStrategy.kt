package com.timilehinaregbesola.mathalarm.navigation

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.setValue
import kotlinx.coroutines.launch
import com.timilehinaregbesola.mathalarm.platform.ChallengeBackHandler
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalWindowInfo
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.scene.OverlayScene
import androidx.navigation3.scene.Scene
import androidx.navigation3.scene.SceneStrategy
import androidx.navigation3.scene.SceneStrategyScope
import com.mohamedrejeb.calf.ui.sheet.AdaptiveBottomSheet
import com.mohamedrejeb.calf.ui.sheet.rememberAdaptiveSheetState
import com.timilehinaregbesola.mathalarm.navigation.BottomSheetSceneStrategy.Companion.bottomSheet
import com.timilehinaregbesola.mathalarm.platform.ConfigureMathPreviewWindow
import com.timilehinaregbesola.mathalarm.platform.mathPreviewDialogProperties
import com.timilehinaregbesola.mathalarm.platform.isIosPlatform

internal val LocalDismissSettingsSheet = staticCompositionLocalOf<(() -> Unit)?> { null }

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

    override val content: @Composable (() -> Unit) = {
        val windowSize = LocalWindowInfo.current.containerSize
        val tabletPortrait = with(LocalDensity.current) {
            windowSize.width.toDp() >= 600.dp && windowSize.height > windowSize.width
        }
        if (tabletPortrait && !isIosPlatform()) {
            Dialog(
                onDismissRequest = onBack,
                properties = DialogProperties(usePlatformDefaultWidth = false),
            ) {
                BoxWithConstraints(
                    Modifier.fillMaxSize().safeDrawingPadding().imePadding().padding(24.dp),
                    contentAlignment = Alignment.Center,
                ) {
                    Surface(
                        modifier = Modifier.width(minOf(600.dp, maxWidth)).height(minOf(900.dp, maxHeight)),
                        shape = RoundedCornerShape(28.dp),
                        color = MaterialTheme.colorScheme.surface,
                    ) {
                        CompositionLocalProvider(LocalDismissSettingsSheet provides onBack) {
                            bottomSheetEntry.Content()
                        }
                    }
                }
            }
        } else {
            val sheetState = rememberAdaptiveSheetState(skipPartiallyExpanded = true)
            val scope = rememberCoroutineScope()
            var dismissing by remember { mutableStateOf(false) }
            var removed by remember { mutableStateOf(false) }
            val finishDismiss: () -> Unit = {
                if (!removed) {
                    removed = true
                    onBack()
                }
            }
            val dismiss: () -> Unit = {
                if (!dismissing) {
                    dismissing = true
                    scope.launch {
                        try {
                            sheetState.hide()
                            if (!sheetState.isVisible) finishDismiss()
                        } finally {
                            dismissing = false
                        }
                    }
                }
            }
            AdaptiveBottomSheet(
                adaptiveSheetState = sheetState,
                containerColor = MaterialTheme.colorScheme.background,
                shape = RoundedCornerShape(topStart = 40.dp, topEnd = 40.dp),
                onDismissRequest = finishDismiss,
            ) {
                ChallengeBackHandler(enabled = true, onBack = dismiss)
                CompositionLocalProvider(LocalDismissSettingsSheet provides dismiss) {
                    bottomSheetEntry.Content()
                }
            }
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
        return "BottomSheetScene(key=$key, entry=$bottomSheetEntry, previousEntries=$previousEntries, overlaidEntries=$overlaidEntries)"
    }
}

/**
 * A [SceneStrategy] that displays entries that have added [bottomSheet] to their [NavEntry.metadata]
 * within an [AdaptiveBottomSheet] instance.
 *
 * This strategy should always be added before any non-overlay scene strategies.
 */
class BottomSheetSceneStrategy<T : Any> : SceneStrategy<T> {

    override fun SceneStrategyScope<T>.calculateScene(
        entries: List<NavEntry<T>>
    ): Scene<T>? {
        val lastEntry = entries.lastOrNull()
        if (lastEntry?.metadata?.get("mathPreview") == true && entries.size >= 2) {
            return MathPreviewScene(lastEntry, entries.dropLast(1))
        }
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
         * should be displayed within a [AdaptiveBottomSheet].
         */
        fun bottomSheet(): Map<String, Any> = mapOf(BOTTOM_SHEET_KEY to true)

        internal const val BOTTOM_SHEET_KEY = "bottomSheet"
    }
}

/** Keep the editor window in place while the full-screen test covers it. */
private data class MathPreviewScene<T : Any>(
    val previewEntry: NavEntry<T>,
    override val overlaidEntries: List<NavEntry<T>>,
) : OverlayScene<T> {
    override val key: Any = previewEntry.contentKey
    override val entries: List<NavEntry<T>> = listOf(previewEntry)
    override val previousEntries: List<NavEntry<T>> = overlaidEntries
    override val content: @Composable () -> Unit = {
        Dialog(
            onDismissRequest = {},
            properties = mathPreviewDialogProperties(),
        ) {
            Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
                ConfigureMathPreviewWindow()
                previewEntry.Content()
            }
        }
    }
}
