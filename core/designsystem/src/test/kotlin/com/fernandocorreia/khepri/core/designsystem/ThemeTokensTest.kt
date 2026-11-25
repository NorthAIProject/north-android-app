package com.fernandocorreia.khepri.core.designsystem

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.colorspace.ColorSpaces
import androidx.compose.ui.unit.dp
import com.fernandocorreia.khepri.core.designsystem.theme.KhepriColors
import com.fernandocorreia.khepri.core.designsystem.theme.Radii
import com.fernandocorreia.khepri.core.designsystem.theme.Spacing
import kotlin.math.roundToInt
import org.junit.jupiter.api.Assertions.assertEquals
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
}
