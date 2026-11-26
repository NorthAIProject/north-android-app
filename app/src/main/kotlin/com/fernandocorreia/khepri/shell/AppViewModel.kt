package com.fernandocorreia.khepri.shell

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStore
import com.fernandocorreia.khepri.core.common.ApplicationScope
import com.fernandocorreia.khepri.core.common.SignOutReason
import com.fernandocorreia.khepri.core.session.AppPhase
import com.fernandocorreia.khepri.core.session.SessionManager
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** The phase the root shows, and the actions the phase screens offer. */
@HiltViewModel
class AppViewModel
@Inject
constructor(private val session: SessionManager, @ApplicationScope private val scope: CoroutineScope) : ViewModel() {
    val phase: StateFlow<AppPhase> = session.phase

    /**
     * The signed-out screens' ViewModels. It outlives rotation and is cleared when those screens leave
     * (`ScopedViewModels`), so nothing typed on the login screen is there after the next sign-out (on iOS the screen's
     * model goes with it).
     */
    val signedOutStore: ViewModelStore = ViewModelStore()

    /** On the application scope: leaving the screen must not cancel a retry or a sign-out halfway. */
    fun retry() {
        scope.launch { session.retry() }
    }

    fun signOut() {
        scope.launch { session.signOut(SignOutReason.Explicit) }
    }

    override fun onCleared() {
        signedOutStore.clear()
    }
}
