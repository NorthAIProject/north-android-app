package com.fernandocorreia.khepri.core.designsystem.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.fernandocorreia.khepri.core.designsystem.theme.KhepriTheme

/** The one prominent action on a screen: full width, signal fill. Disabled, it reads as unavailable in both modes. */
@Composable
public fun ProminentButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier, enabled: Boolean = true) {
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().heightIn(min = 50.dp),
        enabled = enabled,
        shape = RoundedCornerShape(KhepriTheme.radii.control),
        colors =
            ButtonDefaults.buttonColors(
                containerColor = KhepriTheme.colors.signal,
                contentColor = KhepriTheme.colors.primaryForeground,
            ),
    ) {
        Text(text, fontWeight = FontWeight.SemiBold)
    }
}

@Preview
@Composable
private fun ProminentButtonPreview() {
    KhepriTheme { ProminentButton("Preview", onClick = {}) }
}
