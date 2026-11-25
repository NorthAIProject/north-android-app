package com.fernandocorreia.khepri.core.designsystem

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.fernandocorreia.khepri.core.designsystem.component.Eyebrow
import com.fernandocorreia.khepri.core.designsystem.component.KhepriTextField
import com.fernandocorreia.khepri.core.designsystem.component.ProminentButton
import com.fernandocorreia.khepri.core.designsystem.component.SurfaceCard
import com.fernandocorreia.khepri.core.designsystem.component.TrailingAction
import com.fernandocorreia.khepri.core.designsystem.component.clearIcon
import com.fernandocorreia.khepri.core.designsystem.theme.Appearance
import com.fernandocorreia.khepri.core.designsystem.theme.KhepriTheme
import com.github.takahirom.roborazzi.captureRoboImage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.annotation.GraphicsMode

/** Each component in light and dark at font scale 1 and 2. Record with `recordRoborazziDebug`. */
@RunWith(AndroidJUnit4::class)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
class ComponentScreenshotTest {
    @get:Rule val compose = createComposeRule()

    private val variants =
        listOf(Appearance.Light to 1f, Appearance.Dark to 1f, Appearance.Light to 2f, Appearance.Dark to 2f)

    private fun capture(name: String, content: @Composable () -> Unit) {
        var variant by mutableStateOf(variants.first())
        compose.setContent {
            val (appearance, scale) = variant
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, scale)) {
                KhepriTheme(appearance) {
                    Box(Modifier.width(360.dp).background(KhepriTheme.colors.background).padding(16.dp)) {
                        content()
                    }
                }
            }
        }
        variants.forEach { next ->
            variant = next
            compose.waitForIdle()
            val (appearance, scale) = next
            compose
                .onRoot()
                .captureRoboImage("src/test/screenshots/${name}_${appearance.name.lowercase()}_x${scale.toInt()}.png")
        }
    }

    @Test
    fun surfaceCard() =
        capture("surface_card") {
            SurfaceCard {
                Eyebrow("Weight")
                Text("82.4 kg")
            }
        }

    @Test fun prominentButton() = capture("prominent_button") { ProminentButton("Log workout", onClick = {}) }

    @Test
    fun prominentButtonDisabled() =
        capture("prominent_button_disabled") { ProminentButton("Log workout", onClick = {}, enabled = false) }

    @Test
    fun textField() =
        capture("text_field") {
            KhepriTextField(
                value = "ana@example.com",
                onValueChange = {},
                label = "Email",
                trailingAction = TrailingAction(clearIcon(), "Clear", onClick = {}),
            )
        }

    @Test
    fun textFieldError() =
        capture("text_field_error") {
            KhepriTextField(value = "ana@", onValueChange = {}, label = "Email", error = "Enter a valid email.")
        }

    @Test fun eyebrow() = capture("eyebrow") { Eyebrow("Recent") }
}
