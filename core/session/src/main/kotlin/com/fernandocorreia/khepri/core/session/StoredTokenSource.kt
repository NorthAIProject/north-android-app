package com.fernandocorreia.khepri.core.session

import com.fernandocorreia.khepri.core.datastore.SessionStore
import com.fernandocorreia.khepri.core.network.TokenSource
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow

/** `onUnauthorized()` posts here and [SessionManager] collects, which breaks the token → client → manager cycle. */
@Singleton
public class UnauthorizedSignal @Inject constructor() {
    private val flow = MutableSharedFlow<Unit>(extraBufferCapacity = 1, onBufferOverflow = BufferOverflow.DROP_OLDEST)
    public val events: SharedFlow<Unit> = flow

    public fun post() {
        flow.tryEmit(Unit)
    }
}

/**
 * The only [TokenSource]. A protected request that finds no session (its sealed data was lost while signed in) also
 * posts the signal, so the app signs out instead of sending bare requests; the manager ignores it when signed out.
 */
public class StoredTokenSource
@Inject
constructor(private val store: SessionStore, private val signal: UnauthorizedSignal) : TokenSource {
    override suspend fun token(): String? = store.read()?.token.also { if (it == null) signal.post() }

    override suspend fun onUnauthorized(sentToken: String) {
        val current = runCatching { store.read()?.token }.getOrNull()
        if (current == sentToken) signal.post()
    }
}
