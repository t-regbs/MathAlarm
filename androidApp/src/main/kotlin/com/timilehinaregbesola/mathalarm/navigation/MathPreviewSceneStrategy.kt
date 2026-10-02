package com.timilehinaregbesola.mathalarm.navigation

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.window.Dialog
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.scene.OverlayScene
import androidx.navigation3.scene.Scene
import androidx.navigation3.scene.SceneStrategy
import androidx.navigation3.scene.SceneStrategyScope
import com.timilehinaregbesola.mathalarm.platform.ConfigureMathPreviewWindow
import com.timilehinaregbesola.mathalarm.platform.mathPreviewDialogProperties

internal class MathPreviewSceneStrategy<T : Any>(
    private val useDialogOverlay: Boolean = true,
) : SceneStrategy<T> {
    override fun SceneStrategyScope<T>.calculateScene(entries: List<NavEntry<T>>): Scene<T>? {
        if (entries.size < 2) return null
        val entry = entries.last()
        if (entry.metadata[PREVIEW_KEY] != true) return null
        val previousEntries = entries.dropLast(1)
        return if (useDialogOverlay) MathPreviewScene(entry, previousEntries)
        else FullscreenMathPreviewScene(entry, previousEntries)
    }

    companion object {
        fun metadata(): Map<String, Any> = mapOf(PREVIEW_KEY to true)
        private const val PREVIEW_KEY = "mathPreview"
    }
}

/**
 * UIKit sheets sit above Compose dialogs. Remove their presentation during an iOS preview,
 * while retaining the editor entry and its saved state for the return navigation.
 */
private data class FullscreenMathPreviewScene<T : Any>(
    val previewEntry: NavEntry<T>,
    override val previousEntries: List<NavEntry<T>>,
) : Scene<T> {
    override val key: Any = previewEntry.contentKey
    override val entries: List<NavEntry<T>> = listOf(previewEntry)
    override val content: @Composable () -> Unit = {
        Surface(Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
            previewEntry.Content()
        }
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
