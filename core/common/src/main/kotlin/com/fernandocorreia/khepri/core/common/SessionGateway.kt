package com.fernandocorreia.khepri.core.common

import com.fernandocorreia.khepri.core.model.Session

/** How a feature reports a successful sign-in without seeing `:core:session`. */
public interface SessionGateway {
    public suspend fun signedIn(session: Session)
}
