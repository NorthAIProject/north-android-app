package com.fernandocorreia.khepri.di

import com.fernandocorreia.khepri.BuildConfig
import com.fernandocorreia.khepri.core.common.ApiBaseUrl
import com.fernandocorreia.khepri.core.common.ApplicationScope
import com.fernandocorreia.khepri.core.common.ConsentSignal
import com.fernandocorreia.khepri.core.common.DataChangeBus
import com.fernandocorreia.khepri.core.common.DefaultDispatcherProvider
import com.fernandocorreia.khepri.core.common.DispatcherProvider
import com.fernandocorreia.khepri.core.common.SharedFlowConsentSignal
import com.fernandocorreia.khepri.core.common.SharedFlowDataChangeBus
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import java.time.Clock
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob

/** Hilt bindings for `:core:model` and `:core:common`, which are plain Kotlin and cannot hold Hilt modules. */
@Module
@InstallIn(SingletonComponent::class)
internal abstract class CommonBindings {
    @Binds @Singleton abstract fun dataChangeBus(bus: SharedFlowDataChangeBus): DataChangeBus

    @Binds @Singleton abstract fun consentSignal(signal: SharedFlowConsentSignal): ConsentSignal

    @Binds abstract fun dispatchers(provider: DefaultDispatcherProvider): DispatcherProvider

    companion object {
        @Provides fun clock(): Clock = Clock.systemDefaultZone()

        @Provides
        @Singleton
        @ApplicationScope
        fun applicationScope(): CoroutineScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

        @Provides @ApiBaseUrl fun apiBaseUrl(): String = BuildConfig.API_BASE_URL
    }
}
