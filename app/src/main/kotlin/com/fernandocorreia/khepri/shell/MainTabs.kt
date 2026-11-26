package com.fernandocorreia.khepri.shell

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import com.fernandocorreia.khepri.core.designsystem.theme.KhepriTheme
import com.fernandocorreia.khepri.core.navigation.AppTab
import com.fernandocorreia.khepri.core.navigation.NavigationState
import com.fernandocorreia.khepri.core.navigation.ui.EntryInstaller
import com.fernandocorreia.khepri.core.navigation.ui.TabHost
import com.fernandocorreia.khepri.core.ui.R

/**
 * iOS `MainTabView`: five tabs, one stack each. The bar hides under a full-screen route. Labels only until the icon set
 * lands (Phase 2).
 */
@Composable
internal fun MainTabs(state: NavigationState, installers: Set<EntryInstaller>, modifier: Modifier = Modifier) {
    Scaffold(
        modifier = modifier,
        containerColor = KhepriTheme.colors.background,
        bottomBar = { if (!state.hidesTabBar) TabBar(state.selected, onSelect = state::select) },
    ) { padding ->
        Box(Modifier.padding(padding)) {
            TabHost(state, installers, unported = { route -> PlaceholderScreen(route) })
        }
    }
}

@Composable
private fun TabBar(selected: AppTab, onSelect: (AppTab) -> Unit) {
    NavigationBar(containerColor = KhepriTheme.colors.card) {
        AppTab.entries.forEach { tab ->
            NavigationBarItem(
                selected = tab == selected,
                onClick = { onSelect(tab) },
                icon = {},
                label = { Text(stringResource(tab.label())) },
                modifier = Modifier.testTag("tab-${tab.name.lowercase()}"),
            )
        }
    }
}

internal fun AppTab.label(): Int =
    when (this) {
        AppTab.Today -> R.string.today
        AppTab.Coach -> R.string.coach
        AppTab.Training -> R.string.training
        AppTab.Progress -> R.string.progress
        AppTab.More -> R.string.more
    }
