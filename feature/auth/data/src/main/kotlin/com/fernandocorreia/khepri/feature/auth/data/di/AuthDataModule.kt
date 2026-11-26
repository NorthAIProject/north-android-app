package com.fernandocorreia.khepri.feature.auth.data.di

import com.fernandocorreia.khepri.core.api.endpoint.AuthApi
import com.fernandocorreia.khepri.core.network.di.ApiServices
import com.fernandocorreia.khepri.feature.auth.data.repository.AuthRepositoryImpl
import com.fernandocorreia.khepri.feature.auth.domain.repository.AuthRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent

@Module
@InstallIn(SingletonComponent::class)
internal abstract class AuthDataModule {
    @Binds internal abstract fun repository(impl: AuthRepositoryImpl): AuthRepository

    internal companion object {
        @Provides internal fun api(services: ApiServices): AuthApi = services.create()
    }
}
