package com.fernandocorreia.khepri.core.ui

import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.Density
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fernandocorreia.khepri.core.designsystem.theme.Appearance
import com.fernandocorreia.khepri.core.designsystem.theme.KhepriTheme
import com.fernandocorreia.khepri.core.model.AppError
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
class ErrorStateTest {
    @get:Rule val compose = createComposeRule()

    private fun show(appearance: Appearance, fontScale: Float = 1f, content: @Composable () -> Unit) {
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                KhepriTheme(appearance) { Surface { content() } }
            }
        }
    }

    @Test
    fun `offline shows the offline copy and Try Again retries`() {
        var retries = 0
        show(Appearance.Light) { ErrorState(AppError.Network(false), onRetry = { retries++ }) }
        compose.onNodeWithText("Offline").assertExists()
        compose.onRoot().captureRoboImage("src/test/screenshots/error_state_offline.png")
        compose.onNodeWithText("Try Again").performClick()
        assertEquals(1, retries)
    }

    @Test
    fun `a server error shows the server's message`() {
        show(Appearance.Dark) { ErrorState(AppError.Server(500, "Import failed halfway", null), onRetry = {}) }
        compose.onNodeWithText("Couldn't load this").assertExists()
        compose.onNodeWithText("Import failed halfway").assertExists()
        compose.onRoot().captureRoboImage("src/test/screenshots/error_state_server.png")
    }

    @Test
    fun `a blank server message falls back to the generic copy`() {
        show(Appearance.Light) { ErrorState(AppError.Server(500, " ", null), onRetry = {}) }
        compose.onNodeWithText("Something went wrong on our side. Try again in a moment.").assertExists()
    }

    @Test
    fun `at double font size a long message scrolls and Try Again is still reachable`() {
        val long = List(12) { "The import stopped before it finished." }.joinToString(" ")
        show(Appearance.Light, fontScale = 2f) { ErrorState(AppError.Server(500, long, null), onRetry = {}) }
        compose.onRoot().captureRoboImage("src/test/screenshots/error_state_server_x2.png")
        compose.onNodeWithText("Try Again").performScrollTo().assertIsDisplayed()
    }

    @Test
    @Config(qualifiers = "+pt-rPT")
    fun `offline in European Portuguese`() {
        show(Appearance.Dark) { ErrorState(AppError.Network(false), onRetry = {}) }
        compose.onRoot().captureRoboImage("src/test/screenshots/error_state_offline_pt_pt_dark.png")
    }
}
