package com.fernandocorreia.khepri.core.navigation

import com.fernandocorreia.khepri.core.common.SignedInState
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.flow.MutableStateFlow

class FakeSignedIn(initial: String? = "u1") : SignedInState {
    override val account = MutableStateFlow(initial)
}

class FakePendingLinks : PendingLinks {
    val kept = mutableListOf<AppDestination>()
    var failure: Exception? = null

    /** When set, `put` waits for it: a slow disk. */
    var gate: CompletableDeferred<Unit>? = null

    override suspend fun put(destination: AppDestination) {
        gate?.await()
        failure?.let { throw it }
        kept += destination
    }

    override suspend fun take(): List<AppDestination> = kept.toList().also { kept.clear() }
}
