package com.fernandocorreia.khepri.core.navigation.ui

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.ProvidableCompositionLocal
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.staticCompositionLocalOf
import com.fernandocorreia.khepri.core.navigation.ScreenAsk
import com.fernandocorreia.khepri.core.navigation.ScreenRequest
import kotlinx.coroutines.flow.StateFlow

/** The one-shot asks waiting for their screen (`NavigationState`), provided by `TabHost`. */
public interface ScreenAsks {
    public val requests: StateFlow<List<ScreenRequest>>

    public fun consume(request: ScreenRequest)
}

/** Null outside `TabHost` (previews, screenshot tests): [ConsumeAsks] then does nothing. */
public val LocalScreenAsks: ProvidableCompositionLocal<ScreenAsks?> = staticCompositionLocalOf { null }

/**
 * Takes each ask that [pick] maps to a [T], removes it so it runs once, and hands it to [onAsk]. Asks not picked stay
 * for the screen they are meant for.
 */
@Composable
public fun <T : Any> ConsumeAsks(pick: (ScreenAsk) -> T?, onAsk: (T) -> Unit) {
    val asks = LocalScreenAsks.current ?: return
    val latestPick by rememberUpdatedState(pick)
    val handle by rememberUpdatedState(onAsk)
    LaunchedEffect(asks) {
        asks.requests.collect { pending ->
            pending.forEach { request ->
                latestPick(request.ask)?.let {
                    asks.consume(request)
                    handle(it)
                }
            }
        }
    }
}
