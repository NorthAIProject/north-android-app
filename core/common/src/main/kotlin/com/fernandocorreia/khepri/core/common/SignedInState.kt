package com.fernandocorreia.khepri.core.common

import kotlinx.coroutines.flow.StateFlow

/** True while the phase is SignedIn. Lets `:core:navigation` hold links without seeing `:core:session`. */
public interface SignedInState {
    public val signedIn: StateFlow<Boolean>
}
