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
 * iOS `AIConsentCenter`, minus the screen state. A consent refusal from any call re-reads the answer from the server
 * and keeps showing the current one until the server replies, so the question comes back only if the account really has
 * not answered, and a declined account never flickers. Requests run one at a time, so a re-read that started before a
 * save cannot overwrite it. Sign-out forgets the answer; a reply that arrives after a sign-out is dropped, so the next
 * account never sees this one's.
 */
@Singleton
internal class AiConsentRepositoryImpl
@Inject
constructor(private val api: AiConsentApi, signal: ConsentSignal, @ApplicationScope scope: CoroutineScope) :
    AiConsentRepository, SignOutCleaner {
    private val state = MutableStateFlow(AiConsent.Unknown)
    override val consent: StateFlow<AiConsent> = state.asStateFlow()

    private val requests = Mutex()

    /** Guards [generation] and the writes to [state], which come from several threads. Never held across a call. */
    private val lock = Any()

    /** Bumped at sign-out; a reply is applied only to the account it was asked for. */
    private var generation = 0

    init {
        scope.launch { signal.required.collect { refresh() } }
    }

    override suspend fun load(): Outcome<Unit> = requests.withLock {
        if (state.value.choice != ConsentChoice.Unknown) Outcome.Ok(Unit) else fetch()
    }

    override suspend fun decide(allow: Boolean): Outcome<Unit> = requests.withLock {
        applying {
            apiCall(map = { it.toAiConsent() }) { api.setAIConsent(SetAIConsentRequestDto(decisionFor(allow))) }
        }
    }

    override fun reset() {
        synchronized(lock) {
            generation++
            state.value = AiConsent.Unknown
        }
    }

    override suspend fun afterLogout(reason: SignOutReason) = reset()

    /** Re-reads the answer even when it is known; a failure keeps what is there. */
    private suspend fun refresh(): Outcome<Unit> = requests.withLock { fetch() }

    private suspend fun fetch(): Outcome<Unit> = applying { apiCall(map = { it.toAiConsent() }) { api.getAIConsent() } }

    private suspend fun applying(call: suspend () -> Outcome<AiConsent>): Outcome<Unit> {
        val started = synchronized(lock) { generation }
        return when (val result = call()) {
            is Outcome.Ok -> {
                synchronized(lock) { if (generation == started) state.value = result.value }
                Outcome.Ok(Unit)
            }
            is Outcome.Err -> result
        }
    }
}
