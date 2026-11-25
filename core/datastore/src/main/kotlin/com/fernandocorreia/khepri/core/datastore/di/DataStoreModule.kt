package com.fernandocorreia.khepri.core.datastore.di

import android.content.Context
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.preferencesDataStoreFile
import com.fernandocorreia.khepri.core.common.ApplicationScope
import com.fernandocorreia.khepri.core.datastore.DataStoreSessionStore
import com.fernandocorreia.khepri.core.datastore.KeystoreTokenCipher
import com.fernandocorreia.khepri.core.datastore.SessionStore
import com.fernandocorreia.khepri.core.datastore.TokenCipher
import dagger.Binds
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.plus

@Module
@InstallIn(SingletonComponent::class)
internal abstract class DataStoreModule {
    @Binds abstract fun cipher(cipher: KeystoreTokenCipher): TokenCipher

    companion object {
        /** The session has its own file, so clearing app settings never touches it and the reverse. */
        @Provides
        @Singleton
        fun sessionStore(
            @ApplicationContext context: Context,
            @ApplicationScope scope: CoroutineScope,
            cipher: TokenCipher,
        ): SessionStore =
            DataStoreSessionStore(
                PreferenceDataStoreFactory.create(scope = scope + Dispatchers.IO) {
                    context.preferencesDataStoreFile("session")
                },
                cipher,
            )
    }
}
