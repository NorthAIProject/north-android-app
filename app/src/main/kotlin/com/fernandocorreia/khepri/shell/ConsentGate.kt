package com.fernandocorreia.khepri.shell

import com.fernandocorreia.khepri.core.session.AppPhase
import com.fernandocorreia.khepri.feature.auth.domain.usecase.LoadAiConsent
import com.fernandocorreia.khepri.feature.auth.domain.usecase.ObserveAiConsent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * iOS `asksAIConsent`: the AI consent question covers the signed-in app and onboarding while the account has not
 * answered. Entering either phase reads the answer, and so does coming to the foreground in one (iOS reads on every
 * `scenePhase == .active`), so an account that launched offline is asked once the connection is back. A failed read
 * leaves the answer unknown and asks nothing; a known answer is not read again.
 */
internal class ConsentGate(
    private val phase: StateFlow<AppPhase>,
    observe: ObserveAiConsent,
    private val load: LoadAiConsent,
    private val scope: CoroutineScope,
) {
    val asks: StateFlow<Boolean> =
        combine(phase, observe()) { shown, consent -> shown.coveredByConsent() && consent.needsAnswer }
            .stateIn(scope, SharingStarted.Eagerly, false)

    init {
        scope.launch { phase.map { it.coveredByConsent() }.distinctUntilChanged().filter { it }.collect { load() } }
    }

    fun foreground() {
        if (phase.value.coveredByConsent()) scope.launch { load() }
    }
}

internal fun AppPhase.coveredByConsent(): Boolean = this == AppPhase.SignedIn || this == AppPhase.Onboarding
