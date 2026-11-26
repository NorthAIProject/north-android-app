package com.fernandocorreia.khepri.core.session

import com.fernandocorreia.khepri.core.api.endpoint.AuthApi
import com.fernandocorreia.khepri.core.api.endpoint.MeApi
import com.fernandocorreia.khepri.core.model.Outcome
import com.fernandocorreia.khepri.core.network.apiCall
import com.fernandocorreia.khepri.core.network.apiCallUnit
import com.fernandocorreia.khepri.core.network.di.ApiServices
import javax.inject.Inject

/** Narrow port over the generated client, so [SessionManager] tests need no HTTP. */
public interface SessionRemote {
    /** GET /me. True when onboarding is complete. */
    public suspend fun onboardingComplete(): Outcome<Boolean>

    public suspend fun logout(): Outcome<Unit>
}

internal class SessionRemoteImpl @Inject constructor(services: ApiServices) : SessionRemote {
    private val me = services.create<MeApi>()
    private val auth = services.create<AuthApi>()

    override suspend fun onboardingComplete(): Outcome<Boolean> =
        apiCall(map = { !it.user.needsOnboarding }) { me.getMe() }

    override suspend fun logout(): Outcome<Unit> = apiCallUnit { auth.logOut() }
}
