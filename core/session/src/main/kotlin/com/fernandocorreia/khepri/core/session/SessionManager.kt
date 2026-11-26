package com.fernandocorreia.khepri.core.session

import com.fernandocorreia.khepri.core.common.ApplicationScope
import com.fernandocorreia.khepri.core.common.SessionGateway
import com.fernandocorreia.khepri.core.common.SignOutCleaner
import com.fernandocorreia.khepri.core.common.SignOutReason
import com.fernandocorreia.khepri.core.common.SignedInState
import com.fernandocorreia.khepri.core.datastore.SessionStore
import com.fernandocorreia.khepri.core.model.AppError
import com.fernandocorreia.khepri.core.model.Outcome
import com.fernandocorreia.khepri.core.model.Session
import java.io.IOException
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.cancellation.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull

/**
 * Owns the app phase. Every transition runs under one lock in the application scope, so a 401 that arrives while the
 * account loads, or several 401s at once, sign out exactly once, and cancelling the caller (a screen that a sign-out
 * cleaner tears down) never leaves a transition half done. A session past its `expiresAt` is kept: a 401 is the only
 * expiry signal.
 */
@Singleton
public class SessionManager
@Inject
constructor(
    private val store: SessionStore,
    private val remote: SessionRemote,
    signal: UnauthorizedSignal,
    private val cleaners: Set<@JvmSuppressWildcards SignOutCleaner>,
    @ApplicationScope private val scope: CoroutineScope,
    private val accountWork: AccountWork,
) : SessionGateway, SignedInState {
    private val lock = Mutex()
    private val current = MutableStateFlow<AppPhase>(AppPhase.Launching)
    private val accountFlow = MutableStateFlow<String?>(null)

    /** The user of the stored session, once it is read or saved; null after sign-out. */
    private var userId: String? = null

    public val phase: StateFlow<AppPhase> = current.asStateFlow()
    override val account: StateFlow<String?> = accountFlow.asStateFlow()

    init {
        scope.launch(start = CoroutineStart.UNDISPATCHED) {
            signal.events.collect { signOut(SignOutReason.Unauthorized) }
        }
    }

    /** Reads the stored session and loads the account to choose the phase. */
    public suspend fun start(): Unit = transition { restore() }

    /** A sign-in screen finished: keep the session, then load the account. */
    override suspend fun signedIn(session: Session): Outcome<Unit> = transition {
        // Work a screen started after the last sign-out must not run under this account's token.
        accountWork.end()
        val saved =
            try {
                store.write(session)
                true
            } catch (e: CancellationException) {
                throw e
            } catch (@Suppress("TooGenericExceptionCaught") expected: Exception) {
                false
            }
        if (saved) {
            userId = session.userId
            loadAccount()
        }
        if (saved) Outcome.Ok(Unit) else Outcome.Err(AppError.Locked)
    }

    public suspend fun signOut(reason: SignOutReason): Unit = transition { endSession(reason) }

    /** From `Locked`, reads the store again; from `Failed`, reloads the account. Otherwise does nothing. */
    public suspend fun retry(): Unit = transition {
        when (current.value) {
            AppPhase.Locked -> restore()
            is AppPhase.Failed -> loadAccount()
            else -> Unit
        }
    }

    private suspend fun <T> transition(block: suspend () -> T): T = scope.async { lock.withLock { block() } }.await()

    private suspend fun restore() {
        val session =
            try {
                store.read()
            } catch (expected: IOException) {
                return moveTo(AppPhase.Locked)
            }
        if (session == null) return moveTo(AppPhase.SignedOut)
        userId = session.userId
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

    /**
     * The server steps of an explicit sign-out share one budget: on a dead network the user is signed out locally after
     * it, and the token stays valid on the server until it expires. iOS waits for the request instead.
     */
    private suspend fun endSession(reason: SignOutReason) {
        if (current.value == AppPhase.SignedOut) return
        accountWork.end()
        if (reason == SignOutReason.Explicit) {
            withTimeoutOrNull(SERVER_STEPS_MS) {
                cleaners.forEach { bestEffort { it.beforeLogout() } }
                bestEffort { remote.logout() }
            }
        }
        bestEffort { store.clear() }
        userId = null
        cleaners.forEach { bestEffort { it.afterLogout(reason) } }
        moveTo(AppPhase.SignedOut)
    }

    private fun moveTo(phase: AppPhase) {
        current.value = phase
        accountFlow.value = userId.takeIf { phase == AppPhase.SignedIn }
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

    private companion object {
        const val SERVER_STEPS_MS = 5_000L
    }
}
