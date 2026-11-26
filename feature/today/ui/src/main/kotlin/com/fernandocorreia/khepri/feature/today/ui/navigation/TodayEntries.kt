package com.fernandocorreia.khepri.feature.today.ui.navigation

import androidx.compose.runtime.Composable
import com.fernandocorreia.khepri.core.navigation.AppTab
import com.fernandocorreia.khepri.core.navigation.ui.EntryInstaller
import com.fernandocorreia.khepri.feature.today.ui.today.TodayScreen
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet
import javax.inject.Inject

/** Draws the Today tab's root. */
class TodayEntries @Inject constructor() : EntryInstaller {
    override val roots: Map<AppTab, @Composable () -> Unit> = mapOf(AppTab.Today to { TodayScreen() })
}

@Module
@InstallIn(SingletonComponent::class)
internal abstract class TodayEntriesModule {
    @Binds @IntoSet abstract fun entries(entries: TodayEntries): EntryInstaller
}
