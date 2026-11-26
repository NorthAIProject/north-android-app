package com.fernandocorreia.khepri.core.navigation

import com.fernandocorreia.khepri.core.common.SignedInState
import kotlinx.coroutines.flow.MutableStateFlow

class FakeSignedIn(initial: Boolean = true) : SignedInState {
    override val signedIn = MutableStateFlow(initial)
}

class FakePendingLinks : PendingLinks {
    val kept = mutableListOf<AppDestination>()
    var failure: Exception? = null

    override suspend fun put(destination: AppDestination) {
        failure?.let { throw it }
        kept += destination
    }

    override suspend fun take(): List<AppDestination> = kept.toList().also { kept.clear() }
}
