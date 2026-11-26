package com.fernandocorreia.khepri.feature.auth.domain.repository

import com.fernandocorreia.khepri.core.model.Outcome
import com.fernandocorreia.khepri.feature.auth.domain.model.AiConsent
import kotlinx.coroutines.flow.StateFlow

/** The account's AI data sharing answer. The server enforces it; the app uses it to decide which screens to show. */
interface AiConsentRepository {
    /** Starts as [AiConsent.Unknown]. */
    val consent: StateFlow<AiConsent>

    /** Reads the answer, unless it is already known (iOS `load()`). */
    suspend fun load(): Outcome<Unit>

    suspend fun decide(allow: Boolean): Outcome<Unit>

    /** Forgets the answer, so the next [load] asks the server. */
    fun reset()
}
