package com.fernandocorreia.khepri.core.designsystem.theme

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Shapes
import androidx.compose.runtime.Immutable
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/** Corner radii, matching iOS `NorthRadius`. */
@Immutable
public data class Radii(
    val small: Dp = 6.dp,
    val medium: Dp = 10.dp,
    /** Fields, full-width buttons and the inset groups inside a card. */
    val control: Dp = 12.dp,
    val large: Dp = 16.dp,
)

internal fun Radii.materialShapes(): Shapes =
    Shapes(
        extraSmall = RoundedCornerShape(small),
        small = RoundedCornerShape(medium),
        medium = RoundedCornerShape(control),
        large = RoundedCornerShape(large),
        extraLarge = RoundedCornerShape(large),
    )
