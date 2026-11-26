package com.fernandocorreia.khepri.core.navigation

import com.fernandocorreia.khepri.core.common.DispatcherProvider
import com.fernandocorreia.khepri.core.common.SignOutCleaner
import com.fernandocorreia.khepri.core.common.SignOutReason
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.withContext

/** Holds the live [NavigationState], if an activity has one, so sign-out can reach it. */
// A handle to the activity's state, not account data: NavigationSignOutCleaner resets that state at sign-out.
@Suppress("AccountStateNeedsCleaner")
@Singleton
public class NavigationRegistry @Inject constructor() {
    @Volatile internal var live: NavigationState? = null
}

/**
 * Resets every stack and ViewModel store on the main thread before the phase becomes signed out, and suspends the host
 * until then.
 */
internal class NavigationSignOutCleaner
@Inject
constructor(private val registry: NavigationRegistry, private val dispatchers: DispatcherProvider) : SignOutCleaner {
    override suspend fun afterLogout(reason: SignOutReason) {
        withContext(dispatchers.main) { registry.live?.signingOut() }
    }
}
