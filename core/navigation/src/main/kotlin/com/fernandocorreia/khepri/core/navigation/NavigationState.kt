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
// The navigation facade TabHost and the shell call; per-tab store bookkeeping already lives in TabMemory.
@Suppress("TooManyFunctions")
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
    private val memory = TabMemory()

    /** The account the stacks belong to; another account starts from the roots. */
    private var owner: String? = restored.owner
    private val asks = MutableStateFlow<List<ScreenRequest>>(emptyList())
    private var nextAsk = 0L

    /** The tab whose stack is composed now, reported by `TabHost`; null while none is. */
    private var composed: AppTab? = null

    public var selected: AppTab by mutableStateOf(restored.selected)
        private set

    /** Changes on every reset, so the host drops all composed state with the stacks. */
    public var epoch: Int by mutableIntStateOf(restored.epoch)
        private set

    /**
     * True from the sign-out reset until the next sign-in: the host draws nothing, so no screen loads with a session
     * that is being torn down.
     */
    public var suspended: Boolean by mutableStateOf(false)
        private set

    /** Asks waiting for their screen. */
    public val requests: StateFlow<List<ScreenRequest>> = asks.asStateFlow()

    init {
        registry.live = this
        viewModelScope.launch {
            var first = true
            var last: String? = null
            combine(signedInState.account, queue.pending, ::Pair).collect { (account, pending) ->
                if (first || account != last) {
                    if (account != null) arrived(account) else if (last != null) reset()
                    first = false
                    last = account
                }
                if (account != null && pending.isNotEmpty()) queue.takeAll().forEach(::apply)
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

    public fun provider(tab: AppTab): ViewModelStoreProvider = memory.provider(tab)

    public fun consume(request: ScreenRequest) {
        asks.update { it - request }
    }

    /** Called by `TabHost` when [tab]'s stack is composed. A tab still marked composed is settled first. */
    public fun composing(tab: AppTab) {
        composed?.takeIf { it != tab }?.let { memory.settle(it, stack(it).keys()) }
        composed = tab
        memory.saw(tab, stack(tab).keys())
    }

    /**
     * Called by `TabHost` when [tab]'s stack leaves composition. The tab is settled: entries removed in the frame it
     * left in were never seen leaving by Navigation 3.
     */
    public fun leaving(tab: AppTab) {
        memory.settle(tab, stack(tab).keys())
        if (composed == tab) composed = null
    }

    /** Keys removed from [tab] while it was not composed: their saved state must go before the tab shows again. */
    public fun takeStale(tab: AppTab): Set<String> = memory.takeStale(tab)

    internal fun apply(command: NavCommand) {
        val before = TabStacks(selected, stacks.mapValues { (_, stack) -> stack.toList() })
        val plan = NavPlanner.plan(before, command, newOpening)
        AppTab.entries.forEach { tab -> replace(tab, before.stack(tab), plan.state.stack(tab)) }
        selected = plan.state.selected
        plan.asks.forEach { ask -> asks.update { it + ScreenRequest(nextAsk++, ask) } }
        save()
    }

    /** Sign-out: reset, and draw nothing until the next sign-in. */
    internal fun signingOut() {
        reset()
        suspended = true
    }

    /**
     * Back to every tab's root on Today with nothing kept: the next account never sees the last one's screens. Commands
     * queued by the last session are dropped unless [keepQueue].
     */
    public fun reset(keepQueue: Boolean = false) {
        memory.clear()
        stacks.forEach { (tab, stack) ->
            stack.clear()
            stack.add(Route.TabRoot(tab))
        }
        owner = null
        selected = AppTab.Today
        asks.value = emptyList()
        if (!keepQueue) queue.drop()
        epoch++
        save()
    }

    override fun onCleared() {
        if (registry.live === this) registry.live = null
        memory.clear()
    }

    /**
     * A composed tab's entries are cleared by the Navigation 3 decorators when they leave the stack. A tab that is not
     * composed never sees them leave, so its removed entries' ViewModels are cleared here and their saved state later.
     */
    private fun replace(tab: AppTab, old: List<Route>, new: List<Route>) {
        if (old == new) return
        if (tab != composed) memory.discard(tab, old.keys() - new.keys()) else memory.saw(tab, new.keys())
        val stack = stacks.getValue(tab)
        stack.clear()
        stack.addAll(new)
    }

    private fun arrived(account: String) {
        if (owner != null && owner != account) reset(keepQueue = true)
        owner = account
        suspended = false
        save()
    }

    private fun save() {
        val state = TabStacks(selected, stacks.mapValues { (_, stack) -> stack.toList() })
        SavedStacks.write(handle, SavedStacks.Snapshot(state, owner, epoch))
    }
}

private fun List<Route>.keys(): Set<String> = map { it.contentKey() }.toSet()

/**
 * Each tab's ViewModel store, the keys it showed while composed, and the keys whose saved state must go before it shows
 * again.
 */
private class TabMemory {
    private val stores = AppTab.entries.associateWith { ViewModelStore() }
    private val providers = stores.mapValues { (_, store) -> ViewModelStoreProvider(store) }
    private val stale = AppTab.entries.associateWith { mutableSetOf<String>() }
    private val shown = AppTab.entries.associateWith { mutableSetOf<String>() }

    fun provider(tab: AppTab): ViewModelStoreProvider = providers.getValue(tab)

    fun saw(tab: AppTab, keys: Set<String>) {
        shown.getValue(tab).addAll(keys)
    }

    /** Clears what [tab] showed while composed and no longer holds. Clearing twice is harmless. */
    fun settle(tab: AppTab, current: Set<String>) {
        val seen = shown.getValue(tab)
        discard(tab, seen - current)
        seen.clear()
    }

    fun discard(tab: AppTab, keys: Set<String>) {
        keys.forEach { key ->
            provider(tab).clearKey(key)
            stale.getValue(tab).add(key)
        }
    }

    fun takeStale(tab: AppTab): Set<String> = stale.getValue(tab).toSet().also { stale.getValue(tab).clear() }

    fun clear() {
        providers.values.forEach { it.clearAllKeys() }
        stores.values.forEach { it.clear() }
        stale.values.forEach { it.clear() }
        shown.values.forEach { it.clear() }
    }
}

/**
 * The stacks as JSON in [SavedStateHandle], for process death, with the account they belong to and the host's epoch.
 * Sheets are not saved: iOS never restores one. Anything unreadable starts from the roots.
 */
private object SavedStacks {
    private const val KEY = "navigation"
    private val json = Json { ignoreUnknownKeys = true }

    data class Snapshot(val state: TabStacks, val owner: String?, val epoch: Int) {
        val selected: AppTab
            get() = state.selected

        fun stack(tab: AppTab): List<Route> = state.stack(tab)
    }

    @Serializable
    private data class Saved(
        val selected: AppTab,
        val stacks: Map<AppTab, List<Route>>,
        val owner: String? = null,
        val epoch: Int = 0,
    )

    fun write(handle: SavedStateHandle, snapshot: Snapshot) {
        val stacks = snapshot.state.stacks.mapValues { (_, stack) -> stack.dropLastWhile { it is Sheet } }
        handle[KEY] = json.encodeToString(Saved(snapshot.selected, stacks, snapshot.owner, snapshot.epoch))
    }

    fun read(handle: SavedStateHandle): Snapshot {
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
        val state =
            saved?.let { TabStacks(it.selected, it.stacks.filterValues { stack -> stack.isNotEmpty() }) }
                ?: TabStacks.Roots
        return Snapshot(state, saved?.owner, saved?.epoch ?: 0)
    }
}
