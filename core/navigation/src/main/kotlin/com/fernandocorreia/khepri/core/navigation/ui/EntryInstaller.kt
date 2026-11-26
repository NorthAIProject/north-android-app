package com.fernandocorreia.khepri.core.navigation.ui

import androidx.compose.runtime.Composable
import androidx.navigation3.runtime.EntryProviderScope
import com.fernandocorreia.khepri.core.navigation.Route
import com.fernandocorreia.khepri.core.navigation.Sheet
import com.fernandocorreia.khepri.core.navigation.contentKey

/** Contributes a feature's screens to every tab. Multibound with Hilt; route keys live in `:core:navigation`. */
public interface EntryInstaller {
    public fun EntryProviderScope<Route>.install()
}

/** Registers a screen for [T] under [contentKey], as a bottom sheet when [T] is a [Sheet]. */
public inline fun <reified T : Route> EntryProviderScope<Route>.screen(noinline content: @Composable (T) -> Unit) {
    entry<T>(
        clazzContentKey = { it.contentKey() },
        metadata =
            if (Sheet::class.java.isAssignableFrom(T::class.java)) BottomSheetSceneStrategy.bottomSheet()
            else emptyMap(),
        content = content,
    )
}
