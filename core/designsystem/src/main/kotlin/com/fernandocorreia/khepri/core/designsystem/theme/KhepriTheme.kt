package com.fernandocorreia.khepri.core.designsystem.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.ColorScheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.runtime.staticCompositionLocalOf

private val LocalColors = staticCompositionLocalOf { KhepriColors.Light }
private val LocalSpacing = staticCompositionLocalOf { Spacing() }
private val LocalRadii = staticCompositionLocalOf { Radii() }
private val LocalTextStyles = staticCompositionLocalOf { KhepriTextStyles() }
private val typography = geistTypography()
private val lightScheme = KhepriColors.Light.scheme(dark = false)
private val darkScheme = KhepriColors.Dark.scheme(dark = true)

/**
 * The app theme: the web tokens as a Material 3 scheme, Geist everywhere, no dynamic colour. Controls tint with
 * `signal`, as iOS does with its accent colour.
 */
@Composable
public fun KhepriTheme(appearance: Appearance = Appearance.System, content: @Composable () -> Unit) {
    val dark =
        when (appearance) {
            Appearance.System -> isSystemInDarkTheme()
            Appearance.Light -> false
            Appearance.Dark -> true
        }
    val colors = if (dark) KhepriColors.Dark else KhepriColors.Light
    val radii = Radii()
    CompositionLocalProvider(LocalColors provides colors, LocalRadii provides radii) {
        MaterialTheme(
            colorScheme = if (dark) darkScheme else lightScheme,
            typography = typography,
            shapes = radii.materialShapes(),
        ) {
            content()
        }
    }
}

/** Tokens Material 3 has no slot for. */
public object KhepriTheme {
    public val colors: KhepriColors
        @Composable @ReadOnlyComposable get() = LocalColors.current

    public val spacing: Spacing
        @Composable @ReadOnlyComposable get() = LocalSpacing.current

    public val radii: Radii
        @Composable @ReadOnlyComposable get() = LocalRadii.current

    public val textStyles: KhepriTextStyles
        @Composable @ReadOnlyComposable get() = LocalTextStyles.current
}

/**
 * Every slot is mapped, so no default Material component (Snackbar, tonal Surface, pickers) falls back to the baseline
 * purple. Field and button edges use `input`, the web's 3:1 control border; `border` and `hairline` are for dividers
 * and cards only.
 */
internal fun KhepriColors.scheme(dark: Boolean): ColorScheme {
    val base = if (dark) darkColorScheme() else lightColorScheme()
    val other = if (dark) KhepriColors.Light else KhepriColors.Dark
    return base.copy(
        primary = signal,
        onPrimary = primaryForeground,
        primaryContainer = secondary,
        onPrimaryContainer = secondaryForeground,
        inversePrimary = other.signal,
        secondary = primary,
        onSecondary = primaryForeground,
        secondaryContainer = secondary,
        onSecondaryContainer = secondaryForeground,
        tertiary = agent,
        onTertiary = primaryForeground,
        tertiaryContainer = secondary,
        onTertiaryContainer = secondaryForeground,
        background = background,
        onBackground = foreground,
        surface = background,
        onSurface = foreground,
        surfaceVariant = muted,
        onSurfaceVariant = mutedForeground,
        surfaceTint = signal,
        inverseSurface = foreground,
        inverseOnSurface = background,
        surfaceDim = background,
        surfaceBright = card,
        surfaceContainerLowest = background,
        surfaceContainerLow = card,
        surfaceContainer = card,
        surfaceContainerHigh = card,
        surfaceContainerHighest = muted,
        error = destructive,
        onError = primaryForeground,
        errorContainer = muted,
        onErrorContainer = destructive,
        outline = input,
        outlineVariant = border,
        scrim = foreground,
    )
}
