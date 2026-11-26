package com.fernandocorreia.khepri.core.designsystem.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.fernandocorreia.khepri.core.designsystem.theme.KhepriTheme

/** The one prominent action on a screen: full width, signal fill. Disabled, it reads as unavailable in both modes. */
@Composable
public fun ProminentButton(
    text: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    loading: Boolean = false,
) {
    Button(
        onClick = onClick,
        modifier = modifier.fillMaxWidth().heightIn(min = 50.dp),
        enabled = enabled && !loading,
        shape = RoundedCornerShape(KhepriTheme.radii.control),
        colors =
            ButtonDefaults.buttonColors(
                containerColor = KhepriTheme.colors.signal,
                contentColor = KhepriTheme.colors.primaryForeground,
            ),
    ) {
        if (loading) {
            // As iOS: the button is disabled while it runs, and the spinner takes the disabled label's colour.
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp).semantics { contentDescription = text },
                color = LocalContentColor.current,
                strokeWidth = 2.dp,
            )
        } else {
            Text(text, fontWeight = FontWeight.SemiBold)
        }
    }
}

@Preview
@Composable
private fun ProminentButtonPreview() {
    KhepriTheme { ProminentButton("Preview", onClick = {}) }
}
