package com.fernandocorreia.khepri

import androidx.activity.ComponentActivity
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Text
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.test.click
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performTouchInput
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fernandocorreia.khepri.core.designsystem.theme.Appearance
import com.fernandocorreia.khepri.core.designsystem.theme.KhepriTheme
import com.fernandocorreia.khepri.core.model.AppError
import com.fernandocorreia.khepri.core.session.AppPhase
import com.fernandocorreia.khepri.core.ui.LocalReduceMotion
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.Config
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
@Config(qualifiers = "w360dp-h800dp-xhdpi")
class RootContentTest {
    @get:Rule val compose = createAndroidComposeRule<ComponentActivity>()

    private val phase = mutableStateOf<AppPhase>(AppPhase.Launching)
    private val appearance = mutableStateOf(Appearance.Light)
    private val asksConsent = mutableStateOf(false)
    private var retries = 0
    private var signOuts = 0
    private var appTaps = 0
    private var appBacks = 0

    private fun show() {
        compose.setContent {
            CompositionLocalProvider(LocalReduceMotion provides true) {
                KhepriTheme(appearance.value) {
                    RootContent(
                        phase = phase.value,
                        onRetry = { retries++ },
                        onSignOut = { signOuts++ },
                        signedOut = { Text("login") },
                        signedIn = {
                            BackHandler { appBacks++ }
                            Text("tabs", Modifier.fillMaxSize().clickable { appTaps++ })
                        },
                        asksConsent = asksConsent.value,
                        consent = { Text("consent") },
                    )
                }
            }
        }
        compose.waitForIdle()
    }

    private fun capture(name: String) {
        compose.waitForIdle()
        compose.onRoot().captureRoboImage("src/test/screenshots/$name.png")
    }

    @Test
    fun `each phase shows its screen`() {
        show()
        compose.onNodeWithTag("phase-launching").assertExists()
        phase.value = AppPhase.SignedOut
        compose.onNodeWithText("login").assertExists()
        phase.value = AppPhase.SignedIn
        compose.onNodeWithText("tabs").assertExists()
        phase.value = AppPhase.Onboarding
        compose.onNodeWithTag("phase-onboarding").assertExists()
    }

    @Test
    fun `failed offers Try Again and Sign Out`() {
        show()
        phase.value = AppPhase.Failed(AppError.Network(interrupted = false))
        capture("phase_failed_light")
        appearance.value = Appearance.Dark
        capture("phase_failed_dark")
        // After the captures: a pressed button's ripple differs from run to run.
        compose.onNodeWithText("Try Again").performClick()
        compose.onNodeWithText("Sign Out").performClick()
        assertEquals(1 to 1, retries to signOuts)
    }

    @Test
    fun `each phase is saved under its own fixed key, the same in every process`() {
        val phases =
            listOf(
                AppPhase.Launching,
                AppPhase.Locked,
                AppPhase.SignedOut,
                AppPhase.Onboarding,
                AppPhase.SignedIn,
                AppPhase.Failed(AppError.Network(interrupted = false)),
            )
        val keys = phases.map { it.stateKey() }
        assertEquals(phases.size, keys.toSet().size)
        assertEquals("signed-in", AppPhase.SignedIn.stateKey())
        assertEquals("failed", AppPhase.Failed(AppError.InvalidResponse).stateKey())
    }

    @Test
    fun `locked never shows the login form`() {
        show()
        phase.value = AppPhase.Locked
        compose.onNodeWithTag("phase-locked").assertExists()
        compose.onNodeWithText("login").assertDoesNotExist()
        capture("phase_locked_light")
    }

    @Test
    fun `the consent question covers the signed-in app and onboarding, and nothing else`() {
        asksConsent.value = true
        show()
        for (covered in listOf(AppPhase.SignedIn, AppPhase.Onboarding)) {
            phase.value = covered
            compose.waitForIdle()
            compose.onNodeWithText("consent").assertExists()
        }
        for (open in
            listOf(
                AppPhase.SignedOut,
                AppPhase.Locked,
                AppPhase.Launching,
                AppPhase.Failed(AppError.Network(interrupted = false)),
            )) {
            phase.value = open
            compose.waitForIdle()
            compose.onNodeWithText("consent").assertDoesNotExist()
        }
    }

    @Test
    fun `an answered account sees the app`() {
        phase.value = AppPhase.SignedIn
        show()
        compose.onNodeWithText("tabs").assertExists()
        compose.onNodeWithText("consent").assertDoesNotExist()
    }

    private fun pressBack() {
        compose.runOnUiThread { compose.activity.onBackPressedDispatcher.onBackPressed() }
        compose.waitForIdle()
    }

    @Test
    fun `while asked, touches and Back never reach the app, and TalkBack cannot see it`() {
        phase.value = AppPhase.SignedIn
        asksConsent.value = true
        show()
        compose.onRoot().performTouchInput { click(center) }
        pressBack()
        compose.onNodeWithText("tabs").assertDoesNotExist()
        assertEquals(0 to 0, appTaps to appBacks)

        asksConsent.value = false
        compose.waitForIdle()
        compose.onRoot().performTouchInput { click(center) }
        pressBack()
        assertEquals(1 to 1, appTaps to appBacks)
    }
}
