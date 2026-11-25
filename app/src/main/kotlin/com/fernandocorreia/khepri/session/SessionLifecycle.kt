package com.fernandocorreia.khepri.session

import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.ProcessLifecycleOwner
import com.fernandocorreia.khepri.core.common.ApplicationScope
import com.fernandocorreia.khepri.core.session.AppPhase
import com.fernandocorreia.khepri.core.session.SessionManager
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.launch

/**
 * Starts the session at launch, and while it is `Locked` retries each time the app comes forward, the way iOS retries
 * when protected data becomes available. (The app is not direct-boot aware, so it never runs before the first unlock.)
 */
class SessionLifecycle
@Inject
constructor(private val manager: SessionManager, @ApplicationScope private val scope: CoroutineScope) {
    fun install() {
        scope.launch { manager.start() }
        ProcessLifecycleOwner.get()
            .lifecycle
            .addObserver(
                object : DefaultLifecycleObserver {
                    override fun onStart(owner: LifecycleOwner) = retryIfLocked()
                }
            )
    }

    private fun retryIfLocked() {
        if (manager.phase.value == AppPhase.Locked) scope.launch { manager.retry() }
    }
}
