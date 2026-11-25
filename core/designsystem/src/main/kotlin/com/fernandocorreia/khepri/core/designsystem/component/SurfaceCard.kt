package com.fernandocorreia.khepri.core.designsystem.component

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.fernandocorreia.khepri.core.designsystem.theme.KhepriTheme

private const val HAIRLINE_OPACITY = 0.6f

/** The large rounded card most screens are built from: no shadow, a hairline edge that holds it apart in dark mode. */
@Composable
public fun SurfaceCard(
    modifier: Modifier = Modifier,
    contentPadding: Dp = KhepriTheme.spacing.xl,
    content: @Composable ColumnScope.() -> Unit,
) {
    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(KhepriTheme.radii.large),
        color = KhepriTheme.colors.card,
        contentColor = KhepriTheme.colors.cardForeground,
        border = BorderStroke(0.5.dp, KhepriTheme.colors.hairline.copy(alpha = HAIRLINE_OPACITY)),
    ) {
        Column(Modifier.padding(contentPadding), content = content)
    }
}

@Preview
@Composable
private fun SurfaceCardPreview() {
    KhepriTheme { SurfaceCard { Text("Preview") } }
}
