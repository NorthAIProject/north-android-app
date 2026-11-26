package com.fernandocorreia.khepri.core.designsystem.theme

import androidx.compose.material3.Typography
import androidx.compose.runtime.Immutable
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.fernandocorreia.khepri.core.designsystem.R

private val weights =
    listOf(FontWeight.Light, FontWeight.Normal, FontWeight.Medium, FontWeight.SemiBold, FontWeight.Bold)

private fun variable(resource: Int): FontFamily =
    FontFamily(
        weights.map { weight ->
            Font(resource, weight, variationSettings = FontVariation.Settings(FontVariation.weight(weight.weight)))
        }
    )

/** Geist: every text style. */
public val GeistFamily: FontFamily = variable(R.font.geist)

/** Geist Mono: timers and code-like figures. Not for large numbers: its slashed zero reads as "Ø". */
public val GeistMonoFamily: FontFamily = variable(R.font.geist_mono)

/** Marcellus: brand moments only (a welcome, a celebration, a recap headline). It has one weight; never bold it. */
public val MarcellusFamily: FontFamily = FontFamily(Font(R.font.marcellus))

/** Styles Material 3 has no slot for. */
@Immutable
public data class KhepriTextStyles(
    val displayBrand: TextStyle = TextStyle(fontFamily = MarcellusFamily, fontSize = 34.sp, lineHeight = 41.sp),
    val mono: TextStyle = TextStyle(fontFamily = GeistMonoFamily, fontSize = 17.sp, lineHeight = 22.sp),
    /** The small uppercase label above a value, a card or a group. Uppercase it with the current locale. */
    val eyebrow: TextStyle =
        TextStyle(
            fontFamily = GeistFamily,
            fontWeight = FontWeight.Medium,
            fontSize = 12.sp,
            lineHeight = 16.sp,
            letterSpacing = 1.5.sp,
        ),
)

/**
 * Material's type scale in Geist, with no letter spacing: the baseline tracking is tuned for Roboto, and iOS sets none.
 */
internal fun geistTypography(): Typography {
    val base = Typography()
    fun TextStyle.geist() = copy(fontFamily = GeistFamily, letterSpacing = 0.sp)
    return Typography(
        displayLarge = base.displayLarge.geist(),
        displayMedium = base.displayMedium.geist(),
        displaySmall = base.displaySmall.geist(),
        headlineLarge = base.headlineLarge.geist(),
        headlineMedium = base.headlineMedium.geist(),
        headlineSmall = base.headlineSmall.geist(),
        titleLarge = base.titleLarge.geist(),
        titleMedium = base.titleMedium.geist(),
        titleSmall = base.titleSmall.geist(),
        bodyLarge = base.bodyLarge.geist(),
        bodyMedium = base.bodyMedium.geist(),
        bodySmall = base.bodySmall.geist(),
        labelLarge = base.labelLarge.geist(),
        labelMedium = base.labelMedium.geist(),
        labelSmall = base.labelSmall.geist(),
    )
}
