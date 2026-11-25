package com.fernandocorreia.khepri.core.session

import com.fernandocorreia.khepri.core.common.ApplicationScope
import com.fernandocorreia.khepri.core.common.SessionGateway
import com.fernandocorreia.khepri.core.common.SignOutCleaner
import com.fernandocorreia.khepri.core.common.SignOutReason
import com.fernandocorreia.khepri.core.common.SignedInState
import com.fernandocorreia.khepri.core.common.TokenUnreadableException
import com.fernandocorreia.khepri.core.datastore.SessionStore
import com.fernandocorreia.khepri.core.model.AppError
import com.fernandocorreia.khepri.core.model.Outcome
import com.fernandocorreia.khepri.core.model.Session
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

/**
 * Owns the app phase. Every transition runs under one lock, so a 401 that arrives while the account loads, or several
 * 401s at once, sign out exactly once. A session past its `expiresAt` is kept: a 401 is the only expiry signal.
 */
@Singleton
public class SessionManager
@Inject
constructor(
    private val store: SessionStore,
    private val remote: SessionRemote,
    signal: UnauthorizedSignal,
    private val cleaners: Set<@JvmSuppressWildcards SignOutCleaner>,
    @ApplicationScope scope: CoroutineScope,
) : SessionGateway, SignedInState {
    private val lock = Mutex()
    private val current = MutableStateFlow<AppPhase>(AppPhase.Launching)
    private val signedInFlow = MutableStateFlow(false)

    public val phase: StateFlow<AppPhase> = current.asStateFlow()
    override val signedIn: StateFlow<Boolean> = signedInFlow.asStateFlow()

    init {
        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            signal.events.collect { signOut(SignOutReason.Unauthorized) }
        }
    }

    /** Reads the stored session and loads the account to choose the phase. */
    public suspend fun start(): Unit = lock.withLock { restore() }

    /** A sign-in screen finished: keep the session, then load the account. */
    override suspend fun signedIn(session: Session): Unit = lock.withLock {
        store.write(session)
        loadAccount()
    }

    public suspend fun signOut(reason: SignOutReason): Unit = lock.withLock { endSession(reason) }

    /** From `Locked`, reads the store again; from `Failed`, reloads the account. Otherwise does nothing. */
    public suspend fun retry(): Unit = lock.withLock {
        when (current.value) {
            AppPhase.Locked -> restore()
            is AppPhase.Failed -> loadAccount()
            else -> Unit
        }
    }

    private suspend fun restore() {
        val session =
            try {
                store.read()
            } catch (expected: TokenUnreadableException) {
                return moveTo(AppPhase.Locked)
            }
        if (session == null) return moveTo(AppPhase.SignedOut)
        moveTo(AppPhase.Launching)
        loadAccount()
    }

    private suspend fun loadAccount() {
        when (val result = remote.onboardingComplete()) {
            is Outcome.Ok -> moveTo(if (result.value) AppPhase.SignedIn else AppPhase.Onboarding)
            is Outcome.Err ->
                when (result.error) {
                    is AppError.Unauthorized -> endSession(SignOutReason.Unauthorized)
                    AppError.Locked -> moveTo(AppPhase.Locked)
                    else -> moveTo(AppPhase.Failed(result.error))
                }
        }
    }

    private suspend fun endSession(reason: SignOutReason) {
        if (current.value == AppPhase.SignedOut) return
        if (reason == SignOutReason.Explicit) {
            cleaners.forEach { bestEffort { it.beforeLogout() } }
            bestEffort { remote.logout() }
        }
        bestEffort { store.clear() }
        cleaners.forEach { bestEffort { it.afterLogout(reason) } }
        moveTo(AppPhase.SignedOut)
    }

    private fun moveTo(phase: AppPhase) {
        current.value = phase
        signedInFlow.value = phase == AppPhase.SignedIn
    }

    private suspend fun bestEffort(step: suspend () -> Unit) {
        try {
            step()
        } catch (e: CancellationException) {
            throw e
        } catch (@Suppress("TooGenericExceptionCaught") expected: Exception) {
            Unit
        }
    }
}
