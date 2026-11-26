package com.fernandocorreia.khepri.core.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.withContext

/**
 * Collects a screen's one-shot effects while it is at least started. An effect sent while the app is in the background
 * waits in the ViewModel's channel and runs on return, so a navigation or snackbar never fires off screen and is never
 * lost. Every screen collects its `Effect` channel through this.
 */
@Composable
public fun <T> CollectEffects(effects: Flow<T>, onEffect: suspend (T) -> Unit) {
    val owner = LocalLifecycleOwner.current
    val handle by rememberUpdatedState(onEffect)
    LaunchedEffect(effects, owner) {
        owner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            withContext(Dispatchers.Main.immediate) { effects.collect { handle(it) } }
        }
    }
}
