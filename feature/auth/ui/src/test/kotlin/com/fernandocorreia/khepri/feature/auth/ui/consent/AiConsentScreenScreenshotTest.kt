package com.fernandocorreia.khepri.feature.auth.ui.consent

import android.content.res.Configuration
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalResources
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
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
class AiConsentScreenScreenshotTest {
    @get:Rule val compose = createComposeRule()

    private val asked = AiConsentUiState(recipients = listOf("Anthropic", "Google"))
    private var appearance by mutableStateOf(Appearance.Light)
    private var fontScale by mutableFloatStateOf(1f)
    private var language by mutableStateOf("en")
    private var screen by mutableStateOf(asked)
    private val events = mutableListOf<AiConsentEvent>()

    /** Starts in Light, en, 1.0 and changes from there: a composition that started in Dark drew an empty window. */
    private fun show() {
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                InLocale(language) { KhepriTheme(appearance) { AiConsentContent(screen, onEvent = { events += it }) } }
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
    fun `the question in both appearances, large text and Portuguese`() {
        show()
        capture("ai_consent_light")
        appearance = Appearance.Dark
        capture("ai_consent_dark")
        appearance = Appearance.Light
        fontScale = 2f
        capture("ai_consent_2x")
        fontScale = 1f
        language = "pt-PT"
        capture("ai_consent_pt_pt")
    }

    @Test
    fun `saving disables both answers, and a failed save shows its message`() {
        show()
        screen = asked.copy(saving = true)
        compose.onNodeWithTag("ai-consent-allow").assertIsNotEnabled()
        compose.onNodeWithTag("ai-consent-decline").assertIsNotEnabled()
        capture("ai_consent_saving")
        screen = asked.copy(error = AppError.Server(500, "Could not save your choice", null))
        capture("ai_consent_failed")
    }

    @Test
    fun `the buttons send their answer`() {
        show()
        compose.onNodeWithTag("ai-consent-allow").performClick()
        compose.onNodeWithTag("ai-consent-decline").performClick()
        assertEquals(listOf(AiConsentEvent.Allow, AiConsentEvent.Decline), events)
    }
}
