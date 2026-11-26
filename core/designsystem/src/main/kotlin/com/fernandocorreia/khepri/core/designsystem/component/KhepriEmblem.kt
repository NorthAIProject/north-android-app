package com.fernandocorreia.khepri.core.designsystem.component

import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.fernandocorreia.khepri.core.designsystem.R
import com.fernandocorreia.khepri.core.designsystem.theme.KhepriColors
import com.fernandocorreia.khepri.core.designsystem.theme.KhepriTheme

/**
 * The flat solar emblem (iOS `NorthBrand.mark`), in the theme's appearance rather than the system's. Fixed brand
 * colours, never tinted; for 48 dp and up. Decorative: the wordmark beside it carries the name.
 */
@Composable
public fun KhepriEmblem(size: Dp, modifier: Modifier = Modifier) {
    val dark = KhepriTheme.colors == KhepriColors.Dark
    Image(
        painter = painterResource(if (dark) R.drawable.khepri_emblem_dark else R.drawable.khepri_emblem_light),
        contentDescription = null,
        modifier = modifier.size(size),
    )
}

@Preview
@Composable
private fun KhepriEmblemPreview() {
    KhepriTheme { KhepriEmblem(120.dp) }
}
