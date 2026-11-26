package com.fernandocorreia.khepri.core.screenshot

import android.annotation.SuppressLint
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
import androidx.compose.ui.test.junit4.ComposeContentTestRule
import androidx.compose.ui.test.onRoot
import androidx.compose.ui.unit.Density
import com.fernandocorreia.khepri.core.designsystem.theme.Appearance
import com.fernandocorreia.khepri.core.designsystem.theme.KhepriTheme
import com.github.takahirom.roborazzi.captureRoboImage
import java.util.Locale

/**
 * Draws one composition in [KhepriTheme] with the [appearance], [fontScale] and [language] it is given, and captures it
 * to `src/test/screenshots/<name>.png` in the module under test. It starts in Light, en and 1.0 and changes from there:
 * a composition that started in Dark drew an empty window, and a second `setContent` in one test drew blank frames. So
 * call [show] once per test, then change the settings or the state the content reads, then [capture].
 */
public class ScreenshotHarness(private val compose: ComposeContentTestRule) {
    public var appearance: Appearance by mutableStateOf(Appearance.Light)
    public var fontScale: Float by mutableFloatStateOf(1f)
    public var language: String by mutableStateOf("en")

    public fun show(content: @Composable () -> Unit) {
        compose.setContent {
            val density = LocalDensity.current
            CompositionLocalProvider(LocalDensity provides Density(density.density, fontScale)) {
                InLocale(language) { KhepriTheme(appearance) { content() } }
            }
        }
        compose.waitForIdle()
    }

    public fun capture(name: String) {
        compose.waitForIdle()
        compose.onRoot().captureRoboImage("src/test/screenshots/$name.png")
    }
}

// Screenshot tests only: no app bundle ever ships this locale switch.
@SuppressLint("AppBundleLocaleChanges")
@Composable
private fun InLocale(tag: String, content: @Composable () -> Unit) {
    val base = LocalContext.current
    val configuration = Configuration(LocalConfiguration.current).apply { setLocale(Locale.forLanguageTag(tag)) }
    val localized = base.createConfigurationContext(configuration)
    CompositionLocalProvider(
        LocalContext provides localized,
        LocalConfiguration provides configuration,
        LocalResources provides localized.resources,
        content = content,
    )
}
