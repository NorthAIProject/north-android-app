package com.fernandocorreia.khepri.core.common

import com.fernandocorreia.khepri.core.model.Outcome
import com.fernandocorreia.khepri.core.model.Session

/** How a feature reports a successful sign-in without seeing `:core:session`. */
public interface SessionGateway {
    /** Keeps the session and loads the account. `Err(Locked)` when the session could not be saved on this phone. */
    public suspend fun signedIn(session: Session): Outcome<Unit>
}
