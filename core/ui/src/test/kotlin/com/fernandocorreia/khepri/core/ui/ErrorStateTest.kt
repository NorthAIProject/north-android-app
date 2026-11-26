package com.fernandocorreia.khepri.core.ui

import android.content.res.Configuration
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalResources
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
class ErrorStateTest {
    @get:Rule val compose = createComposeRule()

    /**
     * Draws one frame in Light, then switches to [appearance], as `ComponentScreenshotTest` does: a composition that
     * started in Dark sometimes drew an empty window under Robolectric.
     */
    private fun show(appearance: Appearance, fontScale: Float = 1f, content: @Composable () -> Unit) {
        val current = mutableStateOf(Appearance.Light)
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                KhepriTheme(current.value) { Surface { content() } }
            }
        }
        compose.waitForIdle()
        current.value = appearance
        compose.waitForIdle()
    }

    /**
     * Resources in [tag] for [content] only: a per-test `@Config` locale rebuilds the environment and drew blank
     * frames.
     */
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

    /** Waits for the frame to be drawn: without it Robolectric sometimes captures an empty window. */
    private fun capture(name: String) {
        compose.waitForIdle()
        compose.onRoot().captureRoboImage("src/test/screenshots/$name.png")
    }

    @Test
    fun `offline shows the offline copy and Try Again retries`() {
        var retries = 0
        show(Appearance.Light) { ErrorState(AppError.Network(false), onRetry = { retries++ }) }
        compose.onNodeWithText("Offline").assertExists()
        capture("error_state_offline")
        compose.onNodeWithText("Try Again").performClick()
        assertEquals(1, retries)
    }

    @Test
    fun `a server error shows the server's message`() {
        show(Appearance.Dark) { ErrorState(AppError.Server(500, "Import failed halfway", null), onRetry = {}) }
        compose.onNodeWithText("Couldn't load this").assertExists()
        compose.onNodeWithText("Import failed halfway").assertExists()
        capture("error_state_server")
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
        capture("error_state_server_x2")
        compose.onNodeWithText("Try Again").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `offline in European Portuguese`() {
        show(Appearance.Dark) { InLocale("pt-PT") { ErrorState(AppError.Network(false), onRetry = {}) } }
        compose.onNodeWithText("Sem ligação").assertExists()
        capture("error_state_offline_pt_pt_dark")
    }
}
