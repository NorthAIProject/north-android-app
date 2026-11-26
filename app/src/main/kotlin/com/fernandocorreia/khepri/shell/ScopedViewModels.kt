package com.fernandocorreia.khepri.shell

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.remember
import androidx.lifecycle.HasDefaultViewModelProviderFactory
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.viewmodel.CreationExtras
import androidx.lifecycle.viewmodel.compose.LocalViewModelStoreOwner

/** ViewModels created inside [content] live in [store], made by the enclosing owner's (Hilt) factory. */
@Composable
internal fun ScopedViewModels(store: ViewModelStore, content: @Composable () -> Unit) {
    val parent = checkNotNull(LocalViewModelStoreOwner.current) { "no ViewModelStoreOwner" }
    val owner = remember(store, parent) { ScopedOwner(store, parent) }
    CompositionLocalProvider(LocalViewModelStoreOwner provides owner, content = content)
}

private class ScopedOwner(override val viewModelStore: ViewModelStore, parent: ViewModelStoreOwner) :
    ViewModelStoreOwner, HasDefaultViewModelProviderFactory {
    private val defaults = parent as HasDefaultViewModelProviderFactory

    override val defaultViewModelProviderFactory: ViewModelProvider.Factory
        get() = defaults.defaultViewModelProviderFactory

    override val defaultViewModelCreationExtras: CreationExtras
        get() = defaults.defaultViewModelCreationExtras
}
