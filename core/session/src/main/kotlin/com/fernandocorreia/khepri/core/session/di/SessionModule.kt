package com.fernandocorreia.khepri.core.session.di

import com.fernandocorreia.khepri.core.common.AccountScope
import com.fernandocorreia.khepri.core.common.SessionGateway
import com.fernandocorreia.khepri.core.common.SignOutCleaner
import com.fernandocorreia.khepri.core.common.SignedInState
import com.fernandocorreia.khepri.core.network.TokenSource
import com.fernandocorreia.khepri.core.session.AccountWork
import com.fernandocorreia.khepri.core.session.SessionManager
import com.fernandocorreia.khepri.core.session.SessionRemote
import com.fernandocorreia.khepri.core.session.SessionRemoteImpl
import com.fernandocorreia.khepri.core.session.StoredTokenSource
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import dagger.multibindings.Multibinds
import kotlinx.coroutines.CoroutineScope

@Module
@InstallIn(SingletonComponent::class)
internal abstract class SessionModule {
    @Binds abstract fun tokenSource(source: StoredTokenSource): TokenSource

    @Binds abstract fun sessionGateway(manager: SessionManager): SessionGateway

    @Binds abstract fun signedInState(manager: SessionManager): SignedInState

    @Binds abstract fun sessionRemote(remote: SessionRemoteImpl): SessionRemote

    @Multibinds abstract fun signOutCleaners(): Set<SignOutCleaner>

    companion object {
        @Provides @AccountScope fun accountScope(work: AccountWork): CoroutineScope = work.scope
    }
}
