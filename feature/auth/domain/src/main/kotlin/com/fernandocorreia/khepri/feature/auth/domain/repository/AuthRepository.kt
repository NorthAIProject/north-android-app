package com.fernandocorreia.khepri.feature.auth.domain.repository

import com.fernandocorreia.khepri.core.model.Outcome
import com.fernandocorreia.khepri.core.model.Session
import com.fernandocorreia.khepri.feature.auth.domain.model.SignUpForm

/** Email and password accounts on the server. */
interface AuthRepository {
    suspend fun signIn(email: String, password: String): Outcome<Session>

    suspend fun signUp(form: SignUpForm): Outcome<Session>

    /** Always succeeds for a well-formed address, registered or not, so the answer reveals nothing. */
    suspend fun requestPasswordReset(email: String): Outcome<Unit>
}
