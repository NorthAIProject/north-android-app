package com.fernandocorreia.khepri.shell

import androidx.activity.compose.LocalActivity
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.remember
import androidx.lifecycle.HasDefaultViewModelProviderFactory
import androidx.lifecycle.VIEW_MODEL_STORE_OWNER_KEY
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.MutableCreationExtras
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner

/**
 * ViewModels created inside [content] live in [store], made by the enclosing owner's (Hilt) factory. The store is
 * cleared when [content] leaves the screen for good (not for a rotation), so the screens' ViewModels finish what they
 * were doing first, as a sign-in that sends its last effect while the phase cross-fades away.
 *
 * A `SavedStateHandle` here is kept in [store] and goes with it; it is not saved across process death.
 */
@Composable
internal fun ScopedViewModels(store: ViewModelStore, content: @Composable () -> Unit) {
    val parent = checkNotNull(LocalViewModelStoreOwner.current) { "no ViewModelStoreOwner" }
    val activity = LocalActivity.current
    val owner = remember(store, parent) { ScopedOwner(store, parent) }
    DisposableEffect(store) { onDispose { if (activity?.isChangingConfigurations != true) store.clear() } }
    CompositionLocalProvider(LocalViewModelStoreOwner provides owner, content = content)
}

private class ScopedOwner(override val viewModelStore: ViewModelStore, parent: ViewModelStoreOwner) :
    ViewModelStoreOwner, HasDefaultViewModelProviderFactory {
    private val defaults = parent as HasDefaultViewModelProviderFactory

    override val defaultViewModelProviderFactory: ViewModelProvider.Factory
        get() = defaults.defaultViewModelProviderFactory

    /** This owner, not the activity, holds the handles: they must not outlive [viewModelStore]. */
    override val defaultViewModelCreationExtras: CreationExtras
        get() =
            MutableCreationExtras(defaults.defaultViewModelCreationExtras).apply {
                set(VIEW_MODEL_STORE_OWNER_KEY, this@ScopedOwner)
            }
}
