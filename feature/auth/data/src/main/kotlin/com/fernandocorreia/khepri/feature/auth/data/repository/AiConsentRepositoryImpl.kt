package com.fernandocorreia.khepri.feature.auth.data.repository

import com.fernandocorreia.khepri.core.api.endpoint.AiConsentApi
import com.fernandocorreia.khepri.core.api.model.SetAIConsentRequestDto
import com.fernandocorreia.khepri.core.common.ApplicationScope
import com.fernandocorreia.khepri.core.common.ConsentSignal
import com.fernandocorreia.khepri.core.common.SignOutCleaner
import com.fernandocorreia.khepri.core.common.SignOutReason
import com.fernandocorreia.khepri.core.model.Outcome
import com.fernandocorreia.khepri.core.network.apiCall
import com.fernandocorreia.khepri.feature.auth.data.remote.decisionFor
import com.fernandocorreia.khepri.feature.auth.data.remote.toAiConsent
import com.fernandocorreia.khepri.feature.auth.domain.model.AiConsent
import com.fernandocorreia.khepri.feature.auth.domain.model.ConsentChoice
import com.fernandocorreia.khepri.feature.auth.domain.repository.AiConsentRepository
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * iOS `AIConsentCenter`, minus the screen state. A consent refusal from any call reloads the answer from the server, so
 * the question comes back only if the account really has not answered. Sign-out forgets the answer; an answer that
 * arrives after a sign-out (or after a newer reset) is dropped, so the next account never sees this one's.
 */
@Singleton
internal class AiConsentRepositoryImpl
@Inject
constructor(private val api: AiConsentApi, signal: ConsentSignal, @ApplicationScope scope: CoroutineScope) :
    AiConsentRepository, SignOutCleaner {
    private val state = MutableStateFlow(AiConsent.Unknown)
    override val consent: StateFlow<AiConsent> = state.asStateFlow()

    private val loading = Mutex()

    /** Bumped by [reset]; a response is applied only if no reset happened while it was in flight. */
    @Volatile private var generation = 0

    init {
        scope.launch {
            signal.required.collect {
                reset()
                load()
            }
        }
    }

    override suspend fun load(): Outcome<Unit> = loading.withLock {
        if (state.value.choice != ConsentChoice.Unknown) return@withLock Outcome.Ok(Unit)
        applying { apiCall(map = { it.toAiConsent() }) { api.getAIConsent() } }
    }

    override suspend fun decide(allow: Boolean): Outcome<Unit> = applying {
        apiCall(map = { it.toAiConsent() }) { api.setAIConsent(SetAIConsentRequestDto(decisionFor(allow))) }
    }

    override fun reset() {
        generation++
        state.value = AiConsent.Unknown
    }

    override suspend fun afterLogout(reason: SignOutReason) = reset()

    private suspend fun applying(call: suspend () -> Outcome<AiConsent>): Outcome<Unit> {
        val started = generation
        return when (val result = call()) {
            is Outcome.Ok -> {
                if (generation == started) state.value = result.value
                Outcome.Ok(Unit)
            }
            is Outcome.Err -> result
        }
    }
}
