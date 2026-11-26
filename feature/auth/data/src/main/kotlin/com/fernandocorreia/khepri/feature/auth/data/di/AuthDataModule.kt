package com.fernandocorreia.khepri.feature.auth.data.di

import com.fernandocorreia.khepri.core.api.endpoint.AiConsentApi
import com.fernandocorreia.khepri.core.api.endpoint.AuthApi
import com.fernandocorreia.khepri.core.common.SignOutCleaner
import com.fernandocorreia.khepri.core.network.di.ApiServices
import com.fernandocorreia.khepri.feature.auth.data.repository.AiConsentRepositoryImpl
import com.fernandocorreia.khepri.feature.auth.data.repository.AuthRepositoryImpl
import com.fernandocorreia.khepri.feature.auth.domain.repository.AiConsentRepository
import com.fernandocorreia.khepri.feature.auth.domain.repository.AuthRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

@Module
@InstallIn(SingletonComponent::class)
internal abstract class AuthDataModule {
    @Binds internal abstract fun repository(impl: AuthRepositoryImpl): AuthRepository

    @Binds internal abstract fun consent(impl: AiConsentRepositoryImpl): AiConsentRepository

    @Binds @IntoSet internal abstract fun consentCleaner(impl: AiConsentRepositoryImpl): SignOutCleaner

    internal companion object {
        @Provides internal fun api(services: ApiServices): AuthApi = services.create()

        @Provides internal fun consentApi(services: ApiServices): AiConsentApi = services.create()
    }
}
