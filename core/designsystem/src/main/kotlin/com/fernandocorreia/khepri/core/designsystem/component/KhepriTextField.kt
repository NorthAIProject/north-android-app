package com.fernandocorreia.khepri.core.designsystem.component

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import com.fernandocorreia.khepri.core.designsystem.R
import com.fernandocorreia.khepri.core.designsystem.theme.KhepriTheme

/** The standard text field. A trailing action gets a 48 dp touch target. */
@Composable
public fun KhepriTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    trailingAction: TrailingAction? = null,
    error: String? = null,
    singleLine: Boolean = true,
    keyboardOptions: KeyboardOptions = KeyboardOptions.Default,
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        label = { Text(label) },
        trailingIcon =
            trailingAction?.let { action ->
                {
                    IconButton(onClick = action.onClick) {
                        Icon(action.icon, contentDescription = action.contentDescription)
                    }
                }
            },
        isError = error != null,
        supportingText = error?.let { message -> { Text(message) } },
        singleLine = singleLine,
        keyboardOptions = keyboardOptions,
        shape = RoundedCornerShape(KhepriTheme.radii.control),
    )
}

/** The clear icon for a [TrailingAction]. */
@Composable public fun clearIcon(): Painter = painterResource(R.drawable.ic_clear)

@Preview
@Composable
private fun KhepriTextFieldPreview() {
    KhepriTheme {
        KhepriTextField(
            value = "Preview",
            onValueChange = {},
            label = "Label",
            trailingAction = TrailingAction(clearIcon(), "Clear", onClick = {}),
        )
    }
}
