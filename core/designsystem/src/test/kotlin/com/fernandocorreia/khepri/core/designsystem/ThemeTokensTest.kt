package com.fernandocorreia.khepri.core.designsystem

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.colorspace.ColorSpaces
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.graphics.luminance
import androidx.compose.ui.unit.dp
import com.fernandocorreia.khepri.core.designsystem.theme.KhepriColors
import com.fernandocorreia.khepri.core.designsystem.theme.Radii
import com.fernandocorreia.khepri.core.designsystem.theme.Spacing
import com.fernandocorreia.khepri.core.designsystem.theme.scheme
import kotlin.math.roundToInt
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

/** The same Display P3 components as the iOS colour sets, written as hex. */
class ThemeTokensTest {
    private fun Color.p3Hex(): String {
        assertEquals(ColorSpaces.DisplayP3, colorSpace)
        return "#%02X%02X%02X".format((red * 255).roundToInt(), (green * 255).roundToInt(), (blue * 255).roundToInt())
    }

    @Test
    fun `colours match the iOS colour sets`() {
        assertEquals("#F5F2EB", KhepriColors.Light.background.p3Hex())
        assertEquals("#0C111B", KhepriColors.Dark.background.p3Hex())
        assertEquals("#846328", KhepriColors.Light.signal.p3Hex())
        assertEquals("#E0B86A", KhepriColors.Dark.signal.p3Hex())
        assertEquals("#9E413D", KhepriColors.Light.destructive.p3Hex())
        assertEquals("#D27E7C", KhepriColors.Dark.destructive.p3Hex())
    }

    @Test
    fun `radii match iOS NorthRadius`() {
        val radii = Radii()
        assertEquals(listOf(6.dp, 10.dp, 12.dp, 16.dp), listOf(radii.small, radii.medium, radii.control, radii.large))
    }

    @Test
    fun `spacing scale`() {
        val s = Spacing()
        assertEquals(listOf(4.dp, 8.dp, 12.dp, 16.dp, 20.dp, 24.dp), listOf(s.xs, s.sm, s.md, s.lg, s.xl, s.xxl))
    }

    private fun Color.contrastWith(other: Color, background: Color): Double {
        val l1 = compositeOver(background).convert(ColorSpaces.Srgb).luminance() + CONTRAST_OFFSET
        val l2 = other.compositeOver(background).convert(ColorSpaces.Srgb).luminance() + CONTRAST_OFFSET
        return maxOf(l1, l2).toDouble() / minOf(l1, l2)
    }

    @Test
    fun `the Material scheme meets WCAG AA in both modes`() {
        for (dark in listOf(false, true)) {
            val scheme = (if (dark) KhepriColors.Dark else KhepriColors.Light).scheme(dark)
            val bg = scheme.background
            val text =
                mapOf(
                    "onBackground" to (scheme.onBackground to scheme.background),
                    "onSurface" to (scheme.onSurface to scheme.surface),
                    "onSurfaceVariant" to (scheme.onSurfaceVariant to scheme.surfaceContainer),
                    "onPrimary" to (scheme.onPrimary to scheme.primary),
                    "onSecondary" to (scheme.onSecondary to scheme.secondary),
                    "onPrimaryContainer" to (scheme.onPrimaryContainer to scheme.primaryContainer),
                    "onTertiary" to (scheme.onTertiary to scheme.tertiary),
                    "onError" to (scheme.onError to scheme.error),
                    "onErrorContainer" to (scheme.onErrorContainer to scheme.errorContainer),
                    "inverseOnSurface" to (scheme.inverseOnSurface to scheme.inverseSurface),
                    "primary on surface" to (scheme.primary to scheme.surface),
                )
            text.forEach { (name, pair) ->
                val ratio = pair.first.contrastWith(pair.second, bg)
                assertTrue(ratio >= TEXT_CONTRAST, "$name in ${if (dark) "dark" else "light"} is $ratio")
            }
            val outline = scheme.outline.contrastWith(scheme.surface, bg)
            assertTrue(outline >= CONTROL_CONTRAST, "outline in ${if (dark) "dark" else "light"} is $outline")
        }
    }

    private companion object {
        const val CONTRAST_OFFSET = 0.05f
        const val TEXT_CONTRAST = 4.5
        const val CONTROL_CONTRAST = 3.0
    }
}
