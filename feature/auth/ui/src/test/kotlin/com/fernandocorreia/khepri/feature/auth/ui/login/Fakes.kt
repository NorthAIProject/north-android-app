package com.fernandocorreia.khepri.feature.auth.ui.login

import com.fernandocorreia.khepri.core.common.SessionGateway
import com.fernandocorreia.khepri.core.model.Outcome
import com.fernandocorreia.khepri.core.model.Session
import com.fernandocorreia.khepri.feature.auth.domain.model.SignUpForm
import com.fernandocorreia.khepri.feature.auth.domain.repository.AuthRepository
import java.time.Instant
import kotlinx.coroutines.CompletableDeferred

class FakeAuthRepository : AuthRepository {
    var result: Outcome<Session> = Outcome.Ok(Session("t0ken", "u1", Instant.parse("2030-01-01T00:00:00Z")))
    var resetResult: Outcome<Unit> = Outcome.Ok(Unit)

    /** When set, calls wait for it: a slow network. */
    var gate: CompletableDeferred<Unit>? = null
    val signIns = mutableListOf<Pair<String, String>>()
    val signUps = mutableListOf<SignUpForm>()
    val resets = mutableListOf<String>()

    override suspend fun signIn(email: String, password: String): Outcome<Session> {
        signIns += email to password
        gate?.await()
        return result
    }

    override suspend fun signUp(form: SignUpForm): Outcome<Session> {
        signUps += form
        gate?.await()
        return result
    }

    override suspend fun requestPasswordReset(email: String): Outcome<Unit> {
        resets += email
        gate?.await()
        return resetResult
    }
}

class FakeGateway : SessionGateway {
    val kept = mutableListOf<Session>()

    override suspend fun signedIn(session: Session): Outcome<Unit> = Outcome.Ok(Unit).also { kept += session }
}
