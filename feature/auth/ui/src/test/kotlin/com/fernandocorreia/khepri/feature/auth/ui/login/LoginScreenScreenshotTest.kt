package com.fernandocorreia.khepri.feature.auth.ui.login

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fernandocorreia.khepri.core.designsystem.theme.Appearance
import com.fernandocorreia.khepri.core.designsystem.theme.KhepriTheme
import com.fernandocorreia.khepri.core.model.AppError
import com.fernandocorreia.khepri.core.screenshot.ScreenshotHarness
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

    private val shots = ScreenshotHarness(compose)
    private var screen by mutableStateOf(signIn)

    private fun show(content: @Composable (LoginUiState) -> Unit = { LoginContent(it, onEvent = {}) }) {
        shots.show { content(screen) }
    }

    @Test
    fun `sign-in and sign-up in both modes, both font scales and two languages`() {
        show()
        for (form in listOf("sign_in" to signIn, "sign_up" to signUp)) {
            for (mode in Appearance.entries.filter { it != Appearance.System }) {
                for (scale in listOf(1f, 2f)) {
                    for (tag in listOf("en", "pt-PT")) {
                        screen = form.second
                        shots.appearance = mode
                        shots.fontScale = scale
                        shots.language = tag
                        shots.capture("login_${form.first}_${mode.name.lowercase()}_${scale.toInt()}x_$tag")
                    }
                }
            }
        }
    }

    @Test
    fun `running, failed and reset sent`() {
        show()
        screen = signIn.copy(running = true)
        shots.capture("login_running")
        screen = signIn.copy(banner = LoginBanner.Failed(AppError.Unauthorized("Invalid email or password")))
        shots.capture("login_failed")
        shots.appearance = Appearance.Dark
        screen = signIn.copy(banner = LoginBanner.ResetSent)
        shots.capture("login_reset_sent_dark")
    }

    @Test
    fun `the reset form, empty and filled`() {
        screen = signIn
        show {
            Box(Modifier.background(KhepriTheme.colors.background)) {
                ForgotPasswordForm(ForgotPasswordState(it.email), onEvent = {})
            }
        }
        shots.capture("forgot_password_light")
        shots.appearance = Appearance.Dark
        shots.capture("forgot_password_dark")
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
