package com.fernandocorreia.khepri.spike.nav3

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextField
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.key
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.saveable.rememberSaveableStateHolder
import androidx.activity.compose.BackHandler
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewmodel.ViewModelStoreProvider
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.unit.dp
import androidx.navigation3.runtime.NavBackStack
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator

class MainActivity : ComponentActivity() {
    /** A link that arrived while the activity was running; consumed by the composition. */
    private val pendingLink = mutableStateOf<String?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // On a cold start from a link the intent carries it; after process death the saved state wins.
        // A relaunch from recents replays the task's original intent; it is not a new link.
        val fromHistory = (intent?.flags ?: 0) and Intent.FLAG_ACTIVITY_LAUNCHED_FROM_HISTORY != 0
        if (savedInstanceState == null && !fromHistory) pendingLink.value = intent?.dataString
        setContent { MaterialTheme { SpikeApp(pendingLink, onExit = { finish() }) } }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        pendingLink.value = intent.dataString
    }
}

@Composable
fun SpikeApp(pendingLink: MutableState<String?>, onExit: () -> Unit) {
    val stacks: Map<Tab, NavBackStack<NavKey>> = Tab.entries.associateWith { rememberNavBackStack(TabRoot(it)) }
    var selected by rememberSaveable { mutableStateOf(Tab.Today) }

    pendingLink.value?.let { link ->
        parseLink(link)?.let { (tab, routes) ->
            val stack = stacks.getValue(tab)
            stack.clear()
            stack.addAll(routes)
            selected = tab
        }
        pendingLink.value = null
    }

    val current = stacks.getValue(selected)
    val top = current.last() as Route
    // One NavDisplay per tab; the tab's whole subtree (entry state included) is saved when it is not selected.
    val tabStates = rememberSaveableStateHolder()
    // Each tab's entry ViewModels live in a store that outlives the tab leaving composition.
    val tabStores: TabStores = viewModel()

    Scaffold(
        bottomBar = {
            if (!top.hidesTabBar()) {
                NavigationBar {
                    Tab.entries.forEach { tab ->
                        NavigationBarItem(
                            modifier = Modifier.testTag("tab-${tab.name.lowercase()}"),
                            selected = tab == selected,
                            onClick = {
                                if (tab == selected) {
                                    // Re-select pops to root.
                                    val stack = stacks.getValue(tab)
                                    while (stack.size > 1) stack.removeAt(stack.lastIndex)
                                } else {
                                    selected = tab
                                }
                            },
                            icon = { Text(tab.name.take(1)) },
                            label = { Text(tab.name) },
                        )
                    }
                }
            }
        }
    ) { padding ->
        // Back pops the tab's stack (NavDisplay), then returns to Today, then exits (design 8.2).
        BackHandler(enabled = current.size == 1 && selected != Tab.Today) { selected = Tab.Today }
        // key() gives each tab its own composition group, so its NavDisplay and decorators are not reused by another tab.
        key(selected) {
        tabStates.SaveableStateProvider(selected.name) {
        NavDisplay(
            backStack = current,
            modifier = Modifier.padding(padding),
            onBack = { if (current.size > 1) current.removeAt(current.lastIndex) else onExit() },
            entryDecorators =
                listOf(
                    rememberSaveableStateHolderNavEntryDecorator(),
                    // The tab's provider outlives the tab leaving composition; only a pop clears an entry's ViewModels.
                    rememberViewModelStoreNavEntryDecorator(tabStores.providerFor(selected)),
                ),
            sceneStrategies = listOf(BottomSheetSceneStrategy()),
            entryProvider =
                entryProvider {
                    entry<TabRoot> { key ->
                        Screen("${key.tab} root") {
                            Button(onClick = { current.add(Detail(key.tab, 1)) }) { Text("Open detail") }
                            if (key.tab == Tab.More) {
                                Button(onClick = { current.add(Goals) }) { Text("Goals") }
                                Button(onClick = { current.add(SettingsSheet) }) { Text("Settings sheet") }
                            }
                            if (key.tab == Tab.Training) {
                                Button(onClick = { current.add(Workout) }) { Text("Start workout") }
                            }
                            if (key.tab == Tab.Coach) {
                                Button(onClick = { current.add(Thread("t1")) }) { Text("Open thread") }
                            }
                        }
                    }
                    entry<Detail> { key ->
                        Screen("${key.tab} detail ${key.depth}") {
                            Button(onClick = { current.add(Detail(key.tab, key.depth + 1)) }) { Text("Deeper") }
                        }
                    }
                    entry<Goals> { Screen("Goals") { Button(onClick = { current.add(Goal("g1")) }) { Text("Goal g1") } } }
                    entry<Goal> { key -> Screen("Goal ${key.id}") {} }
                    entry<SettingsSheet>(metadata = BottomSheetSceneStrategy.bottomSheet()) {
                        Screen("Settings (sheet)") {}
                    }
                    entry<Workout> { Screen("Workout (full screen)") {} }
                    entry<Thread> { key -> Screen("Thread ${key.id} (no tab bar)") {} }
                },
        )
        }
        }
    }
}

/** Each screen keeps a typed field in saveable state, to prove per-entry state survives tab switches and process death. */
@Composable
private fun Screen(title: String, content: @Composable () -> Unit) {
    var note by rememberSaveable { mutableStateOf("") }
    val vm: StampViewModel = viewModel()
    Column(Modifier.fillMaxSize().padding(24.dp)) {
        Text(title, style = MaterialTheme.typography.headlineSmall, modifier = Modifier.testTag("screen-title"))
        Text("vm#${vm.stamp}")
        TextField(value = note, onValueChange = { note = it }, modifier = Modifier.testTag("note"), label = { Text("note") })
        content()
    }
}

/** Numbers ViewModels in creation order, so a recreated one shows a new number. */
class StampViewModel : ViewModel() {
    val stamp: Int = next++

    private companion object {
        var next = 1
    }
}

/** Activity-scoped holder of one ViewModelStoreProvider per tab; cleared with the activity's ViewModels. */
class TabStores : ViewModel() {
    private val stores = mutableMapOf<Tab, ViewModelStore>()
    private val providers = mutableMapOf<Tab, ViewModelStoreProvider>()

    fun providerFor(tab: Tab): ViewModelStoreProvider =
        providers.getOrPut(tab) { ViewModelStoreProvider(stores.getOrPut(tab) { ViewModelStore() }) }

    override fun onCleared() {
        providers.values.forEach { it.clearAllKeys() }
        stores.values.forEach { it.clear() }
    }
}
