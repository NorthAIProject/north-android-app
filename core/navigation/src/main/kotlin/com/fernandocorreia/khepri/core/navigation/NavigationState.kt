package com.fernandocorreia.khepri.core.navigation

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.ViewModelStoreProvider
import com.fernandocorreia.khepri.core.common.SignedInState
import dagger.hilt.android.lifecycle.HiltViewModel
import java.util.UUID
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.serialization.Serializable
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json

/**
 * The per-tab back stacks, the selected tab and each tab's ViewModel store, for the whole activity: it sits above the
 * phase switch, so a phase change keeps the stacks and sign-out clears them. Saved for process death in
 * [SavedStateHandle]. Applies `Navigator` commands while signed in.
 */
@HiltViewModel
public class NavigationState
internal constructor(
    private val handle: SavedStateHandle,
    private val queue: QueuedNavigator,
    signedInState: SignedInState,
    private val registry: NavigationRegistry,
    private val newOpening: () -> String,
) : ViewModel() {
    @Inject
    public constructor(
        handle: SavedStateHandle,
        queue: QueuedNavigator,
        signedInState: SignedInState,
        registry: NavigationRegistry,
    ) : this(handle, queue, signedInState, registry, { UUID.randomUUID().toString() })

    private val restored = SavedStacks.read(handle)
    private val stacks: Map<AppTab, SnapshotStateList<Route>> =
        AppTab.entries.associateWith { tab -> mutableStateListOf<Route>().apply { addAll(restored.stack(tab)) } }
    private val stores = AppTab.entries.associateWith { ViewModelStore() }
    private val providers = stores.mapValues { (_, store) -> ViewModelStoreProvider(store) }
    private val stale = AppTab.entries.associateWith { mutableSetOf<String>() }
    private val asks = MutableStateFlow<List<ScreenRequest>>(emptyList())
    private var nextAsk = 0L

    /** The tab whose stack is composed now, reported by `TabHost`; null while none is. */
    private var composed: AppTab? = null

    public var selected: AppTab by mutableStateOf(restored.selected)
        private set

    /** Changes on every reset, so the host drops all composed state with the stacks. */
    public var epoch: Int by mutableIntStateOf(0)
        private set

    /** Asks waiting for their screen. */
    public val requests: StateFlow<List<ScreenRequest>> = asks.asStateFlow()

    init {
        registry.live = this
        viewModelScope.launch {
            combine(signedInState.signedIn, queue.pending) { signedIn, pending -> signedIn && pending.isNotEmpty() }
                .filter { it }
                .collect { queue.takeAll().forEach(::apply) }
        }
        viewModelScope.launch {
            var wasSignedIn = signedInState.signedIn.value
            signedInState.signedIn.collect { signedIn ->
                if (wasSignedIn && !signedIn) reset()
                wasSignedIn = signedIn
            }
        }
    }

    public fun stack(tab: AppTab): List<Route> = stacks.getValue(tab)

    /** True when the selected tab's top screen (sheets aside) is full screen. */
    public val hidesTabBar: Boolean
        get() = stack(selected).hidesTabBar()

    /** A tab bar tap: selects [tab], or on the selected tab pops to its root and asks the root to reload. */
    public fun select(tab: AppTab) {
        apply(NavCommand.Select(tab))
    }

    public fun provider(tab: AppTab): ViewModelStoreProvider = providers.getValue(tab)

    public fun consume(request: ScreenRequest) {
        asks.update { it - request }
    }

    /** Called by `TabHost` while [tab]'s stack is composed, and with null when it leaves. */
    public fun composing(tab: AppTab?) {
        composed = tab
    }

    /** Keys removed from [tab] while it was not composed: their saved state must go before the tab shows again. */
    public fun takeStale(tab: AppTab): Set<String> {
        val keys = stale.getValue(tab).toSet()
        stale.getValue(tab).clear()
        return keys
    }

    internal fun apply(command: NavCommand) {
        val before = TabStacks(selected, stacks.mapValues { (_, stack) -> stack.toList() })
        val plan = NavPlanner.plan(before, command, newOpening)
        AppTab.entries.forEach { tab -> replace(tab, before.stack(tab), plan.state.stack(tab)) }
        selected = plan.state.selected
        plan.asks.forEach { ask -> asks.update { it + ScreenRequest(nextAsk++, ask) } }
        save()
    }

    /** Back to every tab's root on Today with nothing kept: the next account never sees the last one's screens. */
    public fun reset() {
        providers.values.forEach { it.clearAllKeys() }
        stores.values.forEach { it.clear() }
        stacks.forEach { (tab, stack) ->
            stack.clear()
            stack.add(Route.TabRoot(tab))
        }
        stale.values.forEach { it.clear() }
        selected = AppTab.Today
        asks.value = emptyList()
        queue.drop()
        epoch++
        save()
    }

    override fun onCleared() {
        if (registry.live === this) registry.live = null
        providers.values.forEach { it.clearAllKeys() }
        stores.values.forEach { it.clear() }
    }

    /**
     * A composed tab's entries are cleared by the Navigation 3 decorators when they leave the stack. A tab that is not
     * composed never sees them leave, so its removed entries' ViewModels are cleared here and their saved state later.
     */
    private fun replace(tab: AppTab, old: List<Route>, new: List<Route>) {
        if (old == new) return
        if (tab != composed) {
            val kept = new.map { it.contentKey() }.toSet()
            old.map { it.contentKey() }
                .filterNot(kept::contains)
                .forEach { key ->
                    provider(tab).clearKey(key)
                    stale.getValue(tab).add(key)
                }
        }
        val stack = stacks.getValue(tab)
        stack.clear()
        stack.addAll(new)
    }

    private fun save() {
        SavedStacks.write(handle, TabStacks(selected, stacks.mapValues { (_, stack) -> stack.toList() }))
    }
}

/** The stacks as JSON in [SavedStateHandle], for process death. Anything unreadable starts from the roots. */
private object SavedStacks {
    private const val KEY = "navigation"
    private val json = Json { ignoreUnknownKeys = true }

    @Serializable private data class Saved(val selected: AppTab, val stacks: Map<AppTab, List<Route>>)

    fun write(handle: SavedStateHandle, state: TabStacks) {
        handle[KEY] = json.encodeToString(Saved(state.selected, state.stacks))
    }

    fun read(handle: SavedStateHandle): TabStacks {
        val saved =
            handle.get<String>(KEY)?.let {
                try {
                    json.decodeFromString<Saved>(it)
                } catch (expected: SerializationException) {
                    null
                } catch (expected: IllegalArgumentException) {
                    null
                }
            }
        return saved?.let { TabStacks(it.selected, it.stacks.filterValues { stack -> stack.isNotEmpty() }) }
            ?: TabStacks.Roots
    }
}
