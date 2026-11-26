package com.fernandocorreia.khepri

import android.content.ActivityNotFoundException
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.testTagsAsResourceId
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.fernandocorreia.khepri.core.designsystem.theme.KhepriTheme
import com.fernandocorreia.khepri.core.navigation.NavigationState
import com.fernandocorreia.khepri.core.navigation.Navigator
import com.fernandocorreia.khepri.core.navigation.ui.EntryInstaller
import com.fernandocorreia.khepri.core.ui.ProvideReduceMotion
import com.fernandocorreia.khepri.feature.auth.ui.navigation.SignedOutRoot
import com.fernandocorreia.khepri.shell.AppViewModel
import com.fernandocorreia.khepri.shell.MainTabs
import com.fernandocorreia.khepri.shell.ScopedViewModels
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var navigator: Navigator

    @Inject lateinit var installers: Set<@JvmSuppressWildcards EntryInstaller>

    override fun onCreate(savedInstanceState: Bundle?) {
        enableEdgeToEdge()
        super.onCreate(savedInstanceState)
        // A recreated activity gets its original intent again; the link was handled the first time.
        if (savedInstanceState == null) consume(intent)
        setContent {
            KhepriTheme {
                ProvideReduceMotion {
                    // Test tags as resource ids, so UI flows (Maestro) find `login-email` and the like.
                    Box(Modifier.semantics { testTagsAsResourceId = true }) { AppRoot(installers) }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        consume(intent)
    }

    /** Hands a link to the navigator; an own web link it does not know opens in the browser. Either way, once. */
    private fun consume(intent: Intent) {
        setIntent(LinkIntent.consume(intent, navigator::open, ::openInBrowser))
    }

    private fun openInBrowser(browser: Intent) {
        try {
            startActivity(browser)
        } catch (expected: ActivityNotFoundException) {
            // No browser on the phone: the link is dropped, as iOS drops a link it cannot open.
        }
    }
}

@Composable
internal fun AppRoot(
    installers: Set<EntryInstaller>,
    app: AppViewModel = hiltViewModel(),
    navigation: NavigationState = hiltViewModel(),
) {
    val phase by app.phase.collectAsStateWithLifecycle()
    RootContent(
        phase = phase,
        onRetry = app::retry,
        onSignOut = app::signOut,
        signedOut = { ScopedViewModels(app.signedOutStore) { SignedOutRoot() } },
        signedIn = { MainTabs(navigation, installers) },
    )
}
