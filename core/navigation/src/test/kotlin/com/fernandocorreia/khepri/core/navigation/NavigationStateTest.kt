package com.fernandocorreia.khepri.core.navigation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewmodel.CreationExtras
import com.fernandocorreia.khepri.core.common.SignOutReason
import com.fernandocorreia.khepri.core.navigation.AppTab.More
import com.fernandocorreia.khepri.core.navigation.AppTab.Today
import com.fernandocorreia.khepri.core.testing.MainDispatcherExtension
import com.fernandocorreia.khepri.core.testing.TestDispatcherProvider
import kotlin.reflect.KClass
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.extension.RegisterExtension

class NavigationStateTest {
    @JvmField @RegisterExtension val main = MainDispatcherExtension()

    private val signedIn = FakeSignedIn()
    private val registry = NavigationRegistry()
    private var openings = 0
    private lateinit var queue: QueuedNavigator

    private fun TestScope.state(handle: SavedStateHandle = SavedStateHandle()): NavigationState {
        if (!::queue.isInitialized) queue = QueuedNavigator(FakePendingLinks(), backgroundScope)
        return NavigationState(handle, queue, signedIn, registry) { "o${++openings}" }
    }

    private class Probe : ViewModel() {
        var cleared = false

        override fun onCleared() {
            cleared = true
        }
    }

    private fun NavigationState.probe(tab: AppTab, route: Route): Probe {
        val factory =
            object : ViewModelProvider.Factory {
                @Suppress("UNCHECKED_CAST")
                override fun <T : ViewModel> create(modelClass: KClass<T>, extras: CreationExtras): T = Probe() as T
            }
        return ViewModelProvider.create(provider(tab).getOrCreate(route.contentKey()), factory)[Probe::class]
    }

    @Test
    fun `a link opened while signed out is held and lands once signed in`() = runTest {
        signedIn.signedIn.value = false
        val state = state()
        queue.open(AppDestination.Goal("g1"))
        runCurrent()
        assertEquals(Today, state.selected)
        signedIn.signedIn.value = true
        runCurrent()
        assertEquals(More, state.selected)
        assertEquals(listOf(Route.TabRoot(More), Route.Section("goals"), Route.Goal("g1")), state.stack(More))
    }

    @Test
    fun `stacks, the selected tab and crew openings survive process death`() = runTest {
        val handle = SavedStateHandle()
        val state = state(handle)
        queue.open(AppDestination.CrewChannel("66666666-6666-6666-6666-666666666666", "c1"))
        queue.open(AppDestination.Exercise("push-up"))
        runCurrent()
        val restored = state(SavedStateHandle(mapOf("navigation" to handle.get<String>("navigation"))))
        assertEquals(More, restored.selected)
        assertEquals(state.stack(More).toList(), restored.stack(More).toList())
        assertEquals("o1", (restored.stack(More)[2] as Route.CrewSpace).opening)
    }

    @Test
    fun `unreadable saved state starts from the roots`() = runTest {
        val state = state(SavedStateHandle(mapOf("navigation" to "{not json")))
        assertEquals(Today, state.selected)
        AppTab.entries.forEach { assertEquals(listOf(Route.TabRoot(it)), state.stack(it)) }
    }

    @Test
    fun `a stack replaced while not composed clears what left it and nothing that stayed`() = runTest {
        val state = state()
        state.apply(NavCommand.Open(AppDestination.Goal("g1")))
        val root = state.probe(More, Route.TabRoot(More))
        val goal = state.probe(More, Route.Goal("g1"))
        state.apply(NavCommand.Open(AppDestination.Tab(Today)))
        state.apply(NavCommand.Open(AppDestination.Decision("d1")))
        assertTrue(goal.cleared)
        assertFalse(root.cleared)
        assertEquals(setOf(Route.Section("goals").contentKey(), Route.Goal("g1").contentKey()), state.takeStale(More))
        assertTrue(state.takeStale(More).isEmpty())
    }

    @Test
    fun `the composed tab is left to Navigation 3, which clears entries after their exit animation`() = runTest {
        val state = state()
        state.apply(NavCommand.Open(AppDestination.Goal("g1")))
        state.composing(More)
        val goal = state.probe(More, Route.Goal("g1"))
        state.apply(NavCommand.Pop)
        assertFalse(goal.cleared)
        assertTrue(state.takeStale(More).isEmpty())
    }

    @Test
    fun `asks wait for their screen and are consumed once`() = runTest {
        val state = state()
        state.apply(NavCommand.Open(AppDestination.CheckIn(4)))
        state.apply(NavCommand.Open(AppDestination.CheckIn(4)))
        val requests = state.requests.value
        assertEquals(2, requests.size)
        assertEquals(ScreenAsk.CheckInMood(4), requests.first().ask)
        state.consume(requests.first())
        assertEquals(listOf(requests.last()), state.requests.value)
    }

    @Test
    fun `sign-out resets every stack and clears every ViewModel before the phase changes`() = runTest {
        val state = state()
        state.apply(NavCommand.Open(AppDestination.Goal("g1")))
        state.apply(NavCommand.Open(AppDestination.StartWorkout(2)))
        val root = state.probe(Today, Route.TabRoot(Today))
        val epoch = state.epoch
        NavigationSignOutCleaner(registry, TestDispatcherProvider(main.dispatcher)).afterLogout(SignOutReason.Explicit)
        assertTrue(signedIn.signedIn.value)
        assertTrue(root.cleared)
        assertEquals(Today, state.selected)
        AppTab.entries.forEach { assertEquals(listOf(Route.TabRoot(it)), state.stack(it)) }
        assertTrue(state.requests.value.isEmpty())
        assertTrue(state.epoch > epoch)
    }

    @Test
    fun `the signed-out edge resets too, for a screen recreated in the window before it`() = runTest {
        val state = state()
        state.apply(NavCommand.Open(AppDestination.Goal("g1")))
        val epoch = state.epoch
        signedIn.signedIn.value = false
        runCurrent()
        assertEquals(listOf(Route.TabRoot(More)), state.stack(More))
        assertTrue(state.epoch > epoch)
        signedIn.signedIn.value = true
        runCurrent()
        assertEquals(epoch + 1, state.epoch)
    }

    @Test
    fun `a link that arrives after sign-out is kept for the next session`() = runTest {
        val state = state()
        signedIn.signedIn.value = false
        runCurrent()
        queue.open(AppDestination.Section("care"))
        runCurrent()
        signedIn.signedIn.value = true
        runCurrent()
        assertEquals(listOf(Route.TabRoot(More), Route.Section("care")), state.stack(More))
    }
}
