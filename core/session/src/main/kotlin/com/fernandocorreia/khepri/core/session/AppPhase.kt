package com.fernandocorreia.khepri.core.session

import com.fernandocorreia.khepri.core.model.AppError

/** Which root the app shows. */
public sealed interface AppPhase {
    public data object Launching : AppPhase

    /** A session is stored but cannot be read now. Never signs out; `retry()` tries again. */
    public data object Locked : AppPhase

    public data object SignedOut : AppPhase

    public data object Onboarding : AppPhase

    public data object SignedIn : AppPhase

    /** Signed in, but the account could not be loaded. `retry()` recovers. */
    public data class Failed(val error: AppError) : AppPhase
}
