package com.fernandocorreia.khepri.core.navigation.ui

import androidx.activity.compose.BackHandler
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavEntry
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import com.fernandocorreia.khepri.core.navigation.AppTab
import com.fernandocorreia.khepri.core.navigation.NavCommand
import com.fernandocorreia.khepri.core.navigation.NavigationState
import com.fernandocorreia.khepri.core.navigation.Route
import com.fernandocorreia.khepri.core.navigation.ScreenAsk
import com.fernandocorreia.khepri.core.navigation.contentKey

/**
 * The selected tab's stack (design 8.2): one `NavDisplay` per tab under `key(tab)`, the tab's composed state kept in a
 * `SaveableStateHolder` while another tab shows, and the tab's ViewModels in its own store, so a tab switch keeps both.
 * Back pops, then returns to Today, then leaves the app. A reset drops everything composed. A route no installer
 * registers shows [unported], so a link to a screen not built yet lands somewhere instead of crashing.
 */
@Composable
public fun TabHost(
    state: NavigationState,
    installers: Set<EntryInstaller>,
    modifier: Modifier = Modifier,
    unported: @Composable (Route) -> Unit = {},
) {
    if (state.suspended) return
    val latestUnported by rememberUpdatedState(unported)
    val provider =
        remember(installers) {
            val roots =
                installers.fold(emptyMap<AppTab, @Composable () -> Unit>()) { all, installer -> all + installer.roots }
            entryProvider<Route>(
                fallback = { route -> NavEntry(route, contentKey = route.contentKey()) { latestUnported(route) } }
            ) {
                screen<Route.TabRoot> { root -> roots[root.tab]?.invoke() ?: UnportedRoot(root, latestUnported) }
                installers.forEach { with(it) { install() } }
            }
        }
    key(state.epoch) {
        val tabs = rememberSaveableStateHolder()
        val tab = state.selected
        val stack = state.stack(tab)
        BackHandler(enabled = stack.size == 1 && tab != AppTab.Today) { state.select(AppTab.Today) }
        key(tab) {
            tabs.SaveableStateProvider(tab.name) {
                val entryStates = rememberSaveableStateHolder()
                remember(tab) { state.takeStale(tab).onEach(entryStates::removeState) }
                DisposableEffect(tab) {
                    state.composing(tab)
                    onDispose { state.leaving(tab) }
                }
                CompositionLocalProvider(LocalScreenAsks provides state) {
                    NavDisplay(
                        backStack = stack,
                        modifier = modifier,
                        onBack = { state.apply(NavCommand.Pop) },
                        entryDecorators =
                            listOf(
                                rememberSaveableStateHolderNavEntryDecorator(entryStates),
                                rememberViewModelStoreNavEntryDecorator(state.provider(tab)),
                            ),
                        sceneStrategies = listOf(BottomSheetSceneStrategy()),
                        entryProvider = provider,
                    )
                }
            }
        }
    }
}

/** A tab no feature draws yet: shows [unported] and drops the tab's reload asks, which nothing else would take. */
@Composable
private fun UnportedRoot(root: Route.TabRoot, unported: @Composable (Route) -> Unit) {
    ConsumeAsks(pick = { (it as? ScreenAsk.ReloadRoot)?.takeIf { ask -> ask.tab == root.tab } }, onAsk = {})
    unported(root)
}
