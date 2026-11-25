package com.fernandocorreia.khepri.core.network

/** Where requests get the session token. Implemented in `:core:session`. */
public interface TokenSource {
    /** Null when signed out. Throws `TokenUnreadableException` when a token exists but cannot be read. */
    public suspend fun token(): String?

    /** Called after a 401 on a request that carried a token. Must be cheap and idempotent. */
    public fun onUnauthorized()
}

/** The phone's languages, most preferred first, as BCP 47 tags. */
public fun interface PreferredLanguages {
    public fun get(): List<String>
}
