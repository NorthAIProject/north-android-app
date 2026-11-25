package com.fernandocorreia.khepri.core.common

public enum class SignOutReason {
    Explicit,
    Unauthorized,
}

/** Multibound with Hilt; any module registers one. */
public interface SignOutCleaner {
    /** Server calls that need a live token (push unregister). Run only for Explicit, before logout. */
    public suspend fun beforeLogout() {}

    /** Local cleanup. Run for both reasons, after the session is gone. */
    public suspend fun afterLogout(reason: SignOutReason)
}
