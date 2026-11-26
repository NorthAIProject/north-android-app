package com.fernandocorreia.khepri.feature.auth.ui.login

import android.content.res.Configuration
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.unit.Density
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fernandocorreia.khepri.core.designsystem.theme.Appearance
import com.fernandocorreia.khepri.core.designsystem.theme.KhepriTheme
import com.fernandocorreia.khepri.core.model.AppError
import com.github.takahirom.roborazzi.captureRoboImage
import java.util.Locale
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h800dp-xhdpi")
class LoginScreenScreenshotTest {
    @get:Rule val compose = createComposeRule()

    private var appearance by mutableStateOf(Appearance.Light)
    private var fontScale by mutableFloatStateOf(1f)
    private var language by mutableStateOf("en")
    private var screen by mutableStateOf(signIn)

    /** Starts in Light, en, 1.0 and changes from there: a composition that started in Dark drew an empty window. */
    private fun show(content: @Composable (LoginUiState) -> Unit = { LoginContent(it, onEvent = {}) }) {
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                InLocale(language) { KhepriTheme(appearance) { content(screen) } }
            }
        }
        compose.waitForIdle()
    }

    @Composable
    private fun InLocale(tag: String, content: @Composable () -> Unit) {
        val base = LocalContext.current
        val configuration = Configuration(base.resources.configuration).apply { setLocale(Locale.forLanguageTag(tag)) }
        val localized = base.createConfigurationContext(configuration)
        CompositionLocalProvider(
            LocalContext provides localized,
            LocalConfiguration provides configuration,
            LocalResources provides localized.resources,
            content = content,
        )
    }

    private fun capture(name: String) {
        compose.waitForIdle()
        compose.onRoot().captureRoboImage("src/test/screenshots/$name.png")
    }

    @Test
    fun `sign-in and sign-up in both modes, both font scales and two languages`() {
        show()
        for (form in listOf("sign_in" to signIn, "sign_up" to signUp)) {
            for (mode in Appearance.entries.filter { it != Appearance.System }) {
                for (scale in listOf(1f, 2f)) {
                    for (tag in listOf("en", "pt-PT")) {
                        screen = form.second
                        appearance = mode
                        fontScale = scale
                        language = tag
                        capture("login_${form.first}_${mode.name.lowercase()}_${scale.toInt()}x_$tag")
                    }
                }
            }
        }
    }

    @Test
    fun `running, failed and reset sent`() {
        show()
        screen = signIn.copy(running = true)
        capture("login_running")
        screen = signIn.copy(banner = LoginBanner.Failed(AppError.Unauthorized("Invalid email or password")))
        capture("login_failed")
        appearance = Appearance.Dark
        screen = signIn.copy(banner = LoginBanner.ResetSent)
        capture("login_reset_sent_dark")
    }

    @Test
    fun `the reset form, empty and filled`() {
        screen = signIn
        show {
            Box(Modifier.background(KhepriTheme.colors.background)) {
                ForgotPasswordForm(ForgotPasswordState(it.email), onEvent = {})
            }
        }
        capture("forgot_password_light")
        appearance = Appearance.Dark
        capture("forgot_password_dark")
    }

    @Test
    fun `the submit button is disabled until the form is complete and the modes switch`() {
        val events = mutableListOf<LoginEvent>()
        screen = LoginUiState()
        show { LoginContent(it, onEvent = { event -> events += event }) }
        compose.onNodeWithTag("login-submit").assertIsNotEnabled()
        compose.onNodeWithTag("mode-sign-up").performClick()
        compose.onNodeWithText("Forgot password?").performClick()
        assertEquals(listOf(LoginEvent.ModeChanged(LoginMode.SignUp), LoginEvent.ForgotPasswordOpened), events)
    }

    private companion object {
        val signIn = LoginUiState(email = "ana@example.com", password = "secret")
        val signUp =
            LoginUiState(
                mode = LoginMode.SignUp,
                name = "Ana Silva",
                email = "ana@example.com",
                password = "Secret12",
                confirmation = "Secret1",
            )
    }
}
