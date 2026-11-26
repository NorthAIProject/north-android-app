package com.fernandocorreia.khepri

import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fernandocorreia.khepri.core.common.SessionGateway
import com.fernandocorreia.khepri.core.navigation.Navigator
import com.fernandocorreia.khepri.core.navigation.PendingLinks
import com.fernandocorreia.khepri.core.navigation.ui.EntryInstaller
import com.fernandocorreia.khepri.core.network.di.DefaultClient
import com.fernandocorreia.khepri.core.network.di.StreamingClient
import com.fernandocorreia.khepri.core.session.SessionManager
import com.fernandocorreia.khepri.feature.auth.domain.usecase.SignInWithEmail
import dagger.hilt.android.testing.HiltAndroidRule
import dagger.hilt.android.testing.HiltAndroidTest
import dagger.hilt.android.testing.HiltTestApplication
import javax.inject.Inject
import okhttp3.OkHttpClient
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config

/** The application graph builds and the shared pieces resolve: no cycle, no missing binding, one session. */
@HiltAndroidTest
@RunWith(AndroidJUnit4::class)
@Config(application = HiltTestApplication::class)
class HiltGraphTest {
    @get:Rule val hilt = HiltAndroidRule(this)

    @Inject lateinit var session: SessionManager

    @Inject lateinit var gateway: SessionGateway

    @Inject lateinit var navigator: Navigator

    @Inject lateinit var pendingLinks: PendingLinks

    @Inject lateinit var installers: Set<@JvmSuppressWildcards EntryInstaller>

    @Inject @DefaultClient lateinit var client: OkHttpClient

    @Inject @StreamingClient lateinit var streaming: OkHttpClient

    @Inject lateinit var signIn: SignInWithEmail

    @Before fun inject() = hilt.inject()

    @Test
    fun `the graph resolves, with one session behind the gateway and two clients`() {
        assertSame(session, gateway)
        assertNotSame(client, streaming)
    }
}
