package com.fernandocorreia.khepri.feature.auth.ui.consent

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.test.assertIsNotEnabled
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fernandocorreia.khepri.core.designsystem.theme.Appearance
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
class AiConsentScreenScreenshotTest {
    @get:Rule val compose = createComposeRule()

    private val asked = AiConsentUiState(recipients = listOf("Anthropic", "Google"))
    private val shots = ScreenshotHarness(compose)
    private var screen by mutableStateOf(asked)
    private val events = mutableListOf<AiConsentEvent>()

    private fun show() {
        shots.show { AiConsentContent(screen, onEvent = { events += it }) }
    }

    @Test
    fun `the question in both appearances, large text and Portuguese`() {
        show()
        shots.capture("ai_consent_light")
        shots.appearance = Appearance.Dark
        shots.capture("ai_consent_dark")
        shots.appearance = Appearance.Light
        shots.fontScale = 2f
        shots.capture("ai_consent_2x")
        shots.fontScale = 1f
        shots.language = "pt-PT"
        shots.capture("ai_consent_pt_pt")
    }

    @Test
    fun `saving disables both answers, and a failed save shows its message`() {
        show()
        screen = asked.copy(saving = true)
        compose.onNodeWithTag("ai-consent-allow").assertIsNotEnabled()
        compose.onNodeWithTag("ai-consent-decline").assertIsNotEnabled()
        shots.capture("ai_consent_saving")
        screen = asked.copy(error = AppError.Server(500, "Could not save your choice", null))
        shots.capture("ai_consent_failed")
    }

    @Test
    fun `the buttons send their answer`() {
        show()
        compose.onNodeWithTag("ai-consent-allow").performClick()
        compose.onNodeWithTag("ai-consent-decline").performClick()
        assertEquals(listOf(AiConsentEvent.Allow, AiConsentEvent.Decline), events)
    }
}
