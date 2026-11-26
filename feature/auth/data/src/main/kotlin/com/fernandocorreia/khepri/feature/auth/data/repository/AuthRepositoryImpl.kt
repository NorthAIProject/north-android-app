package com.fernandocorreia.khepri.feature.auth.data.repository

import com.fernandocorreia.khepri.core.api.endpoint.AuthApi
import com.fernandocorreia.khepri.core.api.model.ForgotPasswordRequestDto
import com.fernandocorreia.khepri.core.api.model.LoginRequestDto
import com.fernandocorreia.khepri.core.model.Outcome
import com.fernandocorreia.khepri.core.model.Session
import com.fernandocorreia.khepri.core.model.map
import com.fernandocorreia.khepri.core.network.apiCall
import com.fernandocorreia.khepri.core.network.apiCallUnit
import com.fernandocorreia.khepri.feature.auth.data.remote.toDto
import com.fernandocorreia.khepri.feature.auth.data.remote.toSession
import com.fernandocorreia.khepri.feature.auth.domain.model.SignUpForm
import com.fernandocorreia.khepri.feature.auth.domain.repository.AuthRepository
import javax.inject.Inject

internal class AuthRepositoryImpl @Inject constructor(private val api: AuthApi) : AuthRepository {
    override suspend fun signIn(email: String, password: String): Outcome<Session> = apiCall {
        api.logIn(LoginRequestDto(email = email, password = password))
    }
        .map { it.toSession() }

    override suspend fun signUp(form: SignUpForm): Outcome<Session> = apiCall {
        api.signUp(form.toDto())
    }
        .map { it.toSession() }

    override suspend fun requestPasswordReset(email: String): Outcome<Unit> = apiCallUnit {
        api.requestPasswordReset(ForgotPasswordRequestDto(email = email))
    }
}
