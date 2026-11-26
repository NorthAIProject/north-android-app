package com.fernandocorreia.khepri.shell

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.viewModelScope
import com.fernandocorreia.khepri.core.common.ApplicationScope
import com.fernandocorreia.khepri.core.common.SignOutReason
import com.fernandocorreia.khepri.core.session.AppPhase
import com.fernandocorreia.khepri.core.session.SessionManager
import com.fernandocorreia.khepri.feature.auth.domain.usecase.LoadAiConsent
import com.fernandocorreia.khepri.feature.auth.domain.usecase.ObserveAiConsent
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/** The phase the root shows, and the actions the phase screens offer. */
@HiltViewModel
class AppViewModel
@Inject
constructor(
    private val session: SessionManager,
    loadConsent: LoadAiConsent,
    observeConsent: ObserveAiConsent,
    @ApplicationScope private val scope: CoroutineScope,
) : ViewModel() {
    val phase: StateFlow<AppPhase> = session.phase

    /** Whether the AI consent question covers the app. */
    val asksConsent: StateFlow<Boolean> = ConsentGate(phase, observeConsent, loadConsent, viewModelScope).asks

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
