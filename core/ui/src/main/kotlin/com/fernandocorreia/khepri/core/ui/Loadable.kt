package com.fernandocorreia.khepri.core.ui

import com.fernandocorreia.khepri.core.model.AppError
import java.time.Instant

/**
 * What a screen shows. A failed refresh keeps [Content] (with [Content.staleSince]); only an empty screen [Failed]s.
 */
public sealed interface Loadable<out T> {
    public data object Loading : Loadable<Nothing>

    public data class Content<T>(val value: T, val refreshing: Boolean = false, val staleSince: Instant? = null) :
        Loadable<T>

    public data class Failed(val error: AppError) : Loadable<Nothing>
}

/** The state of a save. A failed save shows inline and never blanks the screen. */
public sealed interface ActionState {
    public data object Idle : ActionState

    public data object Running : ActionState

    public data class Failed(val error: AppError) : ActionState
}
