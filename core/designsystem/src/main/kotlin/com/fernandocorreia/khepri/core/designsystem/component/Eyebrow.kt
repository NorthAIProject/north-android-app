package com.fernandocorreia.khepri.core.designsystem.component

import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.intl.Locale
import androidx.compose.ui.text.toUpperCase
import androidx.compose.ui.tooling.preview.Preview
import com.fernandocorreia.khepri.core.designsystem.theme.KhepriTheme

/** The small uppercase label above a value, a card or a group: tracked, quiet unless given a colour. */
@Composable
public fun Eyebrow(text: String, modifier: Modifier = Modifier, color: Color = KhepriTheme.colors.mutedForeground) {
    Text(
        text = text.toUpperCase(Locale.current),
        modifier = modifier,
        color = color,
        style = KhepriTheme.textStyles.eyebrow,
    )
}

@Preview
@Composable
private fun EyebrowPreview() {
    KhepriTheme { Eyebrow("Preview") }
}
