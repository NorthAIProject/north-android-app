package com.fernandocorreia.khepri.feature.auth.domain.usecase

import com.fernandocorreia.khepri.core.common.SessionGateway
import com.fernandocorreia.khepri.core.model.Outcome
import com.fernandocorreia.khepri.core.model.Session
import com.fernandocorreia.khepri.feature.auth.domain.repository.AuthRepository
import javax.inject.Inject

/** Signs in and hands the session to the gateway, which keeps it and moves the app on. */
class SignInWithEmail @Inject constructor(private val repository: AuthRepository, private val gateway: SessionGateway) {
    suspend operator fun invoke(email: String, password: String): Outcome<Unit> =
        repository.signIn(email, password).keptBy(gateway)
}

internal suspend fun Outcome<Session>.keptBy(gateway: SessionGateway): Outcome<Unit> =
    when (this) {
        is Outcome.Ok -> gateway.signedIn(value)
        is Outcome.Err -> this
    }
