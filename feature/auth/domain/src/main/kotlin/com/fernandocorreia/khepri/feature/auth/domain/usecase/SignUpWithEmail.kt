package com.fernandocorreia.khepri.feature.auth.domain.usecase

import com.fernandocorreia.khepri.core.common.SessionGateway
import com.fernandocorreia.khepri.core.model.Outcome
import com.fernandocorreia.khepri.feature.auth.domain.model.SignUpForm
import com.fernandocorreia.khepri.feature.auth.domain.repository.AuthRepository
import javax.inject.Inject

/** Creates the account and hands the session to the gateway. */
class SignUpWithEmail @Inject constructor(private val repository: AuthRepository, private val gateway: SessionGateway) {
    suspend operator fun invoke(form: SignUpForm): Outcome<Unit> = repository.signUp(form).keptBy(gateway)
}
