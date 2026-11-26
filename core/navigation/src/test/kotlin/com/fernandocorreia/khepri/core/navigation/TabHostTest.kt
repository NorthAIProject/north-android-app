package com.fernandocorreia.khepri.core.navigation

import androidx.compose.foundation.clickable
import androidx.compose.material3.Text
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.lifecycle.SavedStateHandle
import androidx.navigation3.runtime.EntryProviderScope
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fernandocorreia.khepri.core.navigation.ui.EntryInstaller
import com.fernandocorreia.khepri.core.navigation.ui.TabHost
import com.fernandocorreia.khepri.core.navigation.ui.screen
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/** Per-tab state kept across tab switches, and dropped for an entry replaced while its tab was not shown. */
@RunWith(AndroidJUnit4::class)
class TabHostTest {
    @get:Rule val compose = createComposeRule()

    private val installer =
        object : EntryInstaller {
            override fun EntryProviderScope<Route>.install() {
                screen<Route.TabRoot> { Text("root ${it.tab}") }
                screen<Route.Section> { Text("section ${it.id}") }
                screen<Route.Decision> { Text("decision ${it.id}") }
                screen<Route.Goal> { goal ->
                    var taps by rememberSaveable { mutableIntStateOf(0) }
                    Text("goal ${goal.id} taps $taps", Modifier.clickable { taps++ })
                }
            }
        }

    private fun state(): NavigationState {
        val queue = QueuedNavigator(FakePendingLinks(), CoroutineScope(Dispatchers.Unconfined))
        return NavigationState(SavedStateHandle(), queue, FakeSignedIn(), NavigationRegistry()) { "o" }
    }

    @Test
    fun `a tab keeps its screens' state while another tab shows`() {
        val state = state()
        compose.setContent { TabHost(state, setOf(installer)) }
        compose.runOnIdle { state.apply(NavCommand.Open(AppDestination.Goal("g1"))) }
        compose.onNodeWithText("goal g1 taps 0").performClick()
        compose.runOnIdle { state.select(AppTab.Today) }
        compose.onNodeWithText("root Today").assertExists()
        compose.runOnIdle { state.select(AppTab.More) }
        compose.onNodeWithText("goal g1 taps 1").assertExists()
    }

    @Test
    fun `an entry replaced while its tab was not shown comes back fresh`() {
        val state = state()
        compose.setContent { TabHost(state, setOf(installer)) }
        compose.runOnIdle { state.apply(NavCommand.Open(AppDestination.Goal("g1"))) }
        compose.onNodeWithText("goal g1 taps 0").performClick()
        compose.onNodeWithText("goal g1 taps 1").assertExists()
        compose.runOnIdle { state.select(AppTab.Today) }
        compose.runOnIdle { state.apply(NavCommand.Open(AppDestination.Decision("d1"))) }
        compose.onNodeWithText("decision d1").assertExists()
        compose.runOnIdle { state.apply(NavCommand.Open(AppDestination.Goal("g1"))) }
        compose.onNodeWithText("goal g1 taps 0").assertExists()
    }

    @Test
    fun `a reset drops every screen's state`() {
        val state = state()
        compose.setContent { TabHost(state, setOf(installer)) }
        compose.runOnIdle { state.apply(NavCommand.Open(AppDestination.Goal("g1"))) }
        compose.onNodeWithText("goal g1 taps 0").performClick()
        compose.runOnIdle { state.reset() }
        compose.onNodeWithText("root Today").assertExists()
        compose.runOnIdle { state.apply(NavCommand.Open(AppDestination.Goal("g1"))) }
        compose.onNodeWithText("goal g1 taps 0").assertExists()
    }
}
