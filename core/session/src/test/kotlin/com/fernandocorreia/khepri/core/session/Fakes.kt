package com.fernandocorreia.khepri.core.session

import com.fernandocorreia.khepri.core.common.SignOutCleaner
import com.fernandocorreia.khepri.core.common.SignOutReason
import com.fernandocorreia.khepri.core.common.TokenUnreadableException
import com.fernandocorreia.khepri.core.datastore.SessionStore
import com.fernandocorreia.khepri.core.model.AppError
import com.fernandocorreia.khepri.core.model.Outcome
import com.fernandocorreia.khepri.core.model.Session
import java.time.Instant

internal val session = Session("t0ken", "u1", Instant.parse("2030-01-01T00:00:00Z"))

internal class Journal {
    val entries = mutableListOf<String>()

    operator fun plusAssign(entry: String) {
        entries += entry
    }
}

internal class FakeStore(private val journal: Journal, var stored: Session? = null) : SessionStore {
    var locked = false

    override suspend fun read(): Session? {
        if (locked) throw TokenUnreadableException()
        return stored
    }

    override suspend fun write(session: Session) {
        journal += "write"
        stored = session
    }

    override suspend fun clear() {
        journal += "clear"
        stored = null
    }
}

internal class FakeRemote(private val journal: Journal, private val store: FakeStore) : SessionRemote {
    var me: Outcome<Boolean> = Outcome.Ok(true)
    var logoutFails = false

    override suspend fun onboardingComplete(): Outcome<Boolean> {
        journal += "me"
        return me
    }

    override suspend fun logout(): Outcome<Unit> {
        journal += "logout(token=${store.stored?.token})"
        if (logoutFails) error("network down")
        return Outcome.Err(AppError.Network(interrupted = false))
    }
}

internal class RecordingCleaner(
    private val name: String,
    private val journal: Journal,
    private val phase: () -> AppPhase,
    private val failBefore: Boolean = false,
) : SignOutCleaner {
    override suspend fun beforeLogout() {
        journal += "before:$name"
        if (failBefore) error("push unregister failed")
    }

    override suspend fun afterLogout(reason: SignOutReason) {
        journal += "after:$name:$reason:${phase()::class.simpleName}"
    }
}
