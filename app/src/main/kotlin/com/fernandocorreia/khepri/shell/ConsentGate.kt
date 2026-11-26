package com.fernandocorreia.khepri.shell

import com.fernandocorreia.khepri.core.session.AppPhase
import com.fernandocorreia.khepri.feature.auth.domain.usecase.LoadAiConsent
import com.fernandocorreia.khepri.feature.auth.domain.usecase.ObserveAiConsent
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.Flow
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
 * answered. Entering either phase reads the answer; a failed read leaves it unknown and asks nothing.
 */
internal class ConsentGate(
    phase: Flow<AppPhase>,
    observe: ObserveAiConsent,
    load: LoadAiConsent,
    scope: CoroutineScope,
) {
    val asks: StateFlow<Boolean> =
        combine(phase, observe()) { shown, consent -> shown.coveredByConsent() && consent.needsAnswer }
            .stateIn(scope, SharingStarted.Eagerly, false)

    init {
        scope.launch { phase.map { it.coveredByConsent() }.distinctUntilChanged().filter { it }.collect { load() } }
    }
}

internal fun AppPhase.coveredByConsent(): Boolean = this == AppPhase.SignedIn || this == AppPhase.Onboarding
