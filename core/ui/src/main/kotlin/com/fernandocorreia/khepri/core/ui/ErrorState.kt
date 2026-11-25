package com.fernandocorreia.khepri.core.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.fernandocorreia.khepri.core.designsystem.theme.KhepriTheme
import com.fernandocorreia.khepri.core.model.AppError

/**
 * An empty screen that could not load: an offline icon when there is no connection, the server's message, Try Again.
 */
@Composable
public fun ErrorState(error: AppError, onRetry: () -> Unit, modifier: Modifier = Modifier) {
    val offline = error is AppError.Network
    Column(
        modifier = modifier.fillMaxSize().padding(KhepriTheme.spacing.xxl),
        verticalArrangement = Arrangement.spacedBy(KhepriTheme.spacing.md, Alignment.CenterVertically),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        Icon(
            painterResource(if (offline) R.drawable.ic_offline else R.drawable.ic_error),
            contentDescription = null,
            modifier = Modifier.size(48.dp),
            tint = KhepriTheme.colors.mutedForeground,
        )
        Text(
            stringResource(if (offline) R.string.error_offline_title else R.string.error_generic_title),
            style = MaterialTheme.typography.titleMedium,
            textAlign = TextAlign.Center,
        )
        Text(
            error.serverMessage()
                ?: stringResource(if (offline) R.string.error_offline_body else R.string.error_generic_body),
            style = MaterialTheme.typography.bodyMedium,
            color = KhepriTheme.colors.mutedForeground,
            textAlign = TextAlign.Center,
        )
        OutlinedButton(onClick = onRetry) { Text(stringResource(R.string.error_try_again)) }
    }
}

private fun AppError.serverMessage(): String? =
    when (this) {
        is AppError.Server -> message
        is AppError.NotFound -> message
        is AppError.Refused -> message
        else -> null
    }

@Preview
@Composable
private fun ErrorStatePreview() {
    KhepriTheme { ErrorState(AppError.Network(interrupted = false), onRetry = {}) }
}
