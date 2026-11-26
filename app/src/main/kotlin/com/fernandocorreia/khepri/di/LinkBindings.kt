package com.fernandocorreia.khepri.di

import android.content.Context
import androidx.datastore.core.handlers.ReplaceFileCorruptionHandler
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.emptyPreferences
import androidx.datastore.preferences.preferencesDataStoreFile
import com.fernandocorreia.khepri.core.common.ApplicationScope
import com.fernandocorreia.khepri.core.navigation.PendingLinks
import com.fernandocorreia.khepri.core.navigation.ui.EntryInstaller
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.Multibinds
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.plus

@Module
@InstallIn(SingletonComponent::class)
internal abstract class LinkBindings {
    /** Features add screens one by one; the set may be empty. */
    @Multibinds abstract fun installers(): Set<EntryInstaller>

    companion object {
        /** Its own file, as the session's: neither is cleared with the other. */
        @Provides
        @Singleton
        fun pendingLinks(
            @ApplicationContext context: Context,
            @ApplicationScope scope: CoroutineScope,
        ): PendingLinks =
            PendingLinksStore(
                PreferenceDataStoreFactory.create(
                    corruptionHandler = ReplaceFileCorruptionHandler { emptyPreferences() },
                    scope = scope + Dispatchers.IO,
                ) {
                    context.preferencesDataStoreFile("pending_links")
                }
            )
    }
}
