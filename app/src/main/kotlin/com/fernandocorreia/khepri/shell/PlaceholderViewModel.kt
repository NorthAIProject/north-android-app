package com.fernandocorreia.khepri.shell

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import com.fernandocorreia.khepri.core.common.ApplicationScope
import com.fernandocorreia.khepri.core.common.SignOutReason
import com.fernandocorreia.khepri.core.navigation.Route
import com.fernandocorreia.khepri.core.session.SessionManager
import dagger.assisted.Assisted
import dagger.assisted.AssistedFactory
import dagger.assisted.AssistedInject
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch

/**
 * Stands in for a screen not ported yet. It is the pattern every screen ViewModel follows: Hilt, the route as an
 * assisted argument, state in [SavedStateHandle] under the tab's store. [taps] proves that state survives a process
 * kill. Only sign-out and retry run on [ApplicationScope]; a screen's work runs on `viewModelScope`, and work that must
 * outlive the screen but belongs to the account runs on `@AccountScope`.
 */
@HiltViewModel(assistedFactory = PlaceholderViewModel.Factory::class)
class PlaceholderViewModel
@AssistedInject
constructor(
    @Assisted val route: Route,
    private val saved: SavedStateHandle,
    private val session: SessionManager,
    @ApplicationScope private val scope: CoroutineScope,
) : ViewModel() {
    val taps: StateFlow<Int> = saved.getStateFlow(TAPS, 0)

    fun tap() {
        saved[TAPS] = taps.value + 1
    }

    fun signOut() {
        scope.launch { session.signOut(SignOutReason.Explicit) }
    }

    @AssistedFactory
    interface Factory {
        fun create(route: Route): PlaceholderViewModel
    }

    private companion object {
        const val TAPS = "taps"
    }
}
