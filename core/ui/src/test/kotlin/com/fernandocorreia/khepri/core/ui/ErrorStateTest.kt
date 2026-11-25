package com.fernandocorreia.khepri.core.ui

import androidx.compose.material3.Surface
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fernandocorreia.khepri.core.designsystem.theme.Appearance
import com.fernandocorreia.khepri.core.designsystem.theme.KhepriTheme
import com.fernandocorreia.khepri.core.model.AppError
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode

@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ErrorStateTest {
    @get:Rule val compose = createComposeRule()

    @Test
    fun `offline shows the offline copy and Try Again retries`() {
        var retries = 0
        compose.setContent {
            KhepriTheme(Appearance.Light) { Surface { ErrorState(AppError.Network(false), onRetry = { retries++ }) } }
        }
        compose.onNodeWithText("Offline").assertExists()
        compose.onRoot().captureRoboImage("src/test/screenshots/error_state_offline.png")
        compose.onNodeWithText("Try Again").performClick()
        assertEquals(1, retries)
    }

    @Test
    fun `a server error shows the server's message`() {
        compose.setContent {
            KhepriTheme(Appearance.Dark) {
                Surface { ErrorState(AppError.Server(500, "Import failed halfway", null), onRetry = {}) }
            }
        }
        compose.onNodeWithText("Couldn't load this").assertExists()
        compose.onNodeWithText("Import failed halfway").assertExists()
        compose.onRoot().captureRoboImage("src/test/screenshots/error_state_server.png")
    }
}
