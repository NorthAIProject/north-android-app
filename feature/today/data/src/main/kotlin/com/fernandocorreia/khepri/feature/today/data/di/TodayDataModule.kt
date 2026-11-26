package com.fernandocorreia.khepri.feature.today.data.di

import com.fernandocorreia.khepri.core.api.endpoint.DayApi
import com.fernandocorreia.khepri.core.api.endpoint.FastingApi
import com.fernandocorreia.khepri.core.api.endpoint.TodayApi
import com.fernandocorreia.khepri.core.common.SignOutCleaner
import com.fernandocorreia.khepri.core.network.di.ApiServices
import com.fernandocorreia.khepri.feature.today.data.repository.TodayRepositoryImpl
import com.fernandocorreia.khepri.feature.today.domain.repository.TodayRepository
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.IntoSet

@Module
@InstallIn(SingletonComponent::class)
internal abstract class TodayDataModule {
    @Binds internal abstract fun repository(impl: TodayRepositoryImpl): TodayRepository

    @Binds @IntoSet internal abstract fun cleaner(impl: TodayRepositoryImpl): SignOutCleaner

    internal companion object {
        @Provides internal fun todayApi(services: ApiServices): TodayApi = services.create()

        @Provides internal fun dayApi(services: ApiServices): DayApi = services.create()

        @Provides internal fun fastingApi(services: ApiServices): FastingApi = services.create()
    }
}
