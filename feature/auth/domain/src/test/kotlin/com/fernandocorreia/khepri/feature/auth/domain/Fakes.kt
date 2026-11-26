package com.fernandocorreia.khepri.feature.auth.domain

import com.fernandocorreia.khepri.core.common.SessionGateway
import com.fernandocorreia.khepri.core.model.Outcome
import com.fernandocorreia.khepri.core.model.Session
import com.fernandocorreia.khepri.feature.auth.domain.model.SignUpForm
import com.fernandocorreia.khepri.feature.auth.domain.repository.AuthRepository
import java.time.Instant

val session = Session("t0ken", "u1", Instant.parse("2030-01-01T00:00:00Z"))

class FakeAuthRepository : AuthRepository {
    var result: Outcome<Session> = Outcome.Ok(session)
    val signUps = mutableListOf<SignUpForm>()

    override suspend fun signIn(email: String, password: String): Outcome<Session> = result

    override suspend fun signUp(form: SignUpForm): Outcome<Session> = result.also { signUps += form }

    override suspend fun requestPasswordReset(email: String): Outcome<Unit> = Outcome.Ok(Unit)
}

class FakeGateway : SessionGateway {
    val kept = mutableListOf<Session>()
    var result: Outcome<Unit> = Outcome.Ok(Unit)

    override suspend fun signedIn(session: Session): Outcome<Unit> = result.also { kept += session }
}
