package com.fernandocorreia.khepri.feature.auth.domain.usecase

import com.fernandocorreia.khepri.core.model.Outcome
import com.fernandocorreia.khepri.feature.auth.domain.repository.AuthRepository
import javax.inject.Inject

/** Asks the server to email a reset link; the reset itself finishes on the web. */
class RequestPasswordReset @Inject constructor(private val repository: AuthRepository) {
    suspend operator fun invoke(email: String): Outcome<Unit> = repository.requestPasswordReset(email)
}
