package com.fernandocorreia.khepri.core.navigation.ui

import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.scene.OverlayScene
import androidx.navigation3.scene.Scene
import androidx.navigation3.scene.SceneStrategy
import androidx.navigation3.scene.SceneStrategyScope

/** Shows the top entry in a modal bottom sheet over the entries below it, when the entry asks for it. */
public class BottomSheetSceneStrategy<T : Any> : SceneStrategy<T> {
    override fun SceneStrategyScope<T>.calculateScene(entries: List<NavEntry<T>>): Scene<T>? {
        val last = entries.lastOrNull()
        if (last?.metadata?.get(KEY) != true) return null
        val below = entries.dropLast(1)
        return BottomSheetScene(last.contentKey, below, below, last, onBack)
    }

    public companion object {
        private const val KEY = "bottom-sheet"

        /** Metadata for an entry shown as a sheet. */
        public fun bottomSheet(): Map<String, Any> = mapOf(KEY to true)
    }
}

@OptIn(ExperimentalMaterial3Api::class)
private class BottomSheetScene<T : Any>(
    override val key: Any,
    override val previousEntries: List<NavEntry<T>>,
    override val overlaidEntries: List<NavEntry<T>>,
    private val entry: NavEntry<T>,
    private val onBack: () -> Unit,
) : OverlayScene<T> {
    override val entries: List<NavEntry<T>> = listOf(entry)
    override val content: @Composable () -> Unit = { ModalBottomSheet(onDismissRequest = onBack) { entry.Content() } }

    override fun equals(other: Any?): Boolean =
        other is BottomSheetScene<*> &&
            key == other.key &&
            entry == other.entry &&
            previousEntries == other.previousEntries

    override fun hashCode(): Int = (key.hashCode() * HASH + entry.hashCode()) * HASH + previousEntries.hashCode()

    private companion object {
        const val HASH = 31
    }
}
