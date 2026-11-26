package com.fernandocorreia.khepri.core.common

import kotlinx.coroutines.flow.StateFlow

/**
 * Who is signed in. Lets `:core:navigation` hold links, and tie saved stacks to an account, without `:core:session`.
 */
public interface SignedInState {
    /** The signed-in user's id while the phase is SignedIn; null in every other phase. */
    public val account: StateFlow<String?>
}
